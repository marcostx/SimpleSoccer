/**
 *  Desc: Definition of a soccer player base class. <del>The player inherits
 *        from the autolist class so that any player created will be 
 *        automatically added to a list that is easily accesible by any
 *        other game objects.</del> (mainly used by the steering behaviors and
 *        player state classes)
 * 
 * @author Petr (http://www.sallyx.org/)
 */
package SimpleSoccer;

import java.util.ListIterator;
import common.D2.Vector2D;
import common.misc.AutoList;
import java.util.LinkedList;
import java.util.List;
import static SimpleSoccer.ParamLoader.Prm;
import static SimpleSoccer.MessageTypes.*;
import static common.D2.Vector2D.*;
import common.Game.Region;
import static common.Messaging.MessageDispatcher.*;
import static common.misc.utils.*;
import static java.lang.Math.abs;

abstract public class PlayerBase extends MovingEntity implements AutoList.Interface {

    static public enum player_role {

        goal_keeper, attacker, defender
    };
    //this player's role in the team
    protected player_role m_PlayerRole;
    //a pointer to this player's team
    protected SoccerTeam m_pTeam;
    //the steering behaviors
    protected SteeringBehaviors m_pSteering;
    //the region that this player is assigned to.
    protected int m_iHomeRegion;
    //the region this player moves to before kickoff
    protected int m_iDefaultRegion;
    //the distance to the ball (in squared-space). This value is queried 
    //a lot so it's calculated once each time-step and stored here.
    protected double m_dDistSqToBall;
    //the vertex buffer
    protected List<Vector2D> m_vecPlayerVB = new LinkedList<Vector2D>();
    //the buffer for the transformed vertices
    protected List<Vector2D> m_vecPlayerVBTrans = new LinkedList<Vector2D>();

//----------------------------- ctor -------------------------------------
//------------------------------------------------------------------------
    public PlayerBase(SoccerTeam home_team,
            int home_region,
            Vector2D heading,
            Vector2D velocity,
            double mass,
            double max_force,
            double max_speed,
            double max_turn_rate,
            double scale,
            player_role role) {

        super(home_team.Pitch().GetRegionFromIndex(home_region).Center(),
                scale * 10.0,
                velocity,
                max_speed,
                heading,
                mass,
                new Vector2D(scale, scale),
                max_turn_rate,
                max_force);
        m_pTeam = home_team;
        m_dDistSqToBall = MaxFloat;
        m_iHomeRegion = home_region;
        m_iDefaultRegion = home_region;
        m_PlayerRole = role;

        //setup the vertex buffers and calculate the bounding radius
        final Vector2D player[] = {
            new Vector2D(-3, 8),
            new Vector2D(3, 10),
            new Vector2D(3, -10),
            new Vector2D(-3, -8)
        };
        final int NumPlayerVerts = player.length;

        for (int vtx = 0; vtx < NumPlayerVerts; ++vtx) {
            m_vecPlayerVB.add(player[vtx]);

            //set the bounding radius to the length of the 
            //greatest extent
            if (abs(player[vtx].x) > m_dBoundingRadius) {
                m_dBoundingRadius = abs(player[vtx].x);
            }

            if (abs(player[vtx].y) > m_dBoundingRadius) {
                m_dBoundingRadius = abs(player[vtx].y);
            }
        }

        //set up the steering behavior class
        m_pSteering = new SteeringBehaviors(this,
                m_pTeam.Pitch(),
                Ball());

        //a player's start target is its start position (because it's just waiting)
        m_pSteering.SetTarget(home_team.Pitch().GetRegionFromIndex(home_region).Center());
        new AutoList<PlayerBase>().add(this);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        m_pSteering = null;
        new AutoList<PlayerBase>().remove(this);
    }

    /**
     *  returns true if there is an opponent within this player's 
     *  comfort zone
     */
    public boolean isThreatened() {
        //check against all opponents to make sure non are within this
        //player's comfort zone
        ListIterator<PlayerBase> it;
        it = Team().Opponents().Members().listIterator();

        while (it.hasNext()) {
            PlayerBase curOpp = it.next();
            //calculate distance to the player. if dist is less than our
            //comfort zone, and the opponent is infront of the player, return true
            if (PositionInFrontOfPlayer(curOpp.Pos())
                    && (Vec2DDistanceSq(Pos(), curOpp.Pos()) < Prm.PlayerComfortZoneSq)) {
                return true;
            }

        }// next opp

        return false;
    }

    /**
     *  rotates the player to face the ball
     */
    public void TrackBall() {
        RotateHeadingToFacePosition(Ball().Pos());
    }

    /**
     * sets the player's heading to point at the current target
     */
    public void TrackTarget() {
        SetHeading(Vec2DNormalize(sub(Steering().Target(), Pos())));
    }

