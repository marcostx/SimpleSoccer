/**
 * @author Petr (http://www.sallyx.org/)
 */
package SimpleSoccer.TeamStates;

import SimpleSoccer.SoccerTeam;
import static SimpleSoccer.DEFINE.*;
import static SimpleSoccer.TeamStates.TeamStates.ChangePlayerHomeRegions;
import static common.Debug.DbgConsole.*;
import common.FSM.State;
import common.Messaging.Telegram;

public class Defending extends State<SoccerTeam> {

    private static Defending instance = new Defending();

    private Defending() {
    }

    //this is a singleton
    public static Defending Instance() {
        return instance;
    }

    @Override
    public void Enter(SoccerTeam team) {
        if (def(DEBUG_TEAM_STATES)) {
            debug_con.print(team.Name()).print(" entering Defending state").print("");
        }

        //these define the home regions for this state of each of the players
        final int BlueRegions[] = {1, 6, 8, 3, 5};
        final int RedRegions[] = {16, 9, 11, 12, 14};

        //set up the player's home regions
        if (team.Color() == SoccerTeam.blue) {
            ChangePlayerHomeRegions(team, BlueRegions);
        } else {
            ChangePlayerHomeRegions(team, RedRegions);
        }

        //if a player is in either the Wait or ReturnToHomeRegion states, its
        //steering target must be updated to that of its new home region
        team.UpdateTargetsOfWaitingPlayers();
    }

    @Override
    public void Execute(SoccerTeam team) {
        //if in control change states
        if (team.InControl()) {
            team.GetFSM().ChangeState(Attacking.Instance());
            return;
        }
    }

    @Override
    public void Exit(SoccerTeam team) {
    }

    @Override
    public boolean OnMessage(SoccerTeam e, final Telegram t) {
        return false;
    }
}
