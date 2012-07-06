/**
 * 
 */
package com.jcloisterzone.ai.starplayer;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.jcloisterzone.Expansion;
import com.jcloisterzone.action.MeepleAction;
import com.jcloisterzone.action.PlayerAction;
import com.jcloisterzone.action.TilePlacementAction;
import com.jcloisterzone.ai.AiPlayer;
import com.jcloisterzone.board.Position;

/**
 * @author carmine
 *
 */
public class StarAiPlayer extends AiPlayer {

  /* (non-Javadoc)
   * This AI only supports the basic extension
   * TODO add the abstract method in AiPlayer, since every AI should have one
   */
  public static EnumSet<Expansion> supportedExpansions() {
    return EnumSet.of(Expansion.BASIC);
  }

  /* (non-Javadoc)
   * @see com.jcloisterzone.UserInterface#selectAction(java.util.List, boolean)
   * entry point for the AI, selects the action to be completed 
   */
  @Override
  public void selectAction(List<PlayerAction> actions, boolean canPass) {
    if (logger.isDebugEnabled()) {
      logger.debug("selectAction({}, {})", actions, canPass);
      logger.debug("* phase: {}", getGame().getPhase().getClass().getName());
    }
    
    PlayerAction firstAction = actions.get(0);
    if (firstAction instanceof TilePlacementAction) {
      selectTilePlacement(actions);
    } else if (firstAction instanceof MeepleAction) {
      selectMeeplePlacement(actions);
    } else {
      getServer().pass();
    }

  }

  private void selectMeeplePlacement(List<PlayerAction> actions) {
    for(PlayerAction action: actions) {
      if (action instanceof MeepleAction) {
        selectDummyMeepleAction((MeepleAction) action); // TODO
      } else {
        logger.error("selectMeeplePlacement(): unsupported action {}", action);
        throw new UnsupportedOperationException(action.toString());
      }
    }
    
  }

  private void selectTilePlacement(List<PlayerAction> actions) {
    for(PlayerAction action: actions) {
      if (action instanceof TilePlacementAction) {
        selectDummyTilePlacement((TilePlacementAction) action); // TODO
      } else {
        logger.error("selectTilePlacement(): unsupported action {}", action);
        throw new UnsupportedOperationException(action.toString());
      }
    }
  }

  /* (non-Javadoc)
   * @see com.jcloisterzone.UserInterface#selectBazaarTile()
   */
  @Override
  public void selectBazaarTile() {
    logger.error("selectBazaarTile(): operation not supported");
    throw new UnsupportedOperationException();
  }

  /* (non-Javadoc)
   * @see com.jcloisterzone.UserInterface#makeBazaarBid(int)
   */
  @Override
  public void makeBazaarBid(int supplyIndex) {
    logger.error("makeBazaarBid(): operation not supported");
    throw new UnsupportedOperationException();
  }

  /* (non-Javadoc)
   * @see com.jcloisterzone.UserInterface#selectBuyOrSellBazaarOffer(int)
   */
  @Override
  public void selectBuyOrSellBazaarOffer(int supplyIndex) {
    logger.error("selectBuyOrSellBazaarOffer(): operation not supported");
    throw new UnsupportedOperationException();
  }

  /* (non-Javadoc)
   * @see com.jcloisterzone.UserInterface#selectDragonMove(java.util.Set, int)
   */
  @Override
  public void selectDragonMove(Set<Position> positions, int movesLeft) {
    logger.error("selectDragonMove(): operation not supported");
    throw new UnsupportedOperationException();
  }

}
