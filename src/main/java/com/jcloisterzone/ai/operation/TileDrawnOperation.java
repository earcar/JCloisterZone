package com.jcloisterzone.ai.operation;

import com.jcloisterzone.board.DefaultTilePack;
import com.jcloisterzone.board.Tile;
import com.jcloisterzone.game.Game;

public class TileDrawnOperation implements Operation {

    private final Tile tile;

    public TileDrawnOperation(Tile tile) {
        this.tile = tile;
    }

    @Override
    public void undo(Game game) {
            ((DefaultTilePack)game.getTilePack()).addTile(tile, game.getTilePack().getGroups().iterator().next());
    }
}
