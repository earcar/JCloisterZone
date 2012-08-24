package com.jcloisterzone.ai.starplayer;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.Maps;
import com.jcloisterzone.Expansion;
import com.jcloisterzone.Player;
import com.jcloisterzone.action.MeepleAction;
import com.jcloisterzone.action.PlayerAction;
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
import com.jcloisterzone.figure.Follower;
import com.jcloisterzone.figure.Meeple;
import com.jcloisterzone.game.Game;
import com.jcloisterzone.game.Snapshot;
import com.jcloisterzone.game.phase.GameOverPhase;
import com.jcloisterzone.game.phase.Phase;
import com.jcloisterzone.game.phase.TilePhase;

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
      logger.debug("selectAction({}, {}), phase = " + getGame().getPhase().getClass().getName(), actions, canPass);
    }
    if (actions.get(0) instanceof MeepleAction) {
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
    rootNode.setPlayer(getGame().getTurnPlayer());
    int depth = DEPTH;

    // rank moves
    double score = expectimax(rootNode, depth);
    
//    printTree(rootNode);
//    System.exit(1);
    logger.info("Expectimax score: {}", score);

    // restore original game
    setGame(original);
    savePointManager.stopRecording();
    savePointManager = null;

    // make move
    makeMove(rootNode);
  }

  private void printTree(ExpectimaxNode rootNode) {
    FileOutputStream out = null;
    try {
      out = new FileOutputStream("/Users/carmine/Desktop/tree.txt");
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    rootNode.print(new PrintStream(out));
  }

  private void makeMove(ExpectimaxNode rootNode) {
    assert getGame().getPhase() instanceof TilePhase;
    ExpectimaxNode bestMove = rootNode.getBestMove();
    
    logger.info("MOVE SCORE: {}", bestMove.getScore());

    getGame().setCurrentTile(bestMove.getTile());
    getServer().placeTile(bestMove.getRotation(), bestMove.getPosition());
    if (bestMove.getLocation() != null && bestMove.getMeepleType() != null) {
      getServer().deployMeeple(bestMove.getPosition(), bestMove.getLocation(), bestMove.getMeepleType());
    } else {
      getServer().pass();
    }
  }

  private Double expectimax(ExpectimaxNode node, int depth) {
    Double score = null;

    // if node is terminal
    if (depth == 0 || getGame().getPhase() instanceof GameOverPhase) {
      score = evaluateCurrentMove();
      node.setScore(score);
      logger.info("depth {} score {} tile " + getGame().getCurrentTile(), depth, score);
    } else {
      if (node.isRootNode()) { // if is root node we don't have to draw a new tile
        score = 0.0;
        assert getGame().getPhase() instanceof AiTilePhase; // ensure we are in tile phase
        SavePoint restore = savePointManager.save(); // save here
        
        Tile backupTile = getGame().getCurrentTile();
        double value = negamax(node, depth); // evaluate game from here
        getGame().setCurrentTile(backupTile);

        double newValue = value * probabilityOfCurrentTile();
        if (isMe()) {
          score -= newValue;
        } else {
          score += newValue;
        }
        node.setScore(score);

        savePointManager.restore(restore); // restore here
      } else { // we have to draw new tiles
        if (getGame().getTilePack().size() > 0) score = 0.0;
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
          
          Tile backupTile = getGame().getCurrentTile();
          double value = negamax(node, depth);
          getGame().setCurrentTile(backupTile);

          double newValue = value * probabilityOfCurrentTile();
          if (isMe()) {
            score -= newValue;
          } else {
            score += newValue;
          }
          node.setScore(score);

          savePointManager.restore(restore);
        }
      }
    }
    return score;
  }

  private boolean isMe() {
    return getGame().getTurnPlayer().getIndex() == this.getPlayer().getIndex();
  }

  private double probabilityOfCurrentTile() {
    return ((double) countSameTiles() + 1)/((double)(getGame().getTilePack().size() + 1));
  }

  private double evaluateCurrentMove() {
    double score;
    assert getGame().getPhase() instanceof AiScorePhase; // ensure we are in the score phase
    AiScorePhase scorePhase = (AiScorePhase) getGame().getPhase();
    scorePhase.setWillScore(true); // allow scoring
    Map<Feature, AiScoreContext> scoreCache = Maps.newHashMap();
    PositionEvaluator evaluator = new PositionEvaluator(getGame().getTurnPlayer(), getGame(), scoreCache);

    // evaluate
    score = evaluator.evaluate();

    scorePhase.setWillScore(false); // disallow scoring
    return score;
  }

  private int countSameTiles() {
    return getGame().getTilePack().countSameTiles(getGame().getTilePack().getGroups().iterator().next(), getGame().getCurrentTile().getId());
  }

  private Map<Position, Set<Rotation>> getAvailablePlacements() {
    getBoard().refreshAvailablePlacements(getGame().getCurrentTile());
    return Maps.newHashMap(getBoard().getAvailablePlacements());
  }

  private double negamax(ExpectimaxNode node, int depth) {
    double score = 0.0;
    if (node.isMax()) { 
      score = Double.NEGATIVE_INFINITY;
    } else {
      score = Double.POSITIVE_INFINITY;
    }

    SavePoint beforeTile = savePointManager.save();
    for (Entry<Position, Set<Rotation>> entry : Maps.newHashMap(getAvailablePlacements()).entrySet()){
      Position position = entry.getKey();
      for (Rotation rotation : entry.getValue()) {

        // place tile
        assert getGame().getPhase() instanceof AiTilePhase;
        getGame().getPhase().placeTile(rotation, position);

        assert getGame().getPhase() instanceof AiActionPhase;
        List<PlayerAction> actions = ((AiActionPhase) getGame().getPhase()).getActions();
        if (actions.isEmpty()) { // no meeples to place
          score = callExpectimax(node, depth, score, getGame().getCurrentTile(), position, rotation,
              null, null);

        } else { // at least one meeple to place
          for (PlayerAction action : actions) {
            Class<? extends Meeple> meepleType = ((MeepleAction) action).getMeepleType();
            if (action instanceof MeepleAction) {
              Set<Location> locations = ((MeepleAction) action).getSites().get(position);
              locations.add(null); // also rate a position without meeples

              for (Location location : locations) {
                score = callExpectimax(node, depth, score, getGame().getCurrentTile(), position, rotation,
                    meepleType, location);
//                System.out.println("position: " + position + " location: " + location + " rotation: " + rotation + " tile pack: " + getGame().getTilePack().size());
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
      savePointManager.restore(beforeTile);
      enterNextPhaseIfIs(AiCleanUpPhase.class);
    }
    return score;
  }
  // TODO: change name
  private double callExpectimax(ExpectimaxNode node, int depth, double score,
      Tile tile, Position position, Rotation rotation, Class<? extends Meeple> meepleType,
      Location location) {
    SavePoint beforeMeeple = savePointManager.save();

    Tile backupTile = getGame().getCurrentTile();
    Player currentPlayer = getGame().getTurnPlayer();
    if (depth != DEPTH) {
      getGame().setPlayer(getGame().getNextPlayer());
    }
    if (location == null || meepleType == null) { // no meeple
      enterNextPhaseIfIs(AiActionPhase.class);
    } else { // meeple
      getGame().getPhase().deployMeeple(position, location, meepleType);
    }

    // evaluate
    ExpectimaxNode childNode = new ExpectimaxNode(node, tile, position, rotation, location, meepleType);
    childNode.setPlayer(getGame().getTurnPlayer());
    Double value = expectimax(childNode, depth - 1);
    getGame().setPlayer(currentPlayer);
    getGame().setCurrentTile(backupTile);
    
    if ((node.isMax() && value > score) || (node.isMin() && value < score)) {
      node.setBestMove(childNode);
      score = value;
      logger.info("Player " + getGame().getTurnPlayer().getIndex() + " depth {} best score {} tile " + getGame().getCurrentTile(), depth, score);
    }
    savePointManager.restore(beforeMeeple);
    return score;
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
