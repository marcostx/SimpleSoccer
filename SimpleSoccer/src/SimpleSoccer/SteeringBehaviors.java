/**
 * 
 *  Desc:   class to encapsulate steering behaviors for a soccer player
 * 
 * @author Petr (http://www.sallyx.org/)
 */
package SimpleSoccer;

import java.util.ArrayList;
import java.util.ListIterator;
import common.misc.AutoList;
import java.util.List;
import common.D2.Vector2D;
import java.lang.reflect.Array;
import java.util.Arrays;
import static common.D2.Vector2D.*;
import static common.misc.Cgdi.gdi;
import static SimpleSoccer.ParamLoader.Prm;

public class SteeringBehaviors {

    private PlayerBase m_pPlayer;
    private SoccerBall m_pBall;
    //the steering force created by the combined effect of all
    //the selected behaviors
    private Vector2D m_vSteeringForce = new Vector2D();
    //the current target (usually the ball or predicted ball position)
    private Vector2D m_vTarget = new Vector2D();
    //the distance the player tries to interpose from the target
    private double m_dInterposeDist;
    //multipliers. 
    private double m_dMultSeparation;
    //how far it can 'see'
    private double m_dViewDistance;
    //binary flags to indicate whether or not a behavior should be active
    private int m_iFlags;

    private enum behavior_type {

        none(0x0000),
        seek(0x0001),
        arrive(0x0002),
        separation(0x0004),
        pursuit(0x0008),
        interpose(0x0010);
        private int flag;

        behavior_type(int flag) {
            this.flag = flag;
        }

        public int flag() {
            return this.flag;
        }
    }
    //used by group behaviors to tag neighbours
    private boolean m_bTagged;

    //Arrive makes use of these to determine how quickly a vehicle
    //should decelerate to its target
    private enum Deceleration {

        slow(3), normal(2), fast(1);
        private int dec;

        Deceleration(int d) {
            this.dec = d;
        }

        public int value() {
            return dec;
        }
    }

    /**
     * Given a target, this behavior returns a steering force which will
     * allign the agent with the target and move the agent in the desired
     * direction
     */
    private Vector2D Seek(Vector2D target) {

        Vector2D DesiredVelocity = Vec2DNormalize(mul(sub(target, m_pPlayer.Pos()),
                m_pPlayer.MaxSpeed()));

        return (sub(DesiredVelocity, m_pPlayer.Velocity()));
    }

    /**
     * This behavior is similar to seek but it attempts to arrive at the
     *  target with a zero velocity
     */
    private Vector2D Arrive(Vector2D TargetPos, Deceleration deceleration) {
        Vector2D ToTarget = sub(TargetPos, m_pPlayer.Pos());

        //calculate the distance to the target
        double dist = ToTarget.Length();

        if (dist > 0) {
            //because Deceleration is enumerated as an int, this value is required
            //to provide fine tweaking of the deceleration..
            final double DecelerationTweaker = 0.3;

            //calculate the speed required to reach the target given the desired
            //deceleration
            double speed = dist / ((double) deceleration.value() * DecelerationTweaker);

            //make sure the velocity does not exceed the max
            speed = Math.min(speed, m_pPlayer.MaxSpeed());

            //from here proceed just like Seek except we don't need to normalize 
            //the ToTarget vector because we have already gone to the trouble
            //of calculating its length: dist. 
            Vector2D DesiredVelocity = mul(ToTarget, speed / dist);

            return sub(DesiredVelocity, m_pPlayer.Velocity());
        }

        return new Vector2D(0, 0);
    }

    /**
     * This behavior predicts where its prey will be and seeks
     * to that location
     * This behavior creates a force that steers the agent towards the 
     * ball
     */
    private Vector2D Pursuit(final SoccerBall ball) {
        Vector2D ToBall = sub(ball.Pos(), m_pPlayer.Pos());

        //the lookahead time is proportional to the distance between the ball
        //and the pursuer; 
        double LookAheadTime = 0.0;

        if (ball.Speed() != 0.0) {
            LookAheadTime = ToBall.Length() / ball.Speed();
        }

        //calculate where the ball will be at this time in the future
        m_vTarget = ball.FuturePosition(LookAheadTime);

        //now seek to the predicted future position of the ball
        return Arrive(m_vTarget, Deceleration.fast);
    }

