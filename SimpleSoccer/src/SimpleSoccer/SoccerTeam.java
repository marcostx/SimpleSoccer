/**
 *  Desc:   class to define a team of soccer playing agents. A SoccerTeam
 *          contains several field players and one goalkeeper. A SoccerTeam
 *          is implemented as a finite state machine and has states for
 *          attacking, defending, and KickOff.
 * 
 * @author Petr (http://www.sallyx.org/)
 */
package SimpleSoccer;

import SimpleSoccer.FieldPlayerStates.ReturnToHomeRegion;
import SimpleSoccer.FieldPlayerStates.Wait;
import SimpleSoccer.TeamStates.PrepareForKickOff;
import SimpleSoccer.TeamStates.Attacking;
import SimpleSoccer.TeamStates.Defending;
import SimpleSoccer.GoalKeeperStates.TendGoal;
import static common.Debug.DbgConsole.*;
import common.D2.Vector2D;
import static common.D2.Vector2D.*;
import static common.D2.Transformation.*;
import static common.D2.geometry.*;
import common.FSM.StateMachine;
import static common.Game.EntityManager.EntityMgr;
import static common.Messaging.MessageDispatcher.*;
import common.misc.Cgdi;
import static common.misc.Cgdi.gdi;
import static common.misc.CppToJava.ObjectRef;
import static common.misc.utils.*;
import static common.misc.Stream_Utility_function.ttos;
import static java.lang.Math.abs;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import static SimpleSoccer.DEFINE.*;
import static SimpleSoccer.ParamLoader.Prm;
import static SimpleSoccer.MessageTypes.*;

public class SoccerTeam {

    public enum team_color {

        blue, red
    };
    public static team_color blue = team_color.blue;
    public static team_color red = team_color.red;
    //an instance of the state machine class
    private StateMachine<SoccerTeam> m_pStateMachine;
    //the team must know its own color!
    private team_color m_Color;
    //pointers to the team members
    private List<PlayerBase> m_Players = new ArrayList<PlayerBase>(5);
    //a pointer to the soccer pitch
    private SoccerPitch m_pPitch;
    //pointers to the goals
    private Goal m_pOpponentsGoal;
    private Goal m_pHomeGoal;
    //a pointer to the opposing team
    private SoccerTeam m_pOpponents;
    //pointers to 'key' players
    private PlayerBase m_pControllingPlayer;
    private PlayerBase m_pSupportingPlayer;
    private PlayerBase m_pReceivingPlayer;
    private PlayerBase m_pPlayerClosestToBall;
    //the squared distance the closest player is from the ball
    private double m_dDistSqToBallOfClosestPlayer;
    //players use this to determine strategic positions on the playing field
    private SupportSpotCalculator m_pSupportSpotCalc;

