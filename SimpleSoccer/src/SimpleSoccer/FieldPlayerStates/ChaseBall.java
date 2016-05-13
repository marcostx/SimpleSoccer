/**
 * @author Petr (http://www.sallyx.org/)
 */
package SimpleSoccer.FieldPlayerStates;

import static SimpleSoccer.DEFINE.*;
import SimpleSoccer.FieldPlayer;
import static common.Debug.DbgConsole.*;
import common.FSM.State;
import common.Messaging.Telegram;

public class ChaseBall extends State<FieldPlayer> {

    private static ChaseBall instance = new ChaseBall();

    private ChaseBall() {
    }

    //this is a singleton
    public static ChaseBall Instance() {
        return instance;
    }

    @Override
    public void Enter(FieldPlayer player) {
        player.Steering().SeekOn();

        if (def(PLAYER_STATE_INFO_ON)) {
            debug_con.print("Player ").print(player.ID()).print(" enters chase state").print("");
        }
    }

    @Override
    public void Execute(FieldPlayer player) {
        //if the ball is within kicking range the player changes state to KickBall.
        if (player.BallWithinKickingRange()) {
            player.GetFSM().ChangeState(KickBall.Instance());
            return;
        }

        //if the player is the closest player to the ball then he should keep
        //chasing it
        if (player.isClosestTeamMemberToBall()) {
            player.Steering().SetTarget(player.Ball().Pos());

            return;
        }

        //if the player is not closest to the ball anymore, he should return back
        //to his home region and wait for another opportunity
        player.GetFSM().ChangeState(ReturnToHomeRegion.Instance());
    }

    @Override
    public void Exit(FieldPlayer player) {
        player.Steering().SeekOff();
    }

    @Override
    public boolean OnMessage(FieldPlayer e, final Telegram t) {
        return false;
    }
}
