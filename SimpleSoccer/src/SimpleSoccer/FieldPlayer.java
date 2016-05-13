/**
 *   Desc:   Derived from a PlayerBase, this class encapsulates a player
 *           capable of moving around a soccer pitch, kicking, dribbling,
 *           shooting etc
 * 
 * @author Petr (http://www.sallyx.org/)
 */
package SimpleSoccer;

import SimpleSoccer.FieldPlayerStates.GlobalPlayerState;
import common.misc.Cgdi;
import common.Messaging.Telegram;
import common.misc.AutoList;
import common.D2.Vector2D;
import static common.D2.Vector2D.*;
import static common.D2.Transformation.*;
import common.FSM.State;
import common.FSM.StateMachine;
import static common.Game.EntityFunctionTemplates.EnforceNonPenetrationContraint;
import common.Time.Regulator;
import static common.misc.Cgdi.gdi;
import static common.misc.utils.clamp;
import static common.misc.Stream_Utility_function.ttos;
import static SimpleSoccer.ParamLoader.Prm;

public class FieldPlayer extends PlayerBase {
    //an instance of the state machine class

    private StateMachine<FieldPlayer> m_pStateMachine;
    //limits the number of kicks a player may take per second
    private Regulator m_pKickLimiter;

//----------------------------- ctor -------------------------------------
//------------------------------------------------------------------------
    public FieldPlayer(SoccerTeam home_team,
            int home_region,
            State<FieldPlayer> start_state,
            Vector2D heading,
            Vector2D velocity,
            double mass,
            double max_force,
            double max_speed,
            double max_turn_rate,
            double scale,
            player_role role) {
        super(home_team,
                home_region,
                heading,
                velocity,
                mass,
                max_force,
                max_speed,
                max_turn_rate,
                scale,
                role);

        //set up the state machine
        m_pStateMachine = new StateMachine<FieldPlayer>(this);

        if (start_state != null) {
            m_pStateMachine.SetCurrentState(start_state);
            m_pStateMachine.SetPreviousState(start_state);
            m_pStateMachine.SetGlobalState(GlobalPlayerState.Instance());

            m_pStateMachine.CurrentState().Enter(this);
        }

        m_pSteering.SeparationOn();

        //set up the kick regulator
        m_pKickLimiter = new Regulator(Prm.PlayerKickFrequency);
    }

    //------------------------------- dtor ---------------------------------------
//----------------------------------------------------------------------------
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        m_pKickLimiter = null;
        m_pStateMachine = null;
    }

    /**
     * call this to update the player's position and orientation
     */
    public void Update() {
        //run the logic for the current state
        m_pStateMachine.Update();

        //calculate the combined steering force
        m_pSteering.Calculate();

        //if no steering force is produced decelerate the player by applying a
        //braking force
        if (m_pSteering.Force().isZero()) {
            final double BrakingRate = 0.8;

            m_vVelocity.mul(BrakingRate);
        }

        //the steering force's side component is a force that rotates the 
        //player about its axis. We must limit the rotation so that a player
        //can only turn by PlayerMaxTurnRate rads per update.
        double TurningForce = m_pSteering.SideComponent();

        TurningForce = clamp(TurningForce, -Prm.PlayerMaxTurnRate, Prm.PlayerMaxTurnRate);

        //rotate the heading vector
        Vec2DRotateAroundOrigin(m_vHeading, TurningForce);

        //make sure the velocity vector points in the same direction as
        //the heading vector
        m_vVelocity = mul(m_vHeading, m_vVelocity.Length());
        
        //and recreate m_vSide
        m_vSide = m_vHeading.Perp();


        //now to calculate the acceleration due to the force exerted by
        //the forward component of the steering force in the direction
        //of the player's heading
        Vector2D accel = mul(m_vHeading, m_pSteering.ForwardComponent()/ m_dMass);

        m_vVelocity.add(accel);

        //make sure player does not exceed maximum velocity
        m_vVelocity.Truncate(m_dMaxSpeed);
        //update the position
        m_vPosition.add(m_vVelocity);

        //enforce a non-penetration constraint if desired
        if (Prm.bNonPenetrationConstraint) {
            EnforceNonPenetrationContraint(this, new AutoList<PlayerBase>().GetAllMembers());
        }
    }

//--------------------------- Render -------------------------------------
//
//------------------------------------------------------------------------
    @Override
    public void Render() {
        gdi.TransparentText();
        gdi.TextColor(Cgdi.grey);

        //set appropriate team color
        if (Team().Color() == SoccerTeam.blue) {
            gdi.BluePen();
        } else {
            gdi.RedPen();
        }

        //render the player's body
        m_vecPlayerVBTrans = WorldTransform(m_vecPlayerVB,
                Pos(),
                Heading(),
                Side(),
                Scale());
        gdi.ClosedShape(m_vecPlayerVBTrans);

        //and 'is 'ead
        gdi.BrownBrush();
        if (Prm.bHighlightIfThreatened && (Team().ControllingPlayer() == this) && isThreatened()) {
            gdi.YellowBrush();
        }
        gdi.Circle(Pos(), 6);


        //render the state
        if (Prm.bStates) {
            gdi.TextColor(0, 170, 0);
            gdi.TextAtPos(m_vPosition.x, m_vPosition.y - 25,
                    new String(m_pStateMachine.GetNameOfCurrentState()));
        }

        //show IDs
        if (Prm.bIDs) {
            gdi.TextColor(0, 170, 0);
            gdi.TextAtPos(Pos().x - 20, Pos().y - 25, ttos(ID()));
        }


        if (Prm.bViewTargets) {
            gdi.RedBrush();
            gdi.Circle(Steering().Target(), 3);
            gdi.TextAtPos(Steering().Target(), ttos(ID()));
        }
    }

    /**
     * routes any messages appropriately
     */
    @Override
    public boolean HandleMessage(final Telegram msg) {
        return m_pStateMachine.HandleMessage(msg);
    }

    public StateMachine<FieldPlayer> GetFSM() {
        return m_pStateMachine;
    }

    public boolean isReadyForNextKick() {
        return m_pKickLimiter.isReady();
    }
}
