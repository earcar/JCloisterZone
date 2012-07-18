package com.jcloisterzone.ai.starplayer;

import java.util.ArrayList;
import java.util.List;

import com.jcloisterzone.board.Location;
import com.jcloisterzone.board.Position;
import com.jcloisterzone.board.Rotation;
import com.jcloisterzone.figure.Meeple;

public class ExpectimaxNode {
  private List<ExpectimaxNode> children;
  private ExpectimaxNode parent;
  private Double score;
  private boolean isMax;
  // the move
  private Position position;
  private Rotation rotation;
  private Location location;
  private Class<? extends Meeple> meepleType;

  public ExpectimaxNode() {
    this(null, null, null, null);
    this.isMax = true;
  }

  public ExpectimaxNode(ExpectimaxNode parent, Position position, Rotation rotation, Location location) {
    this.parent = parent;
    this.setScore(null);
    this.setPosition(position);
    this.setRotation(rotation);
    this.setLocation(location);
    this.children = new ArrayList<ExpectimaxNode>();
    if (parent != null) {
      this.isMax = parent.isMin();
      parent.addChild(this);
    }
    
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
}
