package com.jcloisterzone.ai.starplayer;

import java.util.EnumSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.Maps;
import com.jcloisterzone.Expansion;
import com.jcloisterzone.ai.AiScoreContext;
import com.jcloisterzone.ai.RankingAiPlayer;
import com.jcloisterzone.ai.SavePoint;
import com.jcloisterzone.ai.SavePointManager;
import com.jcloisterzone.board.Position;
import com.jcloisterzone.board.Rotation;
import com.jcloisterzone.board.Tile;
import com.jcloisterzone.feature.Feature;
import com.jcloisterzone.figure.Meeple;
import com.jcloisterzone.game.Game;
import com.jcloisterzone.game.phase.Phase;
import com.jcloisterzone.game.phase.TilePhase;

public class StarAiPlayer extends RankingAiPlayer {
  private SavePointManager savePointManager;
  private Map<Feature, AiScoreContext> scoreCache = Maps.newHashMap();

  public static EnumSet<Expansion> supportedExpansions() {
    return EnumSet.of(Expansion.BASIC);
  }
  
  @Override
  protected double rank() {
    savePointManager = new SavePointManager(getGame());
    PositionEvaluator evaluator = new PositionEvaluator(getGame(), scoreCache);
    ExpectimaxNode node = new ExpectimaxNode(evaluator);
    node.setPositionRotationLocation(getGame().getCurrentTile());
    
    double score = expectimax(node, 0);
    
    // print info
    String meepleIn = node.getLocation() != null ? "meeple in " + node.getLocation().toString() : "no meeples deployed" ;
    System.err.println("The score for position (" + node.getPosition().x + "," + node.getPosition().y + "), rotation " + node.getRotation() + ", " + meepleIn + ", is " + score);
    
    
    return score;
  }

  protected double expectimax(ExpectimaxNode node, int depth) {
    if (node.isTerminal() || depth <= 0) { // or node is terminal
      return node.evaluate();
    }

    double score = 0.0;
    Game game = getGame();
    int numberOfChanceEvents = game.getTilePack().size();
    for (int i = 0; i < numberOfChanceEvents; i++) {
      SavePoint sp = savePointManager.save();
      
      Tile newTile = game.getTilePack().drawTile(i);

      double value = negamax(newTile, sp, node, depth);

//      savePointManager.restore(sp);
      double eventProbability = numberOfSameTiles(newTile)/game.getTilePack().size();
      score += value * eventProbability;
    }

    return score;
  }

  // the actual search function
  private double negamax(Tile tile, SavePoint savePoint, ExpectimaxNode node, int depth) {
    double score = Double.NEGATIVE_INFINITY; 
    for (Entry<Position, Set<Rotation>> entry : getBoard().getAvailablePlacements().entrySet()){
      Position position = entry.getKey();
      for (Rotation rotation : entry.getValue()) {
        assert position != null;
        assert rotation != null;
        goIntoTilePhase();
        getGame().getPhase().placeTile(rotation, position);
        System.err.println("Placing " + position + " " + rotation + "; phase = " + getGame().getPhase());
        ExpectimaxNode child = new ExpectimaxNode(node, node.getEvaluator());
        child.setPositionRotationLocation(getGame().getCurrentTile());
        node.addChild(child);
        double value = -expectimax(child, depth - 1);
        savePointManager.restore(savePoint);
        score = Math.max(value, score);
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

  // FIXME: desperate attempt
  private void goIntoTilePhase() {
      Phase phase = getGame().getPhase();
      while(! (phase instanceof TilePhase)) {
          System.err.println("  * not entered " +  phase.getClass().getName() + " -> " + phase.getDefaultNext().getClass().getName());
          phase.setEntered(true);
          phase.enter();
          phase = getGame().getPhase();
      }
  }
}
