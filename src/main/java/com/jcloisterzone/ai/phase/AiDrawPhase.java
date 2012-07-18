package com.jcloisterzone.ai.phase;

import java.util.Random;

import com.jcloisterzone.board.Tile;
import com.jcloisterzone.game.Game;
import com.jcloisterzone.game.phase.Phase;

public class AiDrawPhase extends Phase {
  private Random random = new Random();
  private boolean willDrawTiles = true;

  public AiDrawPhase(Game game) {
    super(game);
  }

  @Override
  public void enter() {
    // if (getTilePack().isEmpty()) {
    // next(GameOverPhase.class);
    // return;
    // }
    if (this.willDrawTiles()) {
      this.selectTiles(getTilePack().size(), 1);
    }
  }

  // from Server.selectTiles()
  public void selectTiles(int tilesCount, int drawCount) {
    assert tilesCount >= drawCount && drawCount > 0;
    int[] result = new int[drawCount];
    for (int i = 0; i < drawCount; i++) {
      result[i] = random.nextInt(tilesCount--);
    }
    this.drawTiles(result);
  }

  // wrap drawTiles to have a saner API
  public void drawTile(int tileIndex) {
    int[] tiles = new int[1];
    tiles[0] = tileIndex;
    this.drawTiles(tiles);
  }

  @Override
  public void drawTiles(int[] tileIndex) {
    assert tileIndex.length == 1;
    Tile tile = getTilePack().drawTile(tileIndex[0]);
    this.nextTile(tile);
  }

  private void nextTile(Tile tile) {
    game.setCurrentTile(tile);
    getBoard().refreshAvailablePlacements(tile);
    if (getBoard().getAvailablePlacementPositions().isEmpty()) {
      getBoard().discardTile(tile.getId());
      next(AiDrawPhase.class);
      return;
    }
    game.fireGameEvent().tileDrawn(tile);
    next();
  }

  public boolean willDrawTiles() {
    return willDrawTiles;
  }

  public void setWillDrawTiles(boolean willDrawTiles) {
    this.willDrawTiles = willDrawTiles;
  }

}
