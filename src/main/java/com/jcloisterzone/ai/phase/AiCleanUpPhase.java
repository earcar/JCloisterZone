package com.jcloisterzone.ai.phase;

import com.jcloisterzone.game.Game;
import com.jcloisterzone.game.phase.Phase;

public class AiCleanUpPhase extends Phase {

  public AiCleanUpPhase(Game game) {
    super(game);
  }

  @Override
  public void enter() {
    game.expansionDelegate().turnCleanUp();
    game.setCurrentTile(null);
    next();
  }

}
