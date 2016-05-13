/**
 * Desc: State prototypes for soccer team states
 * 
 * @author Petr (http://www.sallyx.org/)
 */
package SimpleSoccer.TeamStates;

import SimpleSoccer.SoccerTeam;

public class TeamStates {

    static {
        //uncomment to send state info to debug window
        //define(DEBUG_TEAM_STATES);
    }

    public static void ChangePlayerHomeRegions(SoccerTeam team, final int NewRegions[]) {
        for (int plyr = 0; plyr < NewRegions.length; ++plyr) {
            team.SetPlayerHomeRegion(plyr, NewRegions[plyr]);
        }
    }
}