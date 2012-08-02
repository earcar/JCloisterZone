package com.jcloisterzone.ai.phase;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.jcloisterzone.Expansion;
import com.jcloisterzone.board.Location;
import com.jcloisterzone.board.Position;
import com.jcloisterzone.board.Rotation;
import com.jcloisterzone.board.Tile;
import com.jcloisterzone.collection.Sites;
import com.jcloisterzone.game.Game;
import com.jcloisterzone.game.Snapshot;
import com.jcloisterzone.game.expansion.BridgesCastlesBazaarsGame;
import com.jcloisterzone.game.phase.Phase;

public class AiTilePhase extends Phase {
  private Map<Position, Set<Rotation>> availablePlacements;

  public AiTilePhase(Game game) {
    super(game);
  }

  @Override
  public void enter() {
    setAvailablePlacements(Maps.newHashMap(getBoard().getAvailablePlacements()));
  }

  @Override
  public void loadGame(Snapshot snapshot) {
    String tileId = snapshot.getNextTile();
    Tile tile = game.getTilePack().drawTile(tileId);
    game.setCurrentTile(tile);
    game.getBoard().refreshAvailablePlacements(tile);
    game.fireGameEvent().tileDrawn(tile);
  }

  @Override
  public void placeTile(Rotation rotation, Position p) {
    Tile tile = getTile();
    tile.setRotation(rotation);

    boolean bridgeRequired = false;
    if (game.hasExpansion(Expansion.BRIDGES_CASTLES_AND_BAZAARS)) {
      bridgeRequired = !getBoard().isPlacementAllowed(tile, p);
    }

    getBoard().refreshAvailablePlacements(tile);
    getBoard().add(tile, p);
    if (tile.getTower() != null) {
      game.getTowerGame().registerTower(p);
    }
    game.fireGameEvent().tilePlaced(tile);

    if (bridgeRequired) {
      BridgesCastlesBazaarsGame bcb = game.getBridgesCastlesBazaarsGame();
      Sites sites = bcb.prepareMandatoryBridgeAction().getSites();

      assert sites.size() == 1;
      Position pos = sites.keySet().iterator().next();
      Location loc = sites.get(pos).iterator().next();

      bcb.decreaseBridges(getActivePlayer());
      bcb.deployBridge(pos, loc);
    }
    getBoard().mergeFeatures(tile);

    next();
  }

  public Map<Position, Set<Rotation>> getAvailablePlacements() {
    return availablePlacements;
  }

  private void setAvailablePlacements(Map<Position, Set<Rotation>> availablePlacements) {
    this.availablePlacements = availablePlacements;
  }
}
