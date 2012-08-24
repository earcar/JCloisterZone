package com.jcloisterzone.ai.starplayer;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.jcloisterzone.Expansion;
import com.jcloisterzone.Player;
import com.jcloisterzone.action.PlayerAction;
import com.jcloisterzone.ai.AiPlayer;
import com.jcloisterzone.ai.AiScoreContext;
import com.jcloisterzone.ai.phase.AiScorePhase;
import com.jcloisterzone.ai.starplayer.PositionEvaluatorScoreContext;
import com.jcloisterzone.board.Position;
import com.jcloisterzone.board.Tile;
import com.jcloisterzone.feature.Castle;
import com.jcloisterzone.feature.City;
import com.jcloisterzone.feature.Cloister;
import com.jcloisterzone.feature.Completable;
import com.jcloisterzone.feature.Farm;
import com.jcloisterzone.feature.Feature;
import com.jcloisterzone.feature.Road;
import com.jcloisterzone.feature.score.ScoreAllCallback;
import com.jcloisterzone.feature.score.ScoreAllFeatureFinder;
import com.jcloisterzone.feature.visitor.score.CityScoreContext;
import com.jcloisterzone.feature.visitor.score.CompletableScoreContext;
import com.jcloisterzone.feature.visitor.score.FarmScoreContext;
import com.jcloisterzone.feature.visitor.score.RoadScoreContext;
import com.jcloisterzone.figure.Barn;
import com.jcloisterzone.figure.BigFollower;
import com.jcloisterzone.figure.Builder;
import com.jcloisterzone.figure.Follower;
import com.jcloisterzone.figure.Meeple;
import com.jcloisterzone.figure.SmallFollower;
import com.jcloisterzone.game.Game;
import com.jcloisterzone.game.expansion.TradersAndBuildersGame;
import com.jcloisterzone.game.expansion.TradersAndBuildersGame.BuilderState;

public class PositionEvaluator extends AiPlayer {
  public static final double L = -250; // lower...
  public static final double U = 250; // ...and upper bounds of the evaluation function
  // private Game game;
  private Map<Feature, AiScoreContext> scoreCache;

  private Player turnPlayer;
  private static final double TRAPPED_MY_FIGURE_POINTS = -12.0;
  private static final double TRAPPED_ENEMY_FIGURE_POINTS = 3.0;
  private static final double MIN_CHANCE = 0.4;

  private static final double[] openRoadPenalty = { 0.0, 0.2, 0.5, 2.5, 5.5,
      9.5, 15.0, 22.0, 30.0 };
  private static final double[] openCityPenalty = { 0.0, 0.15, 0.3, 1.3, 2.3,
      5.3, 10.0, 15.0, 22.0 };
  private static final double[] openFarmPenalty = { 0.0, 5.0, 10.0, 15.0, 20.0,
      25.0, 30.0, 35.0, 40.0 };
  private static final double[] openCloisterPenalty = { 0.0, 0.0, 0.4, 0.8,
      1.2, 2.0, 4.0, 7.0, 11.0 };
  private static final double[][] openPenalties = { openRoadPenalty,
      openCityPenalty, openFarmPenalty, openCloisterPenalty };

  private int tilePackSize;
  private int enemyPlayers;
  private int myTurnsLeft;
  private int openRoadCount;
  private int openCityCount;
  private int openFarmCount;
  private int openCloisterCount;

  private void initVariables() {
    assert getGame() != null;
    tilePackSize = getGame().getTilePack().totalSize();
    enemyPlayers = getGame().getAllPlayers().length - 1;
    myTurnsLeft = ((tilePackSize - 1) / (enemyPlayers + 1)) + 1;
    openRoadCount = 0;
    openCityCount = 0;
    openFarmCount = 0;
    openCloisterCount = 0;
    setPlayer(turnPlayer);
  }

  public PositionEvaluator(Player aiPlayer, Game game, Map<Feature, AiScoreContext> scoreCache) {
    setGame(game);
    this.scoreCache = scoreCache;
    this.turnPlayer = aiPlayer;
    initVariables();
  }

  public double evaluate() {
    double value = 0;
    initVariables();

    // trigger score
    getGame().getPhase().next(AiScorePhase.class);
    getGame().getPhase().enter();

    value += meepleRating();
    value += pointRating();
    value += openObjectRating();

    value += rankPossibleFeatureConnections();
    value += rankConvexity();
    value += rankFairy();

    return value;
  }

  private double rankConvexity() {
    Tile tile = getGame().getCurrentTile();
    Position pos = tile.getPosition();
//    logger.info("rankConvexity(): pos.x = {}, pos.y = {}", pos.x, pos.y);
    return 0.001 * getGame().getBoard().getAdjacentAndDiagonalTiles(pos).size();
  }