    /**
     * creates all the players for this team
     */
    private void CreatePlayers() {
        if (Color() == blue) {
            //goalkeeper
            m_Players.add(new GoalKeeper(this,
                    1,
                    TendGoal.Instance(),
                    new Vector2D(0, 1),
                    new Vector2D(0.0, 0.0),
                    Prm.PlayerMass,
                    Prm.PlayerMaxForce,
                    Prm.PlayerMaxSpeedWithoutBall,
                    Prm.PlayerMaxTurnRate,
                    Prm.PlayerScale));

            //create the players
            m_Players.add(new FieldPlayer(this,
                    6,
                    Wait.Instance(),
                    new Vector2D(0, 1),
                    new Vector2D(0.0, 0.0),
                    Prm.PlayerMass,
                    Prm.PlayerMaxForce,
                    Prm.PlayerMaxSpeedWithoutBall,
                    Prm.PlayerMaxTurnRate,
                    Prm.PlayerScale,
                    PlayerBase.player_role.attacker));



            m_Players.add(new FieldPlayer(this,
                    8,
                    Wait.Instance(),
                    new Vector2D(0, 1),
                    new Vector2D(0.0, 0.0),
                    Prm.PlayerMass,
                    Prm.PlayerMaxForce,
                    Prm.PlayerMaxSpeedWithoutBall,
                    Prm.PlayerMaxTurnRate,
                    Prm.PlayerScale,
                    PlayerBase.player_role.attacker));


            m_Players.add(new FieldPlayer(this,
                    3,
                    Wait.Instance(),
                    new Vector2D(0, 1),
                    new Vector2D(0.0, 0.0),
                    Prm.PlayerMass,
                    Prm.PlayerMaxForce,
                    Prm.PlayerMaxSpeedWithoutBall,
                    Prm.PlayerMaxTurnRate,
                    Prm.PlayerScale,
                    PlayerBase.player_role.defender));


            m_Players.add(new FieldPlayer(this,
                    5,
                    Wait.Instance(),
                    new Vector2D(0, 1),
                    new Vector2D(0.0, 0.0),
                    Prm.PlayerMass,
                    Prm.PlayerMaxForce,
                    Prm.PlayerMaxSpeedWithoutBall,
                    Prm.PlayerMaxTurnRate,
                    Prm.PlayerScale,
                    PlayerBase.player_role.defender));

        } else {
            //goalkeeper
            m_Players.add(new GoalKeeper(this,
                    16,
                    TendGoal.Instance(),
                    new Vector2D(0, -1),
                    new Vector2D(0.0, 0.0),
                    Prm.PlayerMass,
                    Prm.PlayerMaxForce,
                    Prm.PlayerMaxSpeedWithoutBall,
                    Prm.PlayerMaxTurnRate,
                    Prm.PlayerScale));


            //create the players
            m_Players.add(new FieldPlayer(this,
                    9,
                    Wait.Instance(),
                    new Vector2D(0, -1),
                    new Vector2D(0.0, 0.0),
                    Prm.PlayerMass,
                    Prm.PlayerMaxForce,
                    Prm.PlayerMaxSpeedWithoutBall,
                    Prm.PlayerMaxTurnRate,
                    Prm.PlayerScale,
                    PlayerBase.player_role.attacker));

            m_Players.add(new FieldPlayer(this,
                    11,
                    Wait.Instance(),
                    new Vector2D(0, -1),
                    new Vector2D(0.0, 0.0),
                    Prm.PlayerMass,
                    Prm.PlayerMaxForce,
                    Prm.PlayerMaxSpeedWithoutBall,
                    Prm.PlayerMaxTurnRate,
                    Prm.PlayerScale,
                    PlayerBase.player_role.attacker));


            m_Players.add(new FieldPlayer(this,
                    12,
                    Wait.Instance(),
                    new Vector2D(0, -1),
                    new Vector2D(0.0, 0.0),
                    Prm.PlayerMass,
                    Prm.PlayerMaxForce,
                    Prm.PlayerMaxSpeedWithoutBall,
                    Prm.PlayerMaxTurnRate,
                    Prm.PlayerScale,
                    PlayerBase.player_role.defender));


            m_Players.add(new FieldPlayer(this,
                    14,
                    Wait.Instance(),
                    new Vector2D(0, -1),
                    new Vector2D(0.0, 0.0),
                    Prm.PlayerMass,
                    Prm.PlayerMaxForce,
                    Prm.PlayerMaxSpeedWithoutBall,
                    Prm.PlayerMaxTurnRate,
                    Prm.PlayerScale,
                    PlayerBase.player_role.defender));

        }

        //register the players with the entity manager
        ListIterator<PlayerBase> it = m_Players.listIterator();

        while (it.hasNext()) {
            EntityMgr.RegisterEntity(it.next());
        }
    }

