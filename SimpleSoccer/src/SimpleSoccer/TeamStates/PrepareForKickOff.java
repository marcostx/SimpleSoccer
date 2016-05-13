/**
 * @author Petr (http://www.sallyx.org/)
 */
package SimpleSoccer.TeamStates;

import SimpleSoccer.SoccerTeam;
import common.FSM.State;
import common.Messaging.Telegram;

public class PrepareForKickOff extends State<SoccerTeam> {

    private static PrepareForKickOff instance = new PrepareForKickOff();

    private PrepareForKickOff() {
    }

    //this is a singleton
    public static PrepareForKickOff Instance() {
        return instance;
    }

    @Override
    public void Enter(SoccerTeam team) {
        //reset key player pointers
        team.SetControllingPlayer(null);
        team.SetSupportingPlayer(null);
        team.SetReceiver(null);
        team.SetPlayerClosestToBall(null);

        //send Msg_GoHome to each player.
        team.ReturnAllFieldPlayersToHome();
    }

    @Override
    public void Execute(SoccerTeam team) {
        //if both teams in position, start the game
        if (team.AllPlayersAtHome() && team.Opponents().AllPlayersAtHome()) {
            team.GetFSM().ChangeState(Defending.Instance());
        }
    }

    @Override
    public void Exit(SoccerTeam team) {
        team.Pitch().SetGameOn();
    }

    @Override
    public boolean OnMessage(SoccerTeam e, final Telegram t) {
        return false;
    }
}