  private double rankPossibleFeatureConnections() {
    // TODO Auto-generated method stub
    return 0;
  }

  private double rankFairy() {
    double rating = 0;
    if (!getGame().hasExpansion(Expansion.PRINCESS_AND_DRAGON))
      rating = 0;
    return rating;
  }

  // private Player getPlayer() {
  // return game.getActivePlayer();
  // }

   protected boolean isMe(Player p) {
     return p.getIndex() == this.turnPlayer.getIndex();
   }

  protected double reducePoints(double points, Player p) {
    if (isMe(p)) {
      return points;
    } else {
      return -points / enemyPlayers;
    }
  }

  protected double meepleRating() {
    double rating = 0;

    for (Player p : getGame().getAllPlayers()) {
      double meeplePoints = 0;
      int limit = 0;
      for (Follower f : p.getFollowers()) {
        if (f.isDeployed()) {
          if (f instanceof SmallFollower) {
            meeplePoints += 0.15;
          } else if (f instanceof BigFollower) {
            meeplePoints += 0.25;
          }
          if (++limit == myTurnsLeft)
            break;
        }
      }
      rating += reducePoints(meeplePoints, p);
    }
    return rating;
  }

  protected double pointRating() {
    double rating = 0;

    for (Player p : getGame().getAllPlayers()) {
      rating += reducePoints(p.getPoints(), p);
    }

    ScoreAllFeatureFinder scoreAll = new ScoreAllFeatureFinder();
    PositionEvaluatorScoreAllCallback callback = new PositionEvaluatorScoreAllCallback();
    scoreAll.scoreAll(getGame(), callback);
    rating += callback.getRanking();

    return rating;
  }

  class PositionEvaluatorScoreAllCallback implements ScoreAllCallback {

    private double rank = 0;

    @Override
    public void scoreCastle(Meeple meeple, Castle castle) {
      throw new UnsupportedOperationException();
    }

    @Override
    public CompletableScoreContext getCompletableScoreContext(
        Completable completable) {
      return new PositionEvaluatorScoreContext(getGame(),
          completable.getScoreContext(), scoreCache);
    }

    @Override
    public FarmScoreContext getFarmScoreContext(Farm farm) {
      return new PositionEvaluatorFarmScoreContext(getGame(), scoreCache);
    }

    @Override
    public void scoreFarm(FarmScoreContext ctx, Player player) {
      double points = getFarmPoints((Farm) ctx.getMasterFeature(), player, ctx);
      rank += reducePoints(points, player);
    }

    @Override
    public void scoreBarn(FarmScoreContext ctx, Barn meeple) {
      // prefer barn placement - magic constant
      rank += reducePoints(1.2 * ctx.getBarnPoints(), meeple.getPlayer());
    }

    @Override
    public void scoreCompletableFeature(CompletableScoreContext ctx) {
      rank += rankUnfishedCompletable(ctx.getMasterFeature(),
          (PositionEvaluatorScoreContext) ctx);
      rank += rankTrappedMeeples((PositionEvaluatorScoreContext) ctx);
      rank += rankSpecialFigures((PositionEvaluatorScoreContext) ctx);
    }

    public double getRanking() {
      return rank;
    }

  }

  protected double rankSpecialFigures(PositionEvaluatorScoreContext ctx) {
    double rating = 0.0;
    for (Meeple m : ctx.getSpecialMeeples()) {
      if (m instanceof Builder && isMe(m.getPlayer())) {
        rating += rankBuilder((Builder) m, ctx);
      }
    }
    return rating;
  }

  protected double rankBuilder(Builder builder,
      PositionEvaluatorScoreContext ctx) {
    if (!ctx.getMajorOwners().contains(getPlayer())) {
      return -3.0; // builder in enemy object penalty
    }
    if (ctx.getChanceToClose() < 0.55)
      return 0.0;
    double rating = 0.0;
    // builder placed in object
    if (builder.getFeature() instanceof City) {
      rating += 1.5;
    } else {
      rating += 0.5;
    }

    TradersAndBuildersGame tb = getGame().getTradersAndBuildersGame();
    // builder used on object
    if (tb.getBuilderState() == BuilderState.ACTIVATED) {
      rating += 3.5;
    }
    return rating;
  }

