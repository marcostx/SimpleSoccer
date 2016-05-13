/**
 * @author Petr (http://www.sallyx.org/)
 */
package SimpleSoccer.GoalKeeperStates;

import SimpleSoccer.GoalKeeper;
import static SimpleSoccer.ParamLoader.Prm;
import static SimpleSoccer.MessageTypes.*;
import SimpleSoccer.PlayerBase;
import common.D2.Vector2D;
import static common.D2.Vector2D.*;
import common.FSM.State;
import common.Messaging.Telegram;
import static common.Messaging.MessageDispatcher.*;
import common.misc.CppToJava.ObjectRef;

public class PutBallBackInPlay extends State<GoalKeeper> {

    private static PutBallBackInPlay instance = new PutBallBackInPlay();

    private PutBallBackInPlay() {
    }

    //this is a singleton
    public static PutBallBackInPlay Instance() {
        return instance;
    }

    @Override
    public void Enter(GoalKeeper keeper) {
        //let the team know that the keeper is in control
        keeper.Team().SetControllingPlayer(keeper);

        //send all the players home
        keeper.Team().Opponents().ReturnAllFieldPlayersToHome();
        keeper.Team().ReturnAllFieldPlayersToHome();
    }

    @Override
    public void Execute(GoalKeeper keeper) {
        PlayerBase receiver = null;
        Vector2D BallTarget = new Vector2D();

        ObjectRef<PlayerBase> receiverRef = new ObjectRef<PlayerBase>(receiver);

        //test if there are players further forward on the field we might
        //be able to pass to. If so, make a pass.
        if (keeper.Team().FindPass(keeper,
                receiverRef,
                BallTarget,
                Prm.MaxPassingForce,
                Prm.GoalkeeperMinPassDist)) {
            receiver = receiverRef.get();
            //make the pass   
            keeper.Ball().Kick(Vec2DNormalize(sub(BallTarget, keeper.Ball().Pos())),
                    Prm.MaxPassingForce);

            //goalkeeper no longer has ball 
            keeper.Pitch().SetGoalKeeperHasBall(false);

            //let the receiving player know the ball's comin' at him
            Dispatcher.DispatchMsg(SEND_MSG_IMMEDIATELY,
                    keeper.ID(),
                    receiver.ID(),
                    Msg_ReceiveBall,
                    BallTarget);

            //go back to tending the goal   
            keeper.GetFSM().ChangeState(TendGoal.Instance());

            return;
        }

        keeper.SetVelocity(new Vector2D());
    }

    @Override
    public void Exit(GoalKeeper keeper) {
    }

    @Override
    public boolean OnMessage(GoalKeeper e, final Telegram t) {
        return false;
    }
}
