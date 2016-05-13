/*
 * @author Petr (http://www.sallyx.org/)
 */
package common.Messaging;

import SimpleSoccer.SoccerPitch;
import SimpleSoccer.PlayerBase;
import SimpleSoccer.FieldPlayerStates.Wait;
import SimpleSoccer.FieldPlayer;
import SimpleSoccer.GoalKeeper;
import common.D2.Vector2D;
import SimpleSoccer.GoalKeeperStates.TendGoal;
import common.Game.EntityManager;
import SimpleSoccer.MessageTypes;
import static SimpleSoccer.ParamLoader.Prm;
import common.misc.FrameCounter;
import org.junit.*;
import static org.junit.Assert.*;

/**
 *
 * @author petr
 */
public class MessageDispatcherTest {
    
    public MessageDispatcherTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void setUp() {
	   // create players instances
	   // dispatching messages needs it
	   new SoccerPitch(100, 100);
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of clone method, of class MessageDispatcher.
     */
    @Test
    public void testClone() throws Exception {
        MessageDispatcher instance = MessageDispatcher.Instance();
        try {
            instance.clone();
        } catch (CloneNotSupportedException ex) {
            return;
        }
        fail("Message dispatcher must be a singleton.");
    }

    /**
     * Test of Instance method, of class MessageDispatcher.
     */
    @Test
    public void testInstance() {
        MessageDispatcher instance = MessageDispatcher.Instance();
        assertNotNull(instance);
        assertEquals(instance, MessageDispatcher.Instance());
    }

    /**
     * Test of DispatchMsg method, of class MessageDispatcher.
     */
    @Test
    public void testDispatchMsg_4args() {

        MessageDispatcher instance = MessageDispatcher.Instance();
        instance.clear();
        instance.DispatchMsg(0.5, 1, 2, MessageTypes.Msg_GoHome);
        instance.DispatchMsg(0.5, 1, 2, MessageTypes.Msg_PassToMe);
        instance.DispatchMsg(0.5, 1, 2, MessageTypes.Msg_GoHome);
        assertEquals(2, instance.size());
    }

    /**
     * Test of DispatchMsg method, of class MessageDispatcher.
     */
    @Test
    public void testDispatchMsg_5args() {
        Object o = new Object();
        MessageDispatcher instance = MessageDispatcher.Instance();
        instance.clear();
        instance.DispatchMsg(0.5, 1, 2, MessageTypes.Msg_GoHome, o);
        instance.DispatchMsg(0.5, 1, 2, MessageTypes.Msg_GoHome, o);
        instance.DispatchMsg(0.5, 1, 2, MessageTypes.Msg_GoHome, new Object());
        assertEquals(2, instance.size());
    }
    
    @Test
    public void testSize() {
        MessageDispatcher instance = MessageDispatcher.Instance();
        instance.clear();
        assertEquals(0, instance.size());
        instance.DispatchMsg(0.5, 1, 2, MessageTypes.Msg_GoHome);
        assertEquals(1, instance.size());
        instance.DispatchMsg(0.5, 2, 1, MessageTypes.Msg_GoHome);
        assertEquals(2, instance.size());
    }

    @Test
    public void testClear() {
        MessageDispatcher instance = MessageDispatcher.Instance();
        instance.DispatchMsg(0.5, 1, 2, MessageTypes.Msg_GoHome);
        instance.clear();
        assertEquals(0, instance.size());
    }

    /**
     * Test of DispatchDelayedMessages method, of class MessageDispatcher.
     */
    @Test
    public void testDispatchDelayedMessages() throws InterruptedException {
        final double DELAY = 3;
        MessageDispatcher instance = MessageDispatcher.Instance();
        instance.DispatchMsg(DELAY, 1, 2, MessageTypes.Msg_GoHome);
        instance.DispatchMsg(DELAY, 1, 2, MessageTypes.Msg_Wait);
        int size = instance.size();
	for(int i = 0; i <= DELAY; i++) {
		FrameCounter.TickCounter.Update();
	}
        instance.DispatchDelayedMessages();
        assertTrue(size > instance.size());
    }
}
