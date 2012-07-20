package com.jcloisterzone.ai.starplayer;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.Maps;
import com.jcloisterzone.Expansion;
import com.jcloisterzone.action.MeepleAction;
import com.jcloisterzone.action.PlayerAction;
import com.jcloisterzone.action.TilePlacementAction;
import com.jcloisterzone.ai.AiPlayer;
import com.jcloisterzone.ai.AiScoreContext;
import com.jcloisterzone.ai.SavePoint;
import com.jcloisterzone.ai.SavePointManager;
import com.jcloisterzone.ai.copy.CopyGamePhaseForDepthSearch;
import com.jcloisterzone.ai.phase.*;
import com.jcloisterzone.board.Location;
import com.jcloisterzone.board.Position;
import com.jcloisterzone.board.Rotation;
import com.jcloisterzone.board.Tile;
import com.jcloisterzone.event.GameEventAdapter;
import com.jcloisterzone.feature.Feature;
import com.jcloisterzone.figure.Meeple;
import com.jcloisterzone.game.Game;
import com.jcloisterzone.game.Snapshot;
import com.jcloisterzone.game.phase.Phase;

public class ExpectimaxAiPlayer extends AiPlayer {
  private SavePointManager savePointManager = null;
  private static final int DEPTH = 2;

  /*
   * (non-Javadoc) This AI only supports the basic extension
   * TODO add the abstract method in AiPlayer, since every AI should have one
   */
  public static EnumSet<Expansion> supportedExpansions() {
    return EnumSet.of(Expansion.BASIC);
  }

  @Override
  public void selectAction(List<PlayerAction> actions, boolean canPass) {
    assert savePointManager == null; // this function should be called only once through the ranking process

    if (logger.isDebugEnabled()) {
      logger.debug("selectAction({}, {})", actions, canPass);
      logger.debug("* phase: {}", getGame().getPhase().getClass().getName());
    }
    if (! (actions.get(0) instanceof TilePlacementAction)) {
      return;
    }

    // prepare data structures
    // * backup game
    Game original = getGame();
    Game aiGame = copyGame(getGame());
    // * set as current game
    setGame(aiGame);
    savePointManager = new SavePointManager(getGame());
    savePointManager.startRecording();
    // * prepare tree
    ExpectimaxNode rootNode = new ExpectimaxNode();
    int depth = DEPTH;

    // rank moves
    double score = expectimax(rootNode, depth);
    logger.info("Expectimax score: {}", score);

    // restore original game
    setGame(original);
    savePointManager.stopRecording();
    savePointManager = null;

    // make move
    logger.info("MOVE SCORE: {}", rootNode.getScore());
    getServer().placeTile(rootNode.getRotation(), rootNode.getPosition());
    if (rootNode.getLocation() != null) {
      getServer().deployMeeple(rootNode.getPosition(), rootNode.getLocation(), rootNode.getMeepleType());
    } else {
      getServer().pass();
    }
  }

  private double expectimax(ExpectimaxNode node, int depth) {
    double score = 0;

    // if node is terminal
    if (depth == 0) {
      assert getGame().getPhase() instanceof AiScorePhase; // ensure we are in the score phase
      AiScorePhase scorePhase = (AiScorePhase) getGame().getPhase();
      scorePhase.setWillScore(true); // allow scoring
      Map<Feature, AiScoreContext> scoreCache = Maps.newHashMap();
      PositionEvaluator evaluator = new PositionEvaluator(getGame(), scoreCache);
      
      // evaluate
      score = evaluator.evaluate();
      
      scorePhase.setWillScore(false); // disallow scoring
    } else {
      if (node.getParent() == null) { // if is root node we don't have to draw a new tile
        assert getGame().getPhase() instanceof AiTilePhase; // ensure we are in tile phase
        SavePoint restore = savePointManager.save(); // save here
        double value = negamax(node, depth); // evaluate game from here
        score += value * numberOfSameTiles(getGame().getCurrentTile())/getGame().getTilePack().size(); // calc score
        savePointManager.restore(restore); // restore here
      } else { // we have to draw new tiles
        for (int i = 0; i < getGame().getTilePack().size(); i++) {
          enterNextPhaseIfIs(AiScorePhase.class); // jump scoring phase (willScore = false)
          enterNextPhaseIfIs(AiCleanUpPhase.class); // clean up stuff
          SavePoint restore = savePointManager.save();
          
          // draw new tile
          assert getGame().getPhase() instanceof AiDrawPhase;
          ((AiDrawPhase) getGame().getPhase()).setWillDrawTiles(false); // we draw our own tiles, thank you very much
          ((AiDrawPhase) getGame().getPhase()).drawTile(i);
          enterNextPhaseIfIs(AiDrawPhase.class);
          
          enterNextPhaseIfIs(AiTilePhase.class);
          double value = negamax(node, depth);
          score += value * numberOfSameTiles(getGame().getCurrentTile())/getGame().getTilePack().size();
          savePointManager.restore(restore);
        }
      }
    }
    node.setScore(score);
    logger.info("score = {}", score);
    return score;
  }

  private Map<Position, Set<Rotation>> getAvailablePlacements() {
    getBoard().refreshAvailablePlacements(getGame().getCurrentTile());
    return getBoard().getAvailablePlacements();
  }

