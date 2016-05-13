/**
 * @author Petr (http://www.sallyx.org/)
 */
package SimpleSoccer.FieldPlayerStates;

import static SimpleSoccer.DEFINE.*;
import SimpleSoccer.FieldPlayer;
import static common.Debug.DbgConsole.*;
import common.FSM.State;
import common.Game.Region;
import common.Messaging.Telegram;

public class ReturnToHomeRegion extends State<FieldPlayer> {

    private static ReturnToHomeRegion instance = new ReturnToHomeRegion();

    private ReturnToHomeRegion() {
    }

    //this is a singleton
    public static ReturnToHomeRegion Instance() {
        return instance;
    }

    @Override
    public void Enter(FieldPlayer player) {
        player.Steering().ArriveOn();

        if (!player.HomeRegion().Inside(player.Steering().Target(), Region.halfsize)) {
            player.Steering().SetTarget(player.HomeRegion().Center());
        }

        if (def(PLAYER_STATE_INFO_ON)) {
            debug_con.print("Player ").print(player.ID()).print(" enters ReturnToHome state").print("");
        }
    }

    @Override
    public void Execute(FieldPlayer player) {
        if (player.Pitch().GameOn()) {
            //if the ball is nearer this player than any other team member  &&
            //there is not an assigned receiver && the goalkeeper does not gave
            //the ball, go chase it
            if (player.isClosestTeamMemberToBall()
                    && (player.Team().Receiver() == null)
                    && !player.Pitch().GoalKeeperHasBall()) {
                player.GetFSM().ChangeState(ChaseBall.Instance());

                return;
            }
        }

        //if game is on and close enough to home, change state to wait and set the 
        //player target to his current position.(so that if he gets jostled out of 
        //position he can move back to it)
        if (player.Pitch().GameOn() && player.HomeRegion().Inside(player.Pos(),
                Region.halfsize)) {
            player.Steering().SetTarget(player.Pos());
            player.GetFSM().ChangeState(Wait.Instance());
        } //if game is not on the player must return much closer to the center of his
        //home region
        else if (!player.Pitch().GameOn() && player.AtTarget()) {
            player.GetFSM().ChangeState(Wait.Instance());
        }
    }

    @Override
    public void Exit(FieldPlayer player) {
        player.Steering().ArriveOff();
    }

    @Override
    public boolean OnMessage(FieldPlayer e, final Telegram t) {
        return false;
    }
}
