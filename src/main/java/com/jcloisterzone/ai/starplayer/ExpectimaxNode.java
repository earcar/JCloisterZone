package com.jcloisterzone.ai.starplayer;

import java.util.ArrayList;
import java.util.List;

import com.jcloisterzone.board.Location;
import com.jcloisterzone.board.Position;
import com.jcloisterzone.board.Rotation;
import com.jcloisterzone.board.Tile;
import com.jcloisterzone.feature.Feature;
import com.jcloisterzone.figure.Meeple;

public class ExpectimaxNode {
  private List<ExpectimaxNode> children;
  private ExpectimaxNode parent;
  private double value;
  private Position position;
  private Rotation rotation;
  private Location location;
  private PositionEvaluator evaluator;
  
  public ExpectimaxNode(PositionEvaluator evaluator) {
    this(null, evaluator);
  }
  
  public ExpectimaxNode(ExpectimaxNode parent, PositionEvaluator evaluator) {
    this.evaluator = evaluator;
    this.parent = parent;
    this.value = 0.0;
    this.children = new ArrayList<ExpectimaxNode>();
  }
  
  public ExpectimaxNode getChild(int index) {
    return children.get(index);
  }

  public List<ExpectimaxNode> getChildren() {
    return children;
  }
  
  public ExpectimaxNode getParent() {
    return parent;
  }
  
  public double getValue() {
    return value;
  }

  public double evaluate() {
    if (getValue() == 0.0 && ! this.isTerminal()) {
      setValue(evaluator.evaluate());
    }
    return getValue();
  }

  public PositionEvaluator getEvaluator() {
    return evaluator;
  }

  public void setValue(double value) {
    this.value = value;
  }

  public boolean addChild(ExpectimaxNode node) {
    return children.add(node);
  }

  public boolean removeChild(ExpectimaxNode node) {
    return children.remove(node);
  }

  public boolean isTerminal() {
    return false; // TODO: temporary
  }

  public Position getPosition() {
    return position;
  }

  public void setPosition(Position position) {
    this.position = position;
  }

  public Location getLocation() {
    return location;
  }

  public void setLocation(Location location) {
    this.location = location;
  }

  public Rotation getRotation() {
    return rotation;
  }

  public void setRotation(Rotation rotation) {
    this.rotation = rotation;
  }
  
  public void setPositionRotationLocation(Tile currentTile) {
    assert currentTile != null;
    assert currentTile.getPosition() != null;
    assert currentTile.getRotation() != null;
    assert currentTile.getFeatures() != null;
    this.setPosition(currentTile.getPosition());
    this.setRotation(currentTile.getRotation());
    for(Feature f : currentTile.getFeatures()) {
      Meeple m = f.getMeeple();
      if (m != null && m.isDeployed()) {
        this.setLocation(m.getLocation());
        break;
      }
    }
  }
}