    /**
     * called each frame. Sets m_pClosestPlayerToBall to point to the player
     * closest to the ball. 
     */
    private void CalculateClosestPlayerToBall() {
        double ClosestSoFar = MaxFloat;

        ListIterator<PlayerBase> it = m_Players.listIterator();

        while (it.hasNext()) {
            PlayerBase cur = it.next();
            //calculate the dist. Use the squared value to avoid sqrt
            double dist = Vec2DDistanceSq(cur.Pos(), Pitch().Ball().Pos());

            //keep a record of this value for each player
            cur.SetDistSqToBall(dist);

            if (dist < ClosestSoFar) {
                ClosestSoFar = dist;

                m_pPlayerClosestToBall = cur;
            }
        }

        m_dDistSqToBallOfClosestPlayer = ClosestSoFar;
    }

//----------------------------- ctor -------------------------------------
//
//------------------------------------------------------------------------
    public SoccerTeam(Goal home_goal,
            Goal opponents_goal,
            SoccerPitch pitch,
            team_color color) {
        m_pOpponentsGoal = opponents_goal;
        m_pHomeGoal = home_goal;
        m_pOpponents = null;
        m_pPitch = pitch;
        m_Color = color;
        m_dDistSqToBallOfClosestPlayer = 0.0;
        m_pSupportingPlayer = null;
        m_pReceivingPlayer = null;
        m_pControllingPlayer = null;
        m_pPlayerClosestToBall = null;

        //setup the state machine
        m_pStateMachine = new StateMachine<SoccerTeam>(this);

        m_pStateMachine.SetCurrentState(Defending.Instance());
        m_pStateMachine.SetPreviousState(Defending.Instance());
        m_pStateMachine.SetGlobalState(null);

        //create the players and goalkeeper
        CreatePlayers();

        //set default steering behaviors
        ListIterator<PlayerBase> it = m_Players.listIterator();

        while (it.hasNext()) {
            it.next().Steering().SeparationOn();
        }

        //create the sweet spot calculator
        m_pSupportSpotCalc = new SupportSpotCalculator(Prm.NumSupportSpotsX,
                Prm.NumSupportSpotsY,
                this);
    }

    //----------------------- dtor -------------------------------------------
//
//------------------------------------------------------------------------
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        m_pStateMachine = null;

        m_Players.clear();

