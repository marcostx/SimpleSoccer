/**
 * @author Petr (http://www.sallyx.org/)
 */
package SimpleSoccer.FieldPlayerStates;

import common.misc.CppToJava.ObjectRef;
import common.Messaging.Telegram;
import SimpleSoccer.PlayerBase;
import common.D2.Vector2D;
import static common.D2.Vector2D.*;
import static SimpleSoccer.DEFINE.*;
import SimpleSoccer.FieldPlayer;
import static SimpleSoccer.MessageTypes.*;
import static SimpleSoccer.ParamLoader.Prm;
import static SimpleSoccer.SoccerBall.AddNoiseToKick;
import common.FSM.State;
import static common.Debug.DbgConsole.*;
import static common.Messaging.MessageDispatcher.*;
import static common.misc.Stream_Utility_function.ttos;
import static common.misc.utils.*;

public class KickBall extends State<FieldPlayer> {

    private static KickBall instance = new KickBall();

    private KickBall() {
    }

    //this is a singleton
    public static KickBall Instance() {
        return instance;
    }

    @Override
    public void Enter(FieldPlayer player) {
        //let the team know this player is controlling
        player.Team().SetControllingPlayer(player);

        //the player can only make so many kick attempts per second.
        if (!player.isReadyForNextKick()) {
            player.GetFSM().ChangeState(ChaseBall.Instance());
        }


        if (def(PLAYER_STATE_INFO_ON)) {
            debug_con.print("Player ").print(player.ID()).print(" enters kick state").print("");
        }
    }

    @Override
    public void Execute(FieldPlayer player) {
        //calculate the dot product of the vector pointing to the ball
        //and the player's heading
        Vector2D ToBall = sub(player.Ball().Pos(), player.Pos());
        double dot = player.Heading().Dot(Vec2DNormalize(ToBall));

        //cannot kick the ball if the goalkeeper is in possession or if it is 
        //behind the player or if there is already an assigned receiver. So just
        //continue chasing the ball
        if (player.Team().Receiver() != null
                || player.Pitch().GoalKeeperHasBall()
                || (dot < 0)) {
            if (def(PLAYER_STATE_INFO_ON)) {
                debug_con.print("Goaly has ball / ball behind player").print("");
            }

            player.GetFSM().ChangeState(ChaseBall.Instance());

            return;
        }

        /* Attempt a shot at the goal */

        //if a shot is possible, this vector will hold the position along the 
        //opponent's goal line the player should aim for.
        Vector2D BallTarget = new Vector2D();

        //the dot product is used to adjust the shooting force. The more
        //directly the ball is ahead, the more forceful the kick
        double power = Prm.MaxShootingForce * dot;

        //if it is determined that the player could score a goal from this position
        //OR if he should just kick the ball anyway, the player will attempt
        //to make the shot
        if (player.Team().CanShoot(player.Ball().Pos(),
                power,
                BallTarget)
                || (RandFloat() < Prm.ChancePlayerAttemptsPotShot)) {
            if (def(PLAYER_STATE_INFO_ON)) {
                debug_con.print("Player ").print(player.ID()).print(" attempts a shot at ").print(BallTarget).print("");
            }

            //add some noise to the kick. We don't want players who are 
            //too accurate! The amount of noise can be adjusted by altering
            //Prm.PlayerKickingAccuracy
            BallTarget = AddNoiseToKick(player.Ball().Pos(), BallTarget);

            //this is the direction the ball will be kicked in
            Vector2D KickDirection = sub(BallTarget, player.Ball().Pos());

            player.Ball().Kick(KickDirection, power);

            //change state   
            player.GetFSM().ChangeState(Wait.Instance());

            player.FindSupport();

            return;
        }


        /* Attempt a pass to a player */

        //if a receiver is found this will point to it
        PlayerBase receiver = null;

        power = Prm.MaxPassingForce * dot;

        ObjectRef<PlayerBase> receiverRef = new ObjectRef<PlayerBase>();
        //test if there are any potential candidates available to receive a pass
        if (player.isThreatened()
                && player.Team().FindPass(player,
                receiverRef,
                BallTarget,
                power,
                Prm.MinPassDist)) {
            receiver = receiverRef.get();
            //add some noise to the kick
            BallTarget = AddNoiseToKick(player.Ball().Pos(), BallTarget);

            Vector2D KickDirection = sub(BallTarget, player.Ball().Pos());

            player.Ball().Kick(KickDirection, power);

            if (def(PLAYER_STATE_INFO_ON)) {
                debug_con.print("Player ").print(player.ID()).print(" passes the ball with force ").print(ttos(power,3)).print("  to player ").print(receiver.ID()).print("  Target is ").print(BallTarget).print("");
            }


            //let the receiver know a pass is coming 
            Dispatcher.DispatchMsg(SEND_MSG_IMMEDIATELY,
                    player.ID(),
                    receiver.ID(),
                    Msg_ReceiveBall,
                    BallTarget);


            //the player should wait at his current position unless instruced
            //otherwise  
            player.GetFSM().ChangeState(Wait.Instance());

            player.FindSupport();

            return;
        } //cannot shoot or pass, so dribble the ball upfield
        else {
            player.FindSupport();

            player.GetFSM().ChangeState(Dribble.Instance());
        }
    }

    @Override
    public void Exit(FieldPlayer player) {
    }

    @Override
    public boolean OnMessage(FieldPlayer e, final Telegram t) {
        return false;
    }
}
