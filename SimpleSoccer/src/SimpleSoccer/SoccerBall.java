/**
 *  Desc: Class to implement a soccer ball. This class inherits from
 *        MovingEntity and provides further functionality for collision
 *        testing and position prediction.
 * 
 * @author Petr (http://www.sallyx.org/)
 */
package SimpleSoccer;

import common.D2.Wall2D;
import java.util.List;
import static java.lang.Math.sqrt;
import common.D2.Vector2D;
import static SimpleSoccer.ParamLoader.Prm;
import static common.D2.geometry.*;
import static common.D2.geometry.span_type.*;
import static common.D2.Transformation.Vec2DRotateAroundOrigin;
import static common.D2.Vector2D.*;
import common.Messaging.Telegram;
import static common.misc.utils.*;
import static common.misc.Cgdi.gdi;

public class SoccerBall extends MovingEntity {

    //keeps a record of the ball's position at the last update
    private Vector2D m_vOldPos;
    //a local reference to the Walls that make up the pitch boundary
    private final List<Wall2D> m_PitchBoundary;

    public SoccerBall(Vector2D pos,
            double BallSize,
            double mass,
            List<Wall2D> PitchBoundary) {

        //set up the base class
        super(pos,
                BallSize,
                new Vector2D(0, 0),
                -1.0, //max speed - unused
                new Vector2D(0, 1),
                mass,
                new Vector2D(1.0, 1.0), //scale     - unused
                0, //turn rate - unused
                0);                  //max force - unused
        m_PitchBoundary = PitchBoundary;
    }

    /**
     * tests to see if the ball has collided with a ball and reflects 
     * the ball's velocity accordingly
     */
    void TestCollisionWithWalls(final List<Wall2D> walls) {
        //test ball against each wall, find out which is closest
        int idxClosest = -1;

        Vector2D VelNormal = Vec2DNormalize(m_vVelocity);

        Vector2D IntersectionPoint,
                CollisionPoint = new Vector2D();

        double DistToIntersection = MaxFloat;

        /**
         * iterate through each wall and calculate if the ball intersects.
         * If it does then store the index into the closest intersecting wall
         */
        for (int w = 0; w < walls.size(); ++w) {
            //assuming a collision if the ball continued on its current heading 
            //calculate the point on the ball that would hit the wall. This is 
            //simply the wall's normal(inversed) multiplied by the ball's radius
            //and added to the balls center (its position)
            Vector2D ThisCollisionPoint = sub(Pos(), (mul(walls.get(w).Normal(), BRadius())));

            //calculate exactly where the collision point will hit the plane    
            if (WhereIsPoint(ThisCollisionPoint,
                    walls.get(w).From(),
                    walls.get(w).Normal()) == plane_backside) {
                double DistToWall = DistanceToRayPlaneIntersection(ThisCollisionPoint,
                        walls.get(w).Normal(),
                        walls.get(w).From(),
                        walls.get(w).Normal());

                IntersectionPoint = add(ThisCollisionPoint, (mul(DistToWall, walls.get(w).Normal())));

            } else {
                double DistToWall = DistanceToRayPlaneIntersection(ThisCollisionPoint,
                        VelNormal,
                        walls.get(w).From(),
                        walls.get(w).Normal());

                IntersectionPoint = add(ThisCollisionPoint, (mul(DistToWall, VelNormal)));
            }

            //check to make sure the intersection point is actually on the line
            //segment
            boolean OnLineSegment = false;

            if (LineIntersection2D(walls.get(w).From(),
                    walls.get(w).To(),
                    sub(ThisCollisionPoint, mul(walls.get(w).Normal(), 20.0)),
                    add(ThisCollisionPoint, mul(walls.get(w).Normal(), 20.0)))) {

                OnLineSegment = true;
            }


            //Note, there is no test for collision with the end of a line segment

            //now check to see if the collision point is within range of the
            //velocity vector. [work in distance squared to avoid sqrt] and if it
            //is the closest hit found so far. 
            //If it is that means the ball will collide with the wall sometime
            //between this time step and the next one.
            double distSq = Vec2DDistanceSq(ThisCollisionPoint, IntersectionPoint);

            if ((distSq <= m_vVelocity.LengthSq()) && (distSq < DistToIntersection) && OnLineSegment) {
                DistToIntersection = distSq;
                idxClosest = w;
                CollisionPoint = IntersectionPoint;
            }
        }//next wall


        //to prevent having to calculate the exact time of collision we
        //can just check if the velocity is opposite to the wall normal
        //before reflecting it. This prevents the case where there is overshoot
        //and the ball gets reflected back over the line before it has completely
        //reentered the playing area.
        if ((idxClosest >= 0) && VelNormal.Dot(walls.get(idxClosest).Normal()) < 0) {
            m_vVelocity.Reflect(walls.get(idxClosest).Normal());
        }
    }

