/**
 * State machine class. Inherit from this class and create some 
 * states to give your agents FSM functionality
 * 
 * @author Petr (http://www.sallyx.org/)
 */
package common.FSM;

import common.Messaging.Telegram;

public class StateMachine<entity_type> {
    //a pointer to the agent that owns this instance

    private entity_type m_pOwner;
    private State<entity_type> m_pCurrentState;
    //a record of the last state the agent was in
    private State<entity_type> m_pPreviousState;
    //this is called every time the FSM is updated
    private State<entity_type> m_pGlobalState;

    public StateMachine(entity_type owner) {
        m_pOwner = owner;
        m_pCurrentState = null;
        m_pPreviousState = null;
        m_pGlobalState = null;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    //use these methods to initialize the FSM
    public void SetCurrentState(State<entity_type> s) {
        m_pCurrentState = s;
    }

    public void SetGlobalState(State<entity_type> s) {
        m_pGlobalState = s;
    }

    public void SetPreviousState(State<entity_type> s) {
        m_pPreviousState = s;
    }

    //call this to update the FSM
    public void Update() {
        //if a global state exists, call its execute method, else do nothing
        if (m_pGlobalState != null) {
            m_pGlobalState.Execute(m_pOwner);
        }

        //same for the current state
        if (m_pCurrentState != null) {
            m_pCurrentState.Execute(m_pOwner);
        }
    }

    public boolean HandleMessage(Telegram msg) {
        //first see if the current state is valid and that it can handle
        //the message
        if (m_pCurrentState != null && m_pCurrentState.OnMessage(m_pOwner, msg)) {
            return true;
        }

        //if not, and if a global state has been implemented, send 
        //the message to the global state
        if (m_pGlobalState != null && m_pGlobalState.OnMessage(m_pOwner, msg)) {
            return true;
        }

        return false;
    }

    //change to a new state
    public void ChangeState(State<entity_type> pNewState) {
        assert pNewState != null : "<StateMachine::ChangeState>: trying to change to NULL state";

        //keep a record of the previous state
        m_pPreviousState = m_pCurrentState;

        //call the exit method of the existing state
        m_pCurrentState.Exit(m_pOwner);

        //change state to the new state
        m_pCurrentState = pNewState;

        //call the entry method of the new state
        m_pCurrentState.Enter(m_pOwner);
    }

    //change state back to the previous state
    public void RevertToPreviousState() {
        ChangeState(m_pPreviousState);
    }

    //returns true if the current state's type is equal to the type of the
    //class passed as a parameter. 
    public boolean isInState(State<entity_type> st) {
        return m_pCurrentState.getClass() == st.getClass();
    }

    public State<entity_type> CurrentState() {
        return m_pCurrentState;
    }

    public State<entity_type> GlobalState() {
        return m_pGlobalState;
    }

    public State<entity_type> PreviousState() {
        return m_pPreviousState;
    }
    //only ever used during debugging to grab the name of the current state

    public String GetNameOfCurrentState() {
        String [] s = m_pCurrentState.getClass().getName().split("\\.");
        if(s.length > 0) {
            return s[s.length-1];
        }
        return m_pCurrentState.getClass().getName();
    }
}