    /**
     *
     * this calculates a force repelling from the other neighbors
     */
    private Vector2D Separation() {
        //iterate through all the neighbors and calculate the vector from them
        Vector2D SteeringForce = new Vector2D();

        List<PlayerBase> AllPlayers = new AutoList<PlayerBase>().GetAllMembers();
        ListIterator<PlayerBase> it = AllPlayers.listIterator();
        while (it.hasNext()) {
            PlayerBase curPlyr = it.next();
            //make sure this agent isn't included in the calculations and that
            //the agent is close enough
            if ((curPlyr != m_pPlayer) && curPlyr.Steering().Tagged()) {
                Vector2D ToAgent = sub(m_pPlayer.Pos(), curPlyr.Pos());

                //scale the force inversely proportional to the agents distance  
                //from its neighbor.
                SteeringForce.add(div(Vec2DNormalize(ToAgent), ToAgent.Length()));
            }
        }

        return SteeringForce;
    }

    /**
     * Given an opponent and an object position this method returns a 
     * force that attempts to position the agent between them
     */
    private Vector2D Interpose(final SoccerBall ball,
            Vector2D target,
            double DistFromTarget) {
        return Arrive(add(target, mul(Vec2DNormalize(sub(ball.Pos(), target)),
                DistFromTarget)), Deceleration.normal);
    }

    /**
     *  tags any vehicles within a predefined radius
     */
    private void FindNeighbours() {
        List<PlayerBase> AllPlayers = new AutoList<PlayerBase>().GetAllMembers();
        ListIterator<PlayerBase> it = AllPlayers.listIterator();
        while (it.hasNext()) {
            PlayerBase curPlyr = it.next();

            //first clear any current tag
            curPlyr.Steering().UnTag();

            //work in distance squared to avoid sqrts
            Vector2D to = sub(curPlyr.Pos(), m_pPlayer.Pos());

            if (to.LengthSq() < (m_dViewDistance * m_dViewDistance)) {
                curPlyr.Steering().Tag();
            }
        }//next
    }

    /**
     * this function tests if a specific bit of m_iFlags is set
     */
    private boolean On(behavior_type bt) {
        return (m_iFlags & bt.flag()) == bt.flag();
    }

    /**
     *  This function calculates how much of its max steering force the 
     *  vehicle has left to apply and then applies that amount of the
     *  force to add.
     */
    private boolean AccumulateForce(Vector2D sf, Vector2D ForceToAdd) {
        //first calculate how much steering force we have left to use
        double MagnitudeSoFar = sf.Length();

        double magnitudeRemaining = m_pPlayer.MaxForce() - MagnitudeSoFar;

        //return false if there is no more force left to use
        if (magnitudeRemaining <= 0.0) {
            return false;
        }

        //calculate the magnitude of the force we want to add
        double MagnitudeToAdd = ForceToAdd.Length();

        //now calculate how much of the force we can really add  
        if (MagnitudeToAdd > magnitudeRemaining) {
            MagnitudeToAdd = magnitudeRemaining;
        }

        //add it to the steering force
        sf.add(mul(Vec2DNormalize(ForceToAdd), MagnitudeToAdd));

        return true;
    }