    /**
     * updates the ball physics, tests for any collisions and adjusts
     * the ball's velocity accordingly
     */
    @Override
    public void Update() {
        //keep a record of the old position so the goal::scored method
        //can utilize it for goal testing
        m_vOldPos = new Vector2D(m_vPosition);

        //Test for collisions
        TestCollisionWithWalls(m_PitchBoundary);

        //Simulate Prm.Friction. Make sure the speed is positive 
        //first though
        if (m_vVelocity.LengthSq() > Prm.Friction * Prm.Friction) {
            m_vVelocity.add(mul(Vec2DNormalize(m_vVelocity), Prm.Friction));
            m_vPosition.add(m_vVelocity);


            //update heading
            m_vHeading = Vec2DNormalize(m_vVelocity);
        }
    }

    /**
     * Renders the ball
     */
    @Override
    public void Render() {
        gdi.BlackBrush();

        gdi.Circle(m_vPosition, m_dBoundingRadius);

        /*
        gdi.GreenBrush();
        for (int i=0; i<IPPoints.size(); ++i)
        {
        gdi.Circle(IPPoints[i], 3);
        }
         */
    }

    //a soccer ball doesn't need to handle messages
    @Override
    public boolean HandleMessage(final Telegram msg) {
        return false;
    }

    /**
     * applys a force to the ball in the direction of heading. Truncates
     * the new velocity to make sure it doesn't exceed the max allowable.
     */
    public void Kick(Vector2D direction, double force) {
        //ensure direction is normalized
        direction.Normalize();

        //calculate the acceleration
        Vector2D acceleration = div(mul(direction, force), m_dMass);

        //update the velocity
        m_vVelocity = acceleration;
    }

    /**
     * Given a force and a distance to cover given by two vectors, this
     * method calculates how long it will take the ball to travel between
     * the two points
     */
    public double TimeToCoverDistance(Vector2D A,
            Vector2D B,
            double force) {
        //this will be the velocity of the ball in the next time step *if*
        //the player was to make the pass. 
        double speed = force / m_dMass;

        //calculate the velocity at B using the equation
        //
        //  v^2 = u^2 + 2as
        //

        //first calculate s (the distance between the two positions)
        double DistanceToCover = Vec2DDistance(A, B);

        double term = speed * speed + 2.0 * DistanceToCover * Prm.Friction;

        //if  (u^2 + 2as) is negative it means the ball cannot reach point B.
        if (term <= 0.0) {
            return -1.0;
        }

        double v = sqrt(term);

        //it IS possible for the ball to reach B and we know its speed when it
        //gets there, so now it's easy to calculate the time using the equation
        //
        //    t = v-u
        //        ---
        //         a
        //
        return (v - speed) / Prm.Friction;
    }

    /**
     * given a time this method returns the ball position at that time in the
     *  future
     */
    public Vector2D FuturePosition(double time) {
        //using the equation s = ut + 1/2at^2, where s = distance, a = friction
        //u=start velocity

        //calculate the ut term, which is a vector
        Vector2D ut = mul(m_vVelocity, time);

        //calculate the 1/2at^2 term, which is scalar
        double half_a_t_squared = 0.5 * Prm.Friction * time * time;

        //turn the scalar quantity into a vector by multiplying the value with
        //the normalized velocity vector (because that gives the direction)
        Vector2D ScalarToVector = mul(half_a_t_squared, Vec2DNormalize(m_vVelocity));

        //the predicted position is the balls position plus these two terms
        return add(Pos(), ut).add(ScalarToVector);
    }

    /**
     * this is used by players and goalkeepers to 'trap' a ball -- to stop
     * it dead. That player is then assumed to be in possession of the ball
     * and m_pOwner is adjusted accordingly
     */
    public void Trap() {
        m_vVelocity.Zero();
    }

    public Vector2D OldPos() {
        return new Vector2D(m_vOldPos);
    }

    /**
     * positions the ball at the desired location and sets the ball's velocity to
     *  zero
     */
    public void PlaceAtPosition(Vector2D NewPos) {
        m_vPosition = new Vector2D(NewPos);

        m_vOldPos = new Vector2D(m_vPosition);

        m_vVelocity.Zero();
    }

    /**
     *  this can be used to vary the accuracy of a player's kick. Just call it 
     *  prior to kicking the ball using the ball's position and the ball target as
     *  parameters.
     */
    public static Vector2D AddNoiseToKick(Vector2D BallPos, Vector2D BallTarget) {

        double displacement = (Pi - Pi * Prm.PlayerKickingAccuracy) * RandomClamped();

        Vector2D toTarget = sub(BallTarget, BallPos);

        Vec2DRotateAroundOrigin(toTarget, displacement);

        return add(toTarget, BallPos);
    }
}