  private double rankTrappedMeeples(PositionEvaluatorScoreContext ctx) {
    // musi tu byt dolni mez - btw nestaci toto misto hodnoceni figurek, spis
    // asi :)

    // TODO lepe
    if (myTurnsLeft < 8)
      return 0.0;

    if (ctx.getChanceToClose() > 0.4)
      return 0.0;

    double rating = 0.0;
    for (Meeple m : ctx.getMeeples()) {
      if (isMe(m.getPlayer())) {
        rating += TRAPPED_MY_FIGURE_POINTS;
      } else {
        rating += TRAPPED_ENEMY_FIGURE_POINTS;
      }
    }
    return (1.0 - ctx.getChanceToClose()) * rating; // no reduce
  }

  protected double getFarmPoints(Farm farm, Player p, FarmScoreContext ctx) {
    if (isMe(p)) {
      openFarmCount++;
    }
    return ctx.getPoints(p);
  }

  protected double rankUnfishedCompletable(Completable completable,
      PositionEvaluatorScoreContext ctx) {
    double rating = 0.0;
    double points = getUnfinishedCompletablePoints(completable, ctx);
    for (Player p : ctx.getMajorOwners()) {
      rating += reducePoints(points, p);
    }
    return rating;
  }

  protected double getUnfinishedCompletablePoints(Completable complatable,
      PositionEvaluatorScoreContext ctx) {
    if (complatable instanceof City) {
      return getUnfinishedCityPoints((City) complatable, ctx);
    }
    if (complatable instanceof Road) {
      return getUnfinishedRoadPoints((Road) complatable, ctx);
    }
    if (complatable instanceof Cloister) {
      return getUnfinishedCloisterPoints((Cloister) complatable, ctx);
    }
    throw new IllegalArgumentException();
  }

  protected double getUnfinishedCityPoints(City city,
      PositionEvaluatorScoreContext ctx) {
    double chanceToClose = ctx.getChanceToClose();

    if (chanceToClose > MIN_CHANCE
        && ctx.getMajorOwners().contains(getPlayer())) {
      openCityCount++;
    }

    // legacy heuristic
    CityScoreContext cityCtx = (CityScoreContext) ctx
        .getCompletableScoreContext();
    if (chanceToClose < MIN_CHANCE) {
      return cityCtx.getPoints(false) + 3.0 * chanceToClose;
    } else {
      return cityCtx.getPoints(true) - 3.0 * (1.0 - chanceToClose);
    }
  }

  protected double getUnfinishedRoadPoints(Road road,
      PositionEvaluatorScoreContext ctx) {
    double chanceToClose = ctx.getChanceToClose();
    ;

    if (chanceToClose > MIN_CHANCE
        && ctx.getMajorOwners().contains(getPlayer())) {
      openRoadCount++;
    }

    // legacy heuristic
    RoadScoreContext roadCtx = (RoadScoreContext) ctx
        .getCompletableScoreContext();
    if (chanceToClose < MIN_CHANCE) {
      return roadCtx.getPoints(false) + 3.0 * chanceToClose;
    } else {
      return roadCtx.getPoints(true) - 3.0 * (1.0 - chanceToClose);
    }

  }

  protected double getUnfinishedCloisterPoints(Cloister cloister,
      PositionEvaluatorScoreContext ctx) {
    if (isMe(cloister.getMeeple().getPlayer())) {
      openCloisterCount++;
    }
    double chanceToClose = ctx.getChanceToClose();
    int points = ctx.getPoints();
    return points + (9 - points) * chanceToClose;
  }

  protected double openObjectRating() {
    double rating = 0;
    int[] openCount = { openRoadCount, openCityCount, openFarmCount,
        openCloisterCount };
    for (int i = 0; i < openPenalties.length; i++) {
      double penalty;
      // fast fix for strange bug causes ArrayIndexOutOfBoundsException: 9
      if (openCount[i] >= openPenalties[i].length) {
        penalty = openPenalties[i][openPenalties[i].length - 1];
      } else {
        penalty = openPenalties[i][openCount[i]];
      }
      if (i == 2) {
        // Farm
        double modifier = (tilePackSize - ((1 + enemyPlayers) * 3)) / 20.0;
        if (modifier < 1.0)
          modifier = 1.0;
        rating -= modifier * penalty;
      } else {
        rating -= penalty;
      }
    }
    return rating;
  }

  @Override
  public void selectAction(List<PlayerAction> actions, boolean canPass) {
    // TODO Auto-generated method stub

  }

  @Override
  public void selectBazaarTile() {
    // TODO Auto-generated method stub

  }

  @Override
  public void makeBazaarBid(int supplyIndex) {
    // TODO Auto-generated method stub

  }

  @Override
  public void selectBuyOrSellBazaarOffer(int supplyIndex) {
    // TODO Auto-generated method stub

  }

  @Override
  public void selectDragonMove(Set<Position> positions, int movesLeft) {
    // TODO Auto-generated method stub

  }
}
