package com.jcloisterzone.ai.starplayer;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import com.jcloisterzone.Player;
import com.jcloisterzone.board.Location;
import com.jcloisterzone.board.Position;
import com.jcloisterzone.board.Rotation;
import com.jcloisterzone.board.Tile;
import com.jcloisterzone.figure.Meeple;

public class ExpectimaxNode {
  private List<ExpectimaxNode> children;
  private ExpectimaxNode parent;
  private Double score;
  private boolean isMax;
  private ExpectimaxNode bestMove;
  // current move
  private Tile tile;
  private Position position;
  private Rotation rotation;
  private Location location;
  private Class<? extends Meeple> meepleType;
  private Player player;

  public ExpectimaxNode() {
    this(null, null, null, null, null, null);
    this.isMax = true;
  }

  public ExpectimaxNode(ExpectimaxNode parent, Tile tile, Position position, Rotation rotation, Location location, Class<? extends Meeple> meepleType) {
    this.parent = parent;
    this.setScore(null);
    this.setTile(tile);
    this.setPosition(position);
    this.setRotation(rotation);
    this.setLocation(location);
    this.setMeepleType(meepleType);
    this.children = new ArrayList<ExpectimaxNode>();
    if (parent != null) {
      this.isMax = parent.isMin();
      parent.addChild(this);
    }
    this.setBestMove(null);
  }

  public List<ExpectimaxNode> getChildren() {
    return this.children;
  }

  public ExpectimaxNode getParent() {
    return this.parent;
  }

  public boolean addChild(ExpectimaxNode node) {
    return children.add(node);
  }
  
  public boolean removeChild(ExpectimaxNode node) {
    return children.remove(node);
  }

  public Double getScore() {
    return score;
  }

  public void setScore(Double value) {
    this.score = value;
  }

  public boolean isMax() {
    return this.isMax;
  }

  public boolean isMin() {
    return ! this.isMax;
  }

  public Position getPosition() {
    return position;
  }

  public void setPosition(Position position) {
    this.position = position;
  }

  public Rotation getRotation() {
    return rotation;
  }

  public void setRotation(Rotation rotation) {
    this.rotation = rotation;
  }

  public Location getLocation() {
    return location;
  }

  public void setLocation(Location location) {
    this.location = location;
  }

  public Class<? extends Meeple> getMeepleType() {
    return meepleType;
  }

  public void setMeepleType(Class<? extends Meeple> meepleType) {
    this.meepleType = meepleType;
  }

  public String toString() {
    String string = "ExpectimaxNode " + (isMax ? "max" : "min");
    if (position != null && rotation != null) {
      string += " tile: " + tile +  " position: " + position + " rotation: " + rotation + " location: " + location;
    }
    if (bestMove != null) {
      string += " (BEST MOVE: " + bestMove.toString() + ")";
    }
    return string + " score: " + score + " player: " + player;
  }

  public void print(PrintStream printStream) {
    print(printStream, "", true);
  }

  private void print(PrintStream printStream, String prefix, boolean isTail) {
    printStream.println(prefix + (isTail ? "└── " : "├── ") + toString());
    if (children != null) {
      for (int i = 0; i < children.size() - 1; i++) {
        children.get(i).print(printStream, prefix + (isTail ? "    " : "│   "), false);
      }
      if (children.size() >= 1) {
        children.get(children.size() - 1).print(printStream, prefix + (isTail ?"    " : "│   "), true);
      }
    }
  }

  public boolean isRootNode() {
    return parent == null;
  }

  public Tile getTile() {
    return tile;
  }

  public void setTile(Tile tile) {
    this.tile = tile;
  }

  public ExpectimaxNode getBestMove() {
    return bestMove;
  }

  public void setBestMove(ExpectimaxNode bestMove) {
    this.bestMove = bestMove;
  }

  public Player getPlayer() {
    return player;
  }

  public void setPlayer(Player player) {
    this.player = player;
  }
}
