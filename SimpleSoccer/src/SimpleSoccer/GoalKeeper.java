/**
 * Desc:   class to implement a goalkeeper agent
 * 
 * @author Petr (http://www.sallyx.org/)
 */
package SimpleSoccer;

import SimpleSoccer.GoalKeeperStates.GlobalKeeperState;
import common.Messaging.Telegram;
import common.misc.AutoList;
import common.D2.Vector2D;
import static common.D2.Vector2D.*;
import static common.D2.Transformation.WorldTransform;
import common.FSM.State;
import common.FSM.StateMachine;
import static common.misc.Cgdi.gdi;
import static common.misc.Stream_Utility_function.ttos;
import static common.Game.EntityFunctionTemplates.EnforceNonPenetrationContraint;
import static SimpleSoccer.ParamLoader.Prm;

public class GoalKeeper extends PlayerBase {
    //an instance of the state machine class

    private StateMachine<GoalKeeper> m_pStateMachine;
    //this vector is updated to point towards the ball and is used when
    //rendering the goalkeeper (instead of the underlaying vehicle's heading)
    //to ensure he always appears to be watching the ball
    private Vector2D m_vLookAt = new Vector2D();

    //----------------------------- ctor ------------------------------------
//-----------------------------------------------------------------------
    public GoalKeeper(SoccerTeam home_team,
            int home_region,
            State<GoalKeeper> start_state,
            Vector2D heading,
            Vector2D velocity,
            double mass,
            double max_force,
            double max_speed,
            double max_turn_rate,
            double scale) {
        super(home_team,
                home_region,
                heading,
                velocity,
                mass,
                max_force,
                max_speed,
                max_turn_rate,
                scale,
                PlayerBase.player_role.goal_keeper);



        //set up the state machine
        m_pStateMachine = new StateMachine<GoalKeeper>(this);

        m_pStateMachine.SetCurrentState(start_state);
        m_pStateMachine.SetPreviousState(start_state);
        m_pStateMachine.SetGlobalState(GlobalKeeperState.Instance());

        m_pStateMachine.CurrentState().Enter(this);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        m_pStateMachine = null;
    }

    //these must be implemented
    public void Update() {
        //run the logic for the current state
        m_pStateMachine.Update();

        //calculate the combined force from each steering behavior 
        Vector2D SteeringForce = m_pSteering.Calculate();

        //Acceleration = Force/Mass
        Vector2D Acceleration = div(SteeringForce, m_dMass);
        //update velocity
        m_vVelocity.add(Acceleration);

        //make sure player does not exceed maximum velocity
        m_vVelocity.Truncate(m_dMaxSpeed);

        //update the position
        m_vPosition.add(m_vVelocity);


        //enforce a non-penetration constraint if desired
        if (Prm.bNonPenetrationConstraint) {
            EnforceNonPenetrationContraint(this, new AutoList<PlayerBase>().GetAllMembers());
        }

        //update the heading if the player has a non zero velocity
        if (!m_vVelocity.isZero()) {
            m_vHeading = Vec2DNormalize(m_vVelocity);
            m_vSide = m_vHeading.Perp();
        }

        //look-at vector always points toward the ball
        if (!Pitch().GoalKeeperHasBall()) {
            m_vLookAt = Vec2DNormalize(sub(Ball().Pos(), Pos()));
        }
    }

//--------------------------- Render -------------------------------------
//
//------------------------------------------------------------------------
    @Override
    public void Render() {
        if (Team().Color() == SoccerTeam.blue) {
            gdi.BluePen();
        } else {
            gdi.RedPen();
        }

        m_vecPlayerVBTrans = WorldTransform(m_vecPlayerVB,
                Pos(),
                m_vLookAt,
                m_vLookAt.Perp(),
                Scale());

        gdi.ClosedShape(m_vecPlayerVBTrans);

        //draw the head
        gdi.BrownBrush();
        gdi.Circle(Pos(), 6);

        //draw the ID
        if (Prm.bIDs) {
            gdi.TextColor(0, 170, 0);;
            gdi.TextAtPos(Pos().x - 20, Pos().y - 25, ttos(ID()));
        }

        //draw the state
        if (Prm.bStates) {
            gdi.TextColor(0, 170, 0);
            gdi.TransparentText();
            gdi.TextAtPos(m_vPosition.x, m_vPosition.y - 25,
                    new String(m_pStateMachine.GetNameOfCurrentState()));
        }
    }

    /**
     * routes any messages appropriately
     */
    @Override
    public boolean HandleMessage(final Telegram msg) {
        return m_pStateMachine.HandleMessage(msg);
    }

    /**
     * @return true if the ball comes close enough for the keeper to 
     *         consider intercepting
     */
    public boolean BallWithinRangeForIntercept() {
        return (Vec2DDistanceSq(Team().HomeGoal().Center(), Ball().Pos())
                <= Prm.GoalKeeperInterceptRangeSq);
    }

    /**
     * @return true if the keeper has ventured too far away from the goalmouth
     */
    public boolean TooFarFromGoalMouth() {
        return (Vec2DDistanceSq(Pos(), GetRearInterposeTarget())
                > Prm.GoalKeeperInterceptRangeSq);
    }

    /**
     * this method is called by the Intercept state to determine the spot
     * along the goalmouth which will act as one of the interpose targets
     * (the other is the ball).
     * the specific point at the goal line that the keeper is trying to cover
     * is flexible and can move depending on where the ball is on the field.
     * To achieve this we just scale the ball's y value by the ratio of the
     * goal width to playingfield width
     */
    public Vector2D GetRearInterposeTarget() {
        double xPosTarget = Team().HomeGoal().Center().x;

        double yPosTarget = Pitch().PlayingArea().Center().y
                - Prm.GoalWidth * 0.5 + (Ball().Pos().y * Prm.GoalWidth)
                / Pitch().PlayingArea().Height();

        return new Vector2D(xPosTarget, yPosTarget);
    }

    public StateMachine<GoalKeeper> GetFSM() {
        return m_pStateMachine;
    }

    public Vector2D LookAt() {
        return new Vector2D(m_vLookAt);
    }

    public void SetLookAt(Vector2D v) {
        m_vLookAt = new Vector2D(v);
    }
}