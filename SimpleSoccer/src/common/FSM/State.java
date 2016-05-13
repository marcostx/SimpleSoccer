/**
 * abstract base class to define an interface for a state
 * 
 * @author Petr (http://www.sallyx.org/)
 */
package common.FSM;

import common.Messaging.Telegram;

public abstract class State<entity_type>  {

  @Override
  public void finalize() throws Throwable{ super.finalize();}

  //this will execute when the state is entered
  abstract public void Enter(entity_type e);

  //this is the state's normal update function
  abstract public void Execute(entity_type e);

  //this will execute when the state is exited. (My word, isn't
  //life full of surprises... ;o))
  abstract public void Exit(entity_type e);
  
  //this executes if the agent receives a message from the 
  //message dispatcher
  abstract public boolean OnMessage(entity_type e, Telegram t);
}
