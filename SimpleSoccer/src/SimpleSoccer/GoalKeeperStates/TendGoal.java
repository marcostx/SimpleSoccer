/**
 *
 *  This is the main state for the goalkeeper. When in this state he will
 *  move left to right across the goalmouth using the 'interpose' steering
 *  behavior to put himself between the ball and the back of the net.
 *
 *  If the ball comes within the 'goalkeeper range' he moves out of the
 *  goalmouth to attempt to intercept it. (see next state)
 *
 * @author Petr (http://www.sallyx.org/)
 */
package SimpleSoccer.GoalKeeperStates;

import SimpleSoccer.GoalKeeper;
import static SimpleSoccer.ParamLoader.Prm;
import common.FSM.State;
import common.Messaging.Telegram;

public class TendGoal extends State<GoalKeeper> {

    private static TendGoal instance = new TendGoal();

    private TendGoal() {
    }

    //this is a singleton
    public static TendGoal Instance() {
        return instance;
    }

    public void Enter(GoalKeeper keeper) {
        //turn interpose on
        keeper.Steering().InterposeOn(Prm.GoalKeeperTendingDistance);

        //interpose will position the agent between the ball position and a target
        //position situated along the goal mouth. This call sets the target
        keeper.Steering().SetTarget(keeper.GetRearInterposeTarget());
    }

    @Override
    public void Execute(GoalKeeper keeper) {
        //the rear interpose target will change as the ball's position changes
        //so it must be updated each update-step 
        keeper.Steering().SetTarget(keeper.GetRearInterposeTarget());

        //if the ball comes in range the keeper traps it and then changes state
        //to put the ball back in play
        if (keeper.BallWithinKeeperRange()) {
            keeper.Ball().Trap();

            keeper.Pitch().SetGoalKeeperHasBall(true);

            keeper.GetFSM().ChangeState(PutBallBackInPlay.Instance());

            return;
        }

        //if ball is within a predefined distance, the keeper moves out from
        //position to try and intercept it.
        if (keeper.BallWithinRangeForIntercept() && !keeper.Team().InControl()) {
            keeper.GetFSM().ChangeState(InterceptBall.Instance());
        }

        //if the keeper has ventured too far away from the goal-line and there
        //is no threat from the opponents he should move back towards it
        if (keeper.TooFarFromGoalMouth() && keeper.Team().InControl()) {
            keeper.GetFSM().ChangeState(ReturnHome.Instance());
            return;
        }
    }

    @Override
    public void Exit(GoalKeeper keeper) {
        keeper.Steering().InterposeOff();
    }

    public boolean OnMessage(GoalKeeper e, final Telegram t) {
        return false;
    }
}