  private double negamax(ExpectimaxNode node, int depth) {
    double score = Double.NEGATIVE_INFINITY; 
    
    for (Entry<Position, Set<Rotation>> entry : getAvailablePlacements().entrySet()){
      Position position = entry.getKey();
      for (Rotation rotation : entry.getValue()) {
        
        // place tile
        assert getGame().getPhase() instanceof AiTilePhase;
        SavePoint beforeTile = savePointManager.save();
        getGame().getPhase().placeTile(rotation, position);

        assert getGame().getPhase() instanceof AiActionPhase;
        List<PlayerAction> actions = ((AiActionPhase) getGame().getPhase()).getActions();
        if (actions.isEmpty()) { // no meeples to place
          ExpectimaxNode childNode = new ExpectimaxNode(node, position, rotation, null);
          SavePoint beforeMeeple = savePointManager.save();
          enterNextPhaseIfIs(AiActionPhase.class);
          
          double value = expectimax(childNode, depth - 1);
          if ((node.isMax() && value > score) || (node.isMin() && value < score)) {
            node.setScore(value);
            node.setPosition(position);
            node.setRotation(rotation);
            node.setLocation(null);
            node.setMeepleType(null);
            score = value;
          }
          
          savePointManager.restore(beforeMeeple);
          logger.info("depth = {}; best score so far = {}", depth, score);
          
        } else { // at least one meeple to place
          for (PlayerAction action : actions) {
            Class<? extends Meeple> meepleType = ((MeepleAction) action).getMeepleType();
            if (action instanceof MeepleAction) {
              Set<Location> locations = ((MeepleAction) action).getSites().get(position);
              locations.add(null); // also rate a position without meeples
              
              for (Location location : locations) {
                ExpectimaxNode childNode = new ExpectimaxNode(node, position, rotation, location);
                SavePoint beforeMeeple = savePointManager.save();
                
                if (location == null) { // no meeple
                  enterNextPhaseIfIs(AiActionPhase.class);
                } else { // meeple
                  getGame().getPhase().deployMeeple(position, location, meepleType);
                }
                
                // evaluate
                double value = expectimax(childNode, depth - 1);
                
                if ((node.isMax() && value > score) || (node.isMin() && value < score)) {
                  node.setScore(value);
                  node.setPosition(position);
                  node.setRotation(rotation);
                  node.setLocation(location);
                  node.setMeepleType(meepleType);
                  score = value;
                }
                
                savePointManager.restore(beforeMeeple);
                logger.info("depth = {}; best score so far = {}", depth, score);
              }
            } else {
              logger.error("selectMeeplePlacement(): unsupported action {}", action);
              throw new UnsupportedOperationException(action.toString());
            }
          }
        }
        // finished placing meeples
        savePointManager.restore(beforeTile);
      }
    }
    return score;
  }

  private int numberOfSameTiles(Tile tile) {
    int number = 0;
    SavePoint save = savePointManager.save();
    for(String groupId: getGame().getTilePack().getGroups()) { // we don't need to check if it's in an inactive group since it's not possible to have a tile from one
      Tile exctractedTile = getGame().getTilePack().drawTile(groupId, tile.getId());
      if (exctractedTile != null) {
        number++;
      } else {
        break;
      }
    }
    savePointManager.restore(save);
    return number;
  }

  private Game copyGame(Game original) {
    Snapshot snapshot = new Snapshot(original, 0);
    Game gameCopy = snapshot.asGame();
    gameCopy.setConfig(original.getConfig());
    gameCopy.addGameListener(new GameEventAdapter());
    gameCopy.addUserInterface(this);
    Phase phase = new CopyGamePhaseForDepthSearch(gameCopy, snapshot, original.getTilePack());
    gameCopy.getPhases().put(phase.getClass(), phase);
    gameCopy.setPhase(phase);
    phase.startGame();
    return gameCopy;
  }

  private void enterNextPhaseIfIs(Class<? extends Phase> nextPhase) {
    Phase phase = getGame().getPhase();
    if (logger.isDebugEnabled()) {
      StackTraceElement[] stackTraceElements = Thread.currentThread()
          .getStackTrace();
      logger.debug("enterNextPhaseIfIs({}), called by {}", nextPhase,
          stackTraceElements[2]);
      logger.debug("* current: {}, next: {}", phase, phase.getDefaultNext());
    }
    if (phase.getClass() == nextPhase && !phase.isEntered()) {
      phase.setEntered(true);
      phase.enter();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.jcloisterzone.UserInterface#selectBazaarTile()
   */
  @Override
  public void selectBazaarTile() {
    logger.error("selectBazaarTile(): operation not supported");
    throw new UnsupportedOperationException();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.jcloisterzone.UserInterface#makeBazaarBid(int)
   */
  @Override
  public void makeBazaarBid(int supplyIndex) {
    logger.error("makeBazaarBid(): operation not supported");
    throw new UnsupportedOperationException();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.jcloisterzone.UserInterface#selectBuyOrSellBazaarOffer(int)
   */
  @Override
  public void selectBuyOrSellBazaarOffer(int supplyIndex) {
    logger.error("selectBuyOrSellBazaarOffer(): operation not supported");
    throw new UnsupportedOperationException();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.jcloisterzone.UserInterface#selectDragonMove(java.util.Set, int)
   */
  @Override
  public void selectDragonMove(Set<Position> positions, int movesLeft) {
    logger.error("selectDragonMove(): operation not supported");
    throw new UnsupportedOperationException();
  }

}
