package com.jcloisterzone.ai.copy;

import java.util.Map;

import com.jcloisterzone.ai.phase.*;
import com.jcloisterzone.board.TilePack;
import com.jcloisterzone.game.Game;
import com.jcloisterzone.game.Snapshot;
import com.jcloisterzone.game.phase.*;

public class CopyGamePhaseForDepthSearch extends LoadGamePhase {

  public CopyGamePhaseForDepthSearch(Game game, Snapshot snapshot,
      TilePack originalTilePack) {
    super(game, snapshot, null);
  }

  @Override
  protected void preparePlayers() {
    initializePlayersMeeples();
  }

  @Override
  protected void prepareAiPlayers() {
    // empty
  }

  @Override
  protected void preparePhases() {
    Phase next = null;
    // no assignment - phase is out of standard flow
    addPhase(next, new GameOverPhase(game));
    next = addPhase(next, new AiCleanUpPhase(game));
    next = addPhase(next, new BazaarPhase(game, getServer()));
    next = addPhase(next, new EscapePhase(game));
    next = addPhase(next, new WagonPhase(game));
    next = addPhase(next, new AiScorePhase(game));
    next = addPhase(next, new CastlePhase(game));
    addPhase(next, new DragonMovePhase(game));
    next = addPhase(next, new DragonPhase(game));
    next = addPhase(next, new PhantomPhase(game));
    addPhase(next, new TowerCapturePhase(game));
    next = addPhase(next, new AiActionPhase(game));
    next = addPhase(next, new AiTilePhase(game));
    next = addPhase(next, new AiDrawPhase(game));
    next = addPhase(next, new AbbeyPhase(game));
    next = addPhase(next, new FairyPhase(game));
    setDefaultNext(next); // set next phase for this (CreateGamePhase) instance
    game.getPhases().get(AiCleanUpPhase.class).setDefaultNext(next); // after last first is default
    
    Phase active;
    if (snapshot.getActivePhase() == TilePhase.class) {
      active = game.getPhases().get(AiTilePhase.class);
    } else if (snapshot.getActivePhase() == DrawPhase.class) {
      active = game.getPhases().get(AiDrawPhase.class);
    } else if (snapshot.getActivePhase() == ActionPhase.class) {
      active = game.getPhases().get(AiActionPhase.class); 
    } else if (snapshot.getActivePhase() == ScorePhase.class) {
      active = game.getPhases().get(AiScorePhase.class);
    } else if (snapshot.getActivePhase() == CleanUpPhase.class) {
      active = game.getPhases().get(AiCleanUpPhase.class);
    } else {
      active = game.getPhases().get(snapshot.getActivePhase());
    }
    setDefaultNext(active);
  }

  private Phase addPhase(Phase next, Phase phase) {
    if (!phase.isActive())
      return next;

    Map<Class<? extends Phase>, Phase> phases = game.getPhases();
    phases.put(phase.getClass(), phase);
    if (next != null) {
      phase.setDefaultNext(next);
    }
    return phase;
  }

}