    /**
     * this method calls each active steering behavior and acumulates their
     *  forces until the max steering force magnitude is reached at which
     *  time the function returns the steering force accumulated to that 
     *  point
     */
    private Vector2D SumForces() {
        Vector2D force = new Vector2D();

        //the soccer players must always tag their neighbors
        FindNeighbours();

        if (On(behavior_type.separation)) {
            force.add(mul(Separation(), m_dMultSeparation));

            if (!AccumulateForce(m_vSteeringForce, force)) {
                return m_vSteeringForce;
            }
        }

        if (On(behavior_type.seek)) {
            force.add(Seek(m_vTarget));

            if (!AccumulateForce(m_vSteeringForce, force)) {
                return m_vSteeringForce;
            }
        }

        if (On(behavior_type.arrive)) {
            force.add(Arrive(m_vTarget, Deceleration.fast));

            if (!AccumulateForce(m_vSteeringForce, force)) {
                return m_vSteeringForce;
            }
        }

        if (On(behavior_type.pursuit)) {
            force.add(Pursuit(m_pBall));

            if (!AccumulateForce(m_vSteeringForce, force)) {
                return m_vSteeringForce;
            }
        }

        if (On(behavior_type.interpose)) {
            force.add(Interpose(m_pBall, m_vTarget, m_dInterposeDist));

            if (!AccumulateForce(m_vSteeringForce, force)) {
                return m_vSteeringForce;
            }
        }

        return m_vSteeringForce;
    }
    //a vertex buffer to contain the feelers rqd for dribbling
    private List<Vector2D> m_Antenna;

//------------------------- ctor -----------------------------------------
//
//------------------------------------------------------------------------
    public SteeringBehaviors(PlayerBase agent,
            SoccerPitch world,
            SoccerBall ball) {
        m_pPlayer = agent;
        m_iFlags = 0;
        m_dMultSeparation = Prm.SeparationCoefficient;
        m_bTagged = false;
        m_dViewDistance = Prm.ViewDistance;
        m_pBall = ball;
        m_dInterposeDist = 0.0;
        m_Antenna = Arrays.asList((Vector2D[]) Array.newInstance(Vector2D.class, 5));
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    /**
     * calculates the overall steering force based on the currently active
     * steering behaviors. 
     */
    public Vector2D Calculate() {
        //reset the force
        m_vSteeringForce.Zero();

        //this will hold the value of each individual steering force
        m_vSteeringForce = SumForces();

        //make sure the force doesn't exceed the vehicles maximum allowable
        m_vSteeringForce.Truncate(m_pPlayer.MaxForce());

        return new Vector2D(m_vSteeringForce);
    }

    /**
     * calculates the component of the steering force that is parallel
     * with the vehicle heading
     */
    public double ForwardComponent() {
        return m_pPlayer.Heading().Dot(m_vSteeringForce);
    }

    /**
     * calculates the component of the steering force that is perpendicuar
     * with the vehicle heading
     */
    public double SideComponent() {
        return m_pPlayer.Side().Dot(m_vSteeringForce) * m_pPlayer.MaxTurnRate();
    }

    public Vector2D Force() {
        return m_vSteeringForce;
    }

    /**
     * renders visual aids and info for seeing how each behavior is
     * calculated
     */
    public void RenderAids() {
        //render the steering force
        gdi.RedPen();
        gdi.Line(m_pPlayer.Pos(), add(m_pPlayer.Pos(), mul(m_vSteeringForce, 20)));
    }

    public Vector2D Target() {
        return new Vector2D(m_vTarget);
    }

    public void SetTarget(final Vector2D t) {
        m_vTarget = new Vector2D(t);
    }

    public double InterposeDistance() {
        return m_dInterposeDist;
    }

    public void SetInterposeDistance(double d) {
        m_dInterposeDist = d;
    }

    public boolean Tagged() {
        return m_bTagged;
    }

    public void Tag() {
        m_bTagged = true;
    }

    public void UnTag() {
        m_bTagged = false;
    }

    public void SeekOn() {
        m_iFlags |= behavior_type.seek.flag();
    }

    public void ArriveOn() {
        m_iFlags |= behavior_type.arrive.flag();
    }

    public void PursuitOn() {
        m_iFlags |= behavior_type.pursuit.flag();
    }

    public void SeparationOn() {
        m_iFlags |= behavior_type.separation.flag();
    }

    public void InterposeOn(double d) {
        m_iFlags |= behavior_type.interpose.flag();
        m_dInterposeDist = d;
    }

    public void SeekOff() {
        if (On(behavior_type.seek)) {
            m_iFlags ^= behavior_type.seek.flag();
        }
    }

    public void ArriveOff() {
        if (On(behavior_type.arrive)) {
            m_iFlags ^= behavior_type.arrive.flag();
        }
    }

    public void PursuitOff() {
        if (On(behavior_type.pursuit)) {
            m_iFlags ^= behavior_type.pursuit.flag();
        }
    }

    public void SeparationOff() {
        if (On(behavior_type.separation)) {
            m_iFlags ^= behavior_type.separation.flag();
        }
    }

    public void InterposeOff() {
        if (On(behavior_type.interpose)) {
            m_iFlags ^= behavior_type.interpose.flag();
        }
    }

    public boolean SeekIsOn() {
        return On(behavior_type.seek);
    }

    public boolean ArriveIsOn() {
        return On(behavior_type.arrive);
    }

    public boolean PursuitIsOn() {
        return On(behavior_type.pursuit);
    }

    public boolean SeparationIsOn() {
        return On(behavior_type.separation);
    }

    public boolean InterposeIsOn() {
        return On(behavior_type.interpose);
    }
}