        m_pSupportSpotCalc = null;
    }

    /**
     *  renders the players and any team related info
     */
    public void Render() {
        ListIterator<PlayerBase> it = m_Players.listIterator();

        while (it.hasNext()) {
            it.next().Render();
        }

        //show the controlling team and player at the top of the display
        if (Prm.bShowControllingTeam) {
            gdi.TextColor(Cgdi.white);

            if ((Color() == blue) && InControl()) {
                gdi.TextAtPos(20, 3, "Blue in Control");
            } else if ((Color() == red) && InControl()) {
                gdi.TextAtPos(20, 3, "Red in Control");
            }
            if (m_pControllingPlayer != null) {
                gdi.TextAtPos(Pitch().cxClient() - 150, 3,
                        "Controlling Player: " + ttos(m_pControllingPlayer.ID()));
            }
        }

        //render the sweet spots
        if (Prm.bSupportSpots && InControl()) {
            m_pSupportSpotCalc.Render();
        }

//define(SHOW_TEAM_STATE);
        if (def(SHOW_TEAM_STATE)) {
            if (Color() == red) {
                gdi.TextColor(Cgdi.white);

                if (m_pStateMachine.CurrentState() == Attacking.Instance()) {
                    gdi.TextAtPos(160, 20, "Attacking");
                }
                if (m_pStateMachine.CurrentState() == Defending.Instance()) {
                    gdi.TextAtPos(160, 20, "Defending");
                }
                if (m_pStateMachine.CurrentState() == PrepareForKickOff.Instance()) {
                    gdi.TextAtPos(160, 20, "Kickoff");
                }
            } else {
                if (m_pStateMachine.CurrentState() == Attacking.Instance()) {
                    gdi.TextAtPos(160, Pitch().cyClient() - 40, "Attacking");
                }
                if (m_pStateMachine.CurrentState() == Defending.Instance()) {
                    gdi.TextAtPos(160, Pitch().cyClient() - 40, "Defending");
                }
                if (m_pStateMachine.CurrentState() == PrepareForKickOff.Instance()) {
                    gdi.TextAtPos(160, Pitch().cyClient() - 40, "Kickoff");
                }
            }
        }

// define(SHOW_SUPPORTING_PLAYERS_TARGET)
        if (def(SHOW_SUPPORTING_PLAYERS_TARGET)) {
            if (m_pSupportingPlayer != null) {
                gdi.BlueBrush();
                gdi.RedPen();
                gdi.Circle(m_pSupportingPlayer.Steering().Target(), 4);
            }
        }

    }

    /**
     *  iterates through each player's update function and calculates 
     *  frequently accessed info
     */
    public void Update() {
        //this information is used frequently so it's more efficient to 
        //calculate it just once each frame
        CalculateClosestPlayerToBall();

        //the team state machine switches between attack/defense behavior. It
        //also handles the 'kick off' state where a team must return to their
        //kick off positions before the whistle is blown
        m_pStateMachine.Update();

        //now update each player
        ListIterator<PlayerBase> it = m_Players.listIterator();

        while (it.hasNext()) {
            it.next().Update();
        }

    }

    /**
     * calling this changes the state of all field players to that of 
     * ReturnToHomeRegion. Mainly used when a goal keeper has
     * possession
     */
    public void ReturnAllFieldPlayersToHome() {
        ListIterator<PlayerBase> it = m_Players.listIterator();

        while (it.hasNext()) {
            PlayerBase cur = it.next();
            if (cur.Role() != PlayerBase.player_role.goal_keeper) {
                Dispatcher.DispatchMsg(SEND_MSG_IMMEDIATELY,
                        1,
                        cur.ID(),
                        Msg_GoHome,
                        null);
            }
        }
    }

    /**
     *  Given a ball position, a kicking power and a reference to a vector2D
     *  this function will sample random positions along the opponent's goal-
     *  mouth and check to see if a goal can be scored if the ball was to be
     *  kicked in that direction with the given power. If a possible shot is 
     *  found, the function will immediately return true, with the target 
     *  position stored in the vector ShotTarget.
    
     * returns true if player has a clean shot at the goal and sets ShotTarget
     * to a normalized vector pointing in the direction the shot should be
     * made. Else returns false and sets heading to a zero vector
     */
    public boolean CanShoot(Vector2D BallPos, double power) {
        return CanShoot(BallPos, power, new Vector2D());
    }

    public boolean CanShoot(Vector2D BallPos,
            double power,
            Vector2D ShotTarget) {
        //the number of randomly created shot targets this method will test 
        int NumAttempts = Prm.NumAttemptsToFindValidStrike;

        while (NumAttempts-- > 0) {
            //choose a random position along the opponent's goal mouth. (making
            //sure the ball's radius is taken into account)
            ShotTarget.set(OpponentsGoal().Center());

            //the y value of the shot position should lay somewhere between two
            //goalposts (taking into consideration the ball diameter)
            int MinYVal = (int) (OpponentsGoal().LeftPost().y + Pitch().Ball().BRadius());
            int MaxYVal = (int) (OpponentsGoal().RightPost().y - Pitch().Ball().BRadius());

            ShotTarget.y = (double) RandInt(MinYVal, MaxYVal);

            //make sure striking the ball with the given power is enough to drive
            //the ball over the goal line.
            double time = Pitch().Ball().TimeToCoverDistance(BallPos,
                    ShotTarget,
                    power);

            //if it is, this shot is then tested to see if any of the opponents
            //can intercept it.
            if (time >= 0) {
                if (isPassSafeFromAllOpponents(BallPos, ShotTarget, null, power)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * The best pass is considered to be the pass that cannot be intercepted 
     * by an opponent and that is as far forward of the receiver as possible  
     * If a pass is found, the receiver's address is returned in the 
     * reference, 'receiver' and the position the pass will be made to is 
     * returned in the  reference 'PassTarget'
     */
    public boolean FindPass(final PlayerBase passer,
            ObjectRef<PlayerBase> receiver,
            Vector2D PassTarget,
            double power,
            double MinPassingDistance) {
        assert (receiver != null);
        assert (PassTarget != null);
        ListIterator<PlayerBase> it = Members().listIterator();

        double ClosestToGoalSoFar = MaxFloat;
        Vector2D Target = new Vector2D();

        boolean finded = false;
        //iterate through all this player's team members and calculate which
        //one is in a position to be passed the ball 
        while (it.hasNext()) {
            PlayerBase curPlyr = it.next();
            //make sure the potential receiver being examined is not this player
            //and that it is further away than the minimum pass distance
            if ((curPlyr != passer)
                    && (Vec2DDistanceSq(passer.Pos(), curPlyr.Pos())
                    > MinPassingDistance * MinPassingDistance)) {
                if (GetBestPassToReceiver(passer, curPlyr, Target, power)) {
                    //if the pass target is the closest to the opponent's goal line found
                    // so far, keep a record of it
                    double Dist2Goal = abs(Target.x - OpponentsGoal().Center().x);

                    if (Dist2Goal < ClosestToGoalSoFar) {
                        ClosestToGoalSoFar = Dist2Goal;

                        //keep a record of this player
                        receiver.set(curPlyr);

                        //and the target
                        PassTarget.set(Target);

                        finded = true;
                    }
                }
            }
        }//next team member

        return finded;
    }

    /**
     *  Three potential passes are calculated. One directly toward the receiver's
     *  current position and two that are the tangents from the ball position
     *  to the circle of radius 'range' from the receiver.
     *  These passes are then tested to see if they can be intercepted by an
     *  opponent and to make sure they terminate within the playing area. If
     *  all the passes are invalidated the function returns false. Otherwise
     *  the function returns the pass that takes the ball closest to the 
     *  opponent's goal area.
     */
    public boolean GetBestPassToReceiver(final PlayerBase passer,
            final PlayerBase receiver,
            Vector2D PassTarget,
            double power) {
        assert (PassTarget != null);
        //first, calculate how much time it will take for the ball to reach 
        //this receiver, if the receiver was to remain motionless 
        double time = Pitch().Ball().TimeToCoverDistance(Pitch().Ball().Pos(),
                receiver.Pos(),
                power);

        //return false if ball cannot reach the receiver after having been
        //kicked with the given power
        if (time < 0) {
            return false;
        }

        //the maximum distance the receiver can cover in this time
        double InterceptRange = time * receiver.MaxSpeed();

        //Scale the intercept range
        final double ScalingFactor = 0.3;
        InterceptRange *= ScalingFactor;

        //now calculate the pass targets which are positioned at the intercepts
        //of the tangents from the ball to the receiver's range circle.
        Vector2D ip1 = new Vector2D(), ip2 = new Vector2D();

        GetTangentPoints(receiver.Pos(),
                InterceptRange,
                Pitch().Ball().Pos(),
                ip1,
                ip2);

        Vector2D Passes[] = {ip1, receiver.Pos(), ip2};
        final int NumPassesToTry = Passes.length;

        // this pass is the best found so far if it is:
        //
        //  1. Further upfield than the closest valid pass for this receiver
        //     found so far
        //  2. Within the playing area
        //  3. Cannot be intercepted by any opponents

        double ClosestSoFar = MaxFloat;
        boolean bResult = false;

        for (int pass = 0; pass < NumPassesToTry; ++pass) {
            double dist = abs(Passes[pass].x - OpponentsGoal().Center().x);

            if ((dist < ClosestSoFar)
                    && Pitch().PlayingArea().Inside(Passes[pass])
                    && isPassSafeFromAllOpponents(Pitch().Ball().Pos(),
                    Passes[pass],
                    receiver,
                    power)) {
                ClosestSoFar = dist;
                PassTarget.set(Passes[pass]);
                bResult = true;
            }
        }

        return bResult;
    }

    /**
     * test if a pass from positions 'from' to 'target' kicked with force 
     * 'PassingForce'can be intercepted by an opposing player
     */
    public boolean isPassSafeFromOpponent(Vector2D from,
            Vector2D target,
            final PlayerBase receiver,
            final PlayerBase opp,
            double PassingForce) {
        //move the opponent into local space.
        Vector2D ToTarget = sub(target, from);
        Vector2D ToTargetNormalized = Vec2DNormalize(ToTarget);

        Vector2D LocalPosOpp = PointToLocalSpace(opp.Pos(),
                ToTargetNormalized,
                ToTargetNormalized.Perp(),
                from);

        //if opponent is behind the kicker then pass is considered okay(this is 
        //based on the assumption that the ball is going to be kicked with a 
        //velocity greater than the opponent's max velocity)
        if (LocalPosOpp.x < 0) {
            return true;
        }

        //if the opponent is further away than the target we need to consider if
        //the opponent can reach the position before the receiver.
        if (Vec2DDistanceSq(from, target) < Vec2DDistanceSq(opp.Pos(), from)) {
            if (receiver != null) {
                if (Vec2DDistanceSq(target, opp.Pos())
                        > Vec2DDistanceSq(target, receiver.Pos())) {
                    return true;
                } else {
                    return false;
                }

            } else {
                return true;
            }
        }

        //calculate how long it takes the ball to cover the distance to the 
        //position orthogonal to the opponents position
        double TimeForBall =
                Pitch().Ball().TimeToCoverDistance(new Vector2D(0, 0),
                new Vector2D(LocalPosOpp.x, 0),
                PassingForce);

        //now calculate how far the opponent can run in this time
        double reach = opp.MaxSpeed() * TimeForBall
                + Pitch().Ball().BRadius()
                + opp.BRadius();

        //if the distance to the opponent's y position is less than his running
        //range plus the radius of the ball and the opponents radius then the
        //ball can be intercepted
        if (abs(LocalPosOpp.y) < reach) {
            return false;
        }

        return true;
    }

    /**
     * tests a pass from position 'from' to position 'target' against each member
     * of the opposing team. Returns true if the pass can be made without
     * getting intercepted
     */
    public boolean isPassSafeFromAllOpponents(Vector2D from,
            Vector2D target,
            final PlayerBase receiver,
            double PassingForce) {
        ListIterator<PlayerBase> opp = Opponents().Members().listIterator();

        while (opp.hasNext()) {
            if (!isPassSafeFromOpponent(from, target, receiver, opp.next(), PassingForce)) {
                debug_on();

                return false;
            }
        }

        return true;
    }

    /**
     * returns true if an opposing player is within the radius of the position
     * given as a par ameter
     */
    public boolean isOpponentWithinRadius(Vector2D pos, double rad) {
        ListIterator<PlayerBase> it = Opponents().Members().listIterator();

        while (it.hasNext()) {
            if (Vec2DDistanceSq(pos, it.next().Pos()) < rad * rad) {
                return true;
            }
        }

        return false;
    }

    /**
     * this tests to see if a pass is possible between the requester and
     * the controlling player. If it is possible a message is sent to the
     * controlling player to pass the ball asap.
     */
    public void RequestPass(FieldPlayer requester) {
        //maybe put a restriction here
        if (RandFloat() > 0.1) {
            return;
        }

        if (isPassSafeFromAllOpponents(ControllingPlayer().Pos(),
                requester.Pos(),
                requester,
                Prm.MaxPassingForce)) {

            //tell the player to make the pass
            //let the receiver know a pass is coming 
            Dispatcher.DispatchMsg(SEND_MSG_IMMEDIATELY,
                    requester.ID(),
                    ControllingPlayer().ID(),
                    Msg_PassToMe,
                    requester);

        }
    }

    /**
     * calculate the closest player to the SupportSpot
     */
    public PlayerBase DetermineBestSupportingAttacker() {
        double ClosestSoFar = MaxFloat;

        PlayerBase BestPlayer = null;

        ListIterator<PlayerBase> it = m_Players.listIterator();

        while (it.hasNext()) {
            PlayerBase cur = it.next();
            //only attackers utilize the BestSupportingSpot
            if ((cur.Role() == PlayerBase.player_role.attacker) && (cur != m_pControllingPlayer)) {
                //calculate the dist. Use the squared value to avoid sqrt
                double dist = Vec2DDistanceSq(cur.Pos(), m_pSupportSpotCalc.GetBestSupportingSpot());

                //if the distance is the closest so far and the player is not a
                //goalkeeper and the player is not the one currently controlling
                //the ball, keep a record of this player
                if ((dist < ClosestSoFar)) {
                    ClosestSoFar = dist;
                    BestPlayer = cur;
                }
            }
        }

        return BestPlayer;
    }

    public List<PlayerBase> Members() {
        return m_Players;
    }

    public StateMachine<SoccerTeam> GetFSM() {
        return m_pStateMachine;
    }

    public Goal HomeGoal() {
        return m_pHomeGoal;
    }

    public Goal OpponentsGoal() {
        return m_pOpponentsGoal;
    }

    public SoccerPitch Pitch() {
        return m_pPitch;
    }

    public SoccerTeam Opponents() {
        return m_pOpponents;
    }

    public void SetOpponents(SoccerTeam opps) {
        m_pOpponents = opps;
    }

    public team_color Color() {
        return m_Color;
    }

    public void SetPlayerClosestToBall(PlayerBase plyr) {
        m_pPlayerClosestToBall = plyr;
    }

    public PlayerBase PlayerClosestToBall() {
        return m_pPlayerClosestToBall;
    }

    public double ClosestDistToBallSq() {
        return m_dDistSqToBallOfClosestPlayer;
    }

    public Vector2D GetSupportSpot() {
        return new Vector2D(m_pSupportSpotCalc.GetBestSupportingSpot());
    }

    public PlayerBase SupportingPlayer() {
        return m_pSupportingPlayer;
    }

    public void SetSupportingPlayer(PlayerBase plyr) {
        m_pSupportingPlayer = plyr;
    }

    public PlayerBase Receiver() {
        return m_pReceivingPlayer;
    }

    public void SetReceiver(PlayerBase plyr) {
        m_pReceivingPlayer = plyr;
    }

    public PlayerBase ControllingPlayer() {
        return m_pControllingPlayer;
    }

    public void SetControllingPlayer(PlayerBase plyr) {
        m_pControllingPlayer = plyr;

        //rub it in the opponents faces!
        Opponents().LostControl();
    }

    public boolean InControl() {
        if (m_pControllingPlayer != null) {
            return true;
        } else {
            return false;
        }
    }

    public void LostControl() {
        m_pControllingPlayer = null;
    }

    public PlayerBase GetPlayerFromID(int id) {
        ListIterator<PlayerBase> it = m_Players.listIterator();

        while (it.hasNext()) {
            PlayerBase cur = it.next();
            if (cur.ID() == id) {
                return cur;
            }
        }

        return null;
    }

    public void SetPlayerHomeRegion(int plyr, int region) {
        assert ((plyr >= 0) && (plyr < (int) m_Players.size()));

        m_Players.get(plyr).SetHomeRegion(region);
    }

    public void DetermineBestSupportingPosition() {
        m_pSupportSpotCalc.DetermineBestSupportingPosition();
    }

    //---------------------- UpdateTargetsOfWaitingPlayers ------------------------
    //
    //  
    public void UpdateTargetsOfWaitingPlayers() {
        ListIterator<PlayerBase> it = m_Players.listIterator();

        while (it.hasNext()) {
            PlayerBase cur = it.next();
            if (cur.Role() != PlayerBase.player_role.goal_keeper) {
                //cast to a field player
                FieldPlayer plyr = (FieldPlayer) cur;

                if (plyr.GetFSM().isInState(Wait.Instance())
                        || plyr.GetFSM().isInState(ReturnToHomeRegion.Instance())) {
                    plyr.Steering().SetTarget(plyr.HomeRegion().Center());
                }
            }
        }
    }

    /**
     * @return false if any of the team are not located within their home region
     */
    public boolean AllPlayersAtHome() {
        ListIterator<PlayerBase> it = m_Players.listIterator();

        while (it.hasNext()) {
            if (it.next().InHomeRegion() == false) {
                return false;
            }
        }

        return true;
    }

    /**
     * @return Name of the team ("Red" or "Blue")
     */
    public String Name() {
        if (m_Color == blue) {
            return "Blue";
        }
        return "Red";
    }
}
