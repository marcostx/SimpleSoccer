/**
 * @author Petr (http://www.sallyx.org/)
 */
package SimpleSoccer.GoalKeeperStates;

import SimpleSoccer.GoalKeeper;
import common.FSM.State;
import common.Messaging.Telegram;

public class GlobalKeeperState extends State<GoalKeeper> {

    private static GlobalKeeperState instance = new GlobalKeeperState();

    private GlobalKeeperState() {
    }

    public static GlobalKeeperState Instance() {
        return instance;
    }

    @Override
    public void Enter(GoalKeeper keeper) {
    }

    @Override
    public void Execute(GoalKeeper keeper) {
    }

    @Override
    public void Exit(GoalKeeper keeper) {
    }

    @Override
    public boolean OnMessage(GoalKeeper keeper, final Telegram telegram) {
        switch (telegram.Msg) {
            case Msg_GoHome: {
                keeper.SetDefaultHomeRegion();
                keeper.GetFSM().ChangeState(ReturnHome.Instance());
            }

            break;

            case Msg_ReceiveBall: {
                keeper.GetFSM().ChangeState(InterceptBall.Instance());
            }

            break;

        }//end switch

        return false;
    }
}