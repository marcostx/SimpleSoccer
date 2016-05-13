/**
 * 
//------------------------- ReturnHome: ----------------------------------
//
//  In this state the goalkeeper simply returns back to the center of
//  the goal region before changing state back to TendGoal
//------------------------------------------------------------------------
 * 
 * @author Petr (http://www.sallyx.org/)
 */
package SimpleSoccer.GoalKeeperStates;

import SimpleSoccer.GoalKeeper;
import common.FSM.State;
import common.Messaging.Telegram;

public class ReturnHome extends State<GoalKeeper> {

    private static ReturnHome instance = new ReturnHome();

    private ReturnHome() {
    }

    //this is a singleton
    public static ReturnHome Instance() {
        return instance;
    }

    @Override
    public void Enter(GoalKeeper keeper) {
        keeper.Steering().ArriveOn();
    }

    @Override
    public void Execute(GoalKeeper keeper) {
        keeper.Steering().SetTarget(keeper.HomeRegion().Center());

        //if close enough to home or the opponents get control over the ball,
        //change state to tend goal
        if (keeper.InHomeRegion() || !keeper.Team().InControl()) {
            keeper.GetFSM().ChangeState(TendGoal.Instance());
        }
    }

    @Override
    public void Exit(GoalKeeper keeper) {
        keeper.Steering().ArriveOff();
    }

    @Override
    public boolean OnMessage(GoalKeeper e, final Telegram t) {
        return false;
    }
}