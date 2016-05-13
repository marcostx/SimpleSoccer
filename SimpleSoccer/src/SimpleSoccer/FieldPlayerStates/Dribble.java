/**
 * @author Petr (http://www.sallyx.org/)
 */
package SimpleSoccer.FieldPlayerStates;

import common.Messaging.Telegram;
import static SimpleSoccer.DEFINE.*;
import SimpleSoccer.FieldPlayer;
import static SimpleSoccer.ParamLoader.Prm;
import common.D2.Vector2D;
import common.D2.Vector2D.*;
import static common.D2.Transformation.Vec2DRotateAroundOrigin;
import static common.Debug.DbgConsole.*;
import common.FSM.State;
import static common.misc.utils.QuarterPi;

public class Dribble extends State<FieldPlayer> {

    private static Dribble instance = new Dribble();

    private Dribble() {
    }

    //this is a singleton
    public static Dribble Instance() {
        return instance;
    }

    @Override
    public void Enter(FieldPlayer player) {
        //let the team know this player is controlling
        player.Team().SetControllingPlayer(player);

        if (def(PLAYER_STATE_INFO_ON)) {
            debug_con.print("Player ").print(player.ID()).print(" enters dribble state").print("");
        }
    }

    @Override
    public void Execute(FieldPlayer player) {
        double dot = player.Team().HomeGoal().Facing().Dot(player.Heading());

        //if the ball is between the player and the home goal, it needs to swivel
        // the ball around by doing multiple small kicks and turns until the player 
        //is facing in the correct direction
        if (dot < 0) {
            //the player's heading is going to be rotated by a small amount (Pi/4) 
            //and then the ball will be kicked in that direction
            Vector2D direction = player.Heading();

            //calculate the sign (+/-) of the angle between the player heading and the 
            //facing direction of the goal so that the player rotates around in the 
            //correct direction
            double angle = QuarterPi * -1
                    * player.Team().HomeGoal().Facing().Sign(player.Heading());

            Vec2DRotateAroundOrigin(direction, angle);

            //this value works well whjen the player is attempting to control the
            //ball and turn at the same time
            final double KickingForce = 0.8;

            player.Ball().Kick(direction, KickingForce);
        } //kick the ball down the field
        else {
            player.Ball().Kick(player.Team().HomeGoal().Facing(),
                    Prm.MaxDribbleForce);
        }

        //the player has kicked the ball so he must now change state to follow it
        player.GetFSM().ChangeState(ChaseBall.Instance());

        return;
    }

    @Override
    public void Exit(FieldPlayer player) {
    }

    @Override
    public boolean OnMessage(FieldPlayer e, final Telegram t) {
        return false;
    }
}
