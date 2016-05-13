/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package common.Messaging;

import SimpleSoccer.MessageTypes;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author robot
 */
public class TelegramTest {

    public TelegramTest() {
    }

	@BeforeClass
	public static void setUpClass() throws Exception {
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
	}

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

	/**
	 * Test of equals method, of class Telegram.
	 */ @Test
	public void testEquals() {
		Telegram t1 = new Telegram(0.5, 1, 2, MessageTypes.Msg_GoHome);
		Telegram t2 = new Telegram(0.5+Telegram.SmallestDelay-0.0001, 1, 2, MessageTypes.Msg_GoHome);
		boolean result = t1.equals(t2);
		assertEquals(true, result);
	}

	/**
	 * Test of hashCode method, of class Telegram.
	 */ @Test
	public void testHashCode() {
		Telegram t1 = new Telegram(0.5, 1, 2, MessageTypes.Msg_GoHome);
		Telegram t2 = new Telegram(0.5+Telegram.SmallestDelay-0.0001, 1, 2, MessageTypes.Msg_GoHome);
		assertEquals("Equals object must have equal hashCode", t1.hashCode(), t2.hashCode());
	}

	/**
	 * Test of compareTo method, of class Telegram.
	 */ @Test
	public void testCompareTo() {
		Telegram t1 = new Telegram(0.5, 1, 2, MessageTypes.Msg_GoHome);
		Telegram t2 = new Telegram(0.5, 1, 2, MessageTypes.Msg_GoHome);
		int result = t1.compareTo(t2);
		assertEquals(0, result);
	}
}