    /**
     * determines the player who is closest to the SupportSpot and messages him
     * to tell him to change state to SupportAttacker
     */
    public void FindSupport() {
        //if there is no support we need to find a suitable player.
        if (Team().SupportingPlayer() == null) {
            PlayerBase BestSupportPly = Team().DetermineBestSupportingAttacker();
            Team().SetSupportingPlayer(BestSupportPly);
            Dispatcher.DispatchMsg(SEND_MSG_IMMEDIATELY,
                    ID(),
                    Team().SupportingPlayer().ID(),
                    Msg_SupportAttacker,
                    null);
        }

        PlayerBase BestSupportPly = Team().DetermineBestSupportingAttacker();

        //if the best player available to support the attacker changes, update
        //the pointers and send messages to the relevant players to update their
        //states
        if (BestSupportPly != null && (BestSupportPly != Team().SupportingPlayer())) {

            if (Team().SupportingPlayer() != null) {
                Dispatcher.DispatchMsg(SEND_MSG_IMMEDIATELY,
                        ID(),
                        Team().SupportingPlayer().ID(),
                        Msg_GoHome,
                        null);
            }

            Team().SetSupportingPlayer(BestSupportPly);

            Dispatcher.DispatchMsg(SEND_MSG_IMMEDIATELY,
                    ID(),
                    Team().SupportingPlayer().ID(),
                    Msg_SupportAttacker,
                    null);
        }
    }

    /** 
     * @return true if the ball can be grabbed by the goalkeeper 
     */
    public boolean BallWithinKeeperRange() {
        return (Vec2DDistanceSq(Pos(), Ball().Pos()) < Prm.KeeperInBallRangeSq);
    }

    /**
     * @return true if the ball is within kicking range
     */
    public boolean BallWithinKickingRange() {
        return (Vec2DDistanceSq(Ball().Pos(), Pos()) < Prm.PlayerKickingDistanceSq);
    }

    /** 
     * @return true if a ball comes within range of a receiver
     */
    public boolean BallWithinReceivingRange() {
        return (Vec2DDistanceSq(Pos(), Ball().Pos()) < Prm.BallWithinReceivingRangeSq);
    }

    /**
     * @return true if the player is located within the boundaries 
     *        of his home region
     */
    public boolean InHomeRegion() {
        if (m_PlayerRole == player_role.goal_keeper) {
            return Pitch().GetRegionFromIndex(m_iHomeRegion).Inside(Pos(), Region.normal);
        } else {
            return Pitch().GetRegionFromIndex(m_iHomeRegion).Inside(Pos(), Region.halfsize);
        }
    }

    /**
     * 
     * @return true if this player is ahead of the attacker
     */
    public boolean isAheadOfAttacker() {
        return abs(Pos().x - Team().OpponentsGoal().Center().x)
                < abs(Team().ControllingPlayer().Pos().x - Team().OpponentsGoal().Center().x);
    }

    //returns true if a player is located at the designated support spot
    //bool        AtSupportSpot()const;
    /**
     * @return true if the player is located at his steering target
     */
    public boolean AtTarget() {
        return (Vec2DDistanceSq(Pos(), Steering().Target()) < Prm.PlayerInTargetRangeSq);
    }

    /**
     * @return true if the player is the closest player in his team to the ball
     */
    public boolean isClosestTeamMemberToBall() {
        return Team().PlayerClosestToBall() == this;
    }

    /**
     * @param position
     * @return true if the point specified by 'position' is located in
     * front of the player
     */
    public boolean PositionInFrontOfPlayer(Vector2D position) {
        Vector2D ToSubject = sub(position, Pos());

        if (ToSubject.Dot(Heading()) > 0) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * @return true if the player is the closest player on the pitch to the ball
     */
    public boolean isClosestPlayerOnPitchToBall() {
        return isClosestTeamMemberToBall()
                && (DistSqToBall() < Team().Opponents().ClosestDistToBallSq());
    }

    /** 
     * @return true if this player is the controlling player
     */
    public boolean isControllingPlayer() {
        return Team().ControllingPlayer() == this;
    }

    /** 
     * @return true if the player is located in the designated 'hot region' --
     * the area close to the opponent's goal 
     */
    public boolean InHotRegion() {
        return abs(Pos().x - Team().OpponentsGoal().Center().x)
                < Pitch().PlayingArea().Length() / 3.0;
    }

    player_role Role() {
        return m_PlayerRole;
    }

    public double DistSqToBall() {
        return m_dDistSqToBall;
    }

    public void SetDistSqToBall(double val) {
        m_dDistSqToBall = val;
    }

    /**
     *  Calculate distance to opponent's/home goal. Used frequently by the passing methods
     */
    public double DistToOppGoal() {
        return abs(Pos().x - Team().OpponentsGoal().Center().x);
    }

    public double DistToHomeGoal() {
        return abs(Pos().x - Team().HomeGoal().Center().x);
    }

    public void SetDefaultHomeRegion() {
        m_iHomeRegion = m_iDefaultRegion;
    }

    public SoccerBall Ball() {
        return Team().Pitch().Ball();
    }

    public SoccerPitch Pitch() {
        return Team().Pitch();
    }

    public SteeringBehaviors Steering() {
        return m_pSteering;
    }

    public Region HomeRegion() {
        return Pitch().GetRegionFromIndex(m_iHomeRegion);
    }

    public void SetHomeRegion(int NewRegion) {
        m_iHomeRegion = NewRegion;
    }

    public SoccerTeam Team() {
        return m_pTeam;
    }

    /**
     * binary predicates for std::sort (see CanPassForward/Backward)
     */
    static public boolean SortByDistanceToOpponentsGoal(PlayerBase p1,
            PlayerBase p2) {
        return (p1.DistToOppGoal() < p2.DistToOppGoal());
    }

    static public boolean SortByReversedDistanceToOpponentsGoal(PlayerBase p1,
            PlayerBase p2) {
        return (p1.DistToOppGoal() > p2.DistToOppGoal());
    }
}
