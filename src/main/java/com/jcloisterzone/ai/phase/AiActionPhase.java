package com.jcloisterzone.ai.phase;

import java.util.List;

import com.google.common.collect.Lists;
import com.jcloisterzone.action.TakePrisonerAction;
import com.jcloisterzone.action.MeepleAction;
import com.jcloisterzone.action.PlayerAction;
import com.jcloisterzone.board.Location;
import com.jcloisterzone.board.Position;
import com.jcloisterzone.board.TileTrigger;
import com.jcloisterzone.collection.Sites;
import com.jcloisterzone.feature.City;
import com.jcloisterzone.feature.Tower;
import com.jcloisterzone.figure.Follower;
import com.jcloisterzone.figure.Meeple;
import com.jcloisterzone.figure.SmallFollower;
import com.jcloisterzone.game.Game;
import com.jcloisterzone.game.expansion.BridgesCastlesBazaarsGame;
import com.jcloisterzone.game.phase.Phase;
import com.jcloisterzone.game.phase.TowerCapturePhase;


public class AiActionPhase extends Phase {

    public AiActionPhase(Game game) {
        super(game);
    }

    @Override
    public void enter() {
        next();
    }

    public List<PlayerAction> getActions() {
      List<PlayerAction> actions = Lists.newArrayList();

      Sites commonSites = game.prepareCommonSites();
      if (getActivePlayer().hasFollower(SmallFollower.class)  && ! commonSites.isEmpty()) {
          actions.add(new MeepleAction(SmallFollower.class, commonSites));
      }
      game.expansionDelegate().prepareActions(actions, commonSites);
      return actions;
    }

    @Override
    public void notifyRansomPaid() {
        enter(); //recompute available actions
    }

    @Override
    public void pass() {
        next();
    }

    private int doPlaceTowerPiece(Position p) {
        Tower tower = getBoard().get(p).getTower();
        if (tower  == null) {
            throw new IllegalArgumentException("No tower on tile.");
        }
        if (tower.getMeeple() != null) {
            throw new IllegalArgumentException("The tower is sealed");
        }
        game.getTowerGame().decreaseTowerPieces(getActivePlayer());
        return tower.increaseHeight();
    }

    public TakePrisonerAction prepareCapture(Position p, int range) {
        TakePrisonerAction captureAction = new TakePrisonerAction();
        for(Meeple pf : game.getDeployedMeeples()) {
            if (! (pf instanceof Follower)) continue;
            if (pf.getPosition().x != p.x && pf.getPosition().y != p.y) continue; //check if is in same row or column
            if (pf.getPosition().squareDistance(p) > range) continue;
            captureAction.getOrCreate(pf.getPosition()).add(pf.getLocation());
        }
        return captureAction;
    }

    @Override
    public void placeTowerPiece(Position p) {
        int captureRange = doPlaceTowerPiece(p);
        game.fireGameEvent().towerIncreased(p, captureRange);
        TakePrisonerAction captureAction = prepareCapture(p, captureRange);
        if (captureAction.getSites().isEmpty()) {
            next();
            return;
        }
        next(TowerCapturePhase.class);
        notifyUI(captureAction, false);
    }

    @Override
    public void moveFairy(Position p) {
        for(Follower f : getActivePlayer().getFollowers()) {
            if (p.equals(f.getPosition())) {
                game.getPrincessAndDragonGame().setFairyPosition(p);
                game.fireGameEvent().fairyMoved(p);
                next();
                return;
            }
        }
        throw new IllegalArgumentException("No own follower on the tile");
    }

    private boolean isFestivalUndeploy(Meeple m) {
        return getTile().getTrigger() == TileTrigger.FESTIVAL &&  m.getPlayer() == getActivePlayer();
    }

    private boolean isPrincessUndeploy(Meeple m) {
        //TODO proper validation
        return m.getFeature() instanceof City;
    }

    @Override
    public void undeployMeeple(Position p, Location loc) {
        Meeple m = game.getMeeple(p, loc);
        if (isFestivalUndeploy(m) || isPrincessUndeploy(m)) {
            m.undeploy();
            next();
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public void placeTunnelPiece(Position p, Location loc, boolean isB) {
        game.getTunnelGame().placeTunnelPiece(p, loc, isB);
        next(AiActionPhase.class);
    }


    @Override
    public void deployMeeple(Position p, Location loc, Class<? extends Meeple> meepleType) {
        Meeple m = getActivePlayer().getUndeployedMeeple(meepleType);
        m.deploy(getBoard().get(p), loc);
        next();
    }

    @Override
    public void deployBridge(Position pos, Location loc) {
        BridgesCastlesBazaarsGame bcb = game.getBridgesCastlesBazaarsGame();
        bcb.decreaseBridges(getActivePlayer());
        bcb.deployBridge(pos, loc);
        next(AiActionPhase.class);
    }

}
