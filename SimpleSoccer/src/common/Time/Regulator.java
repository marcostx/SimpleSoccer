/**
 *  Desc:   Use this class to regulate code flow (for an update function say)
 *          Instantiate the class with the frequency you would like your code
 *          section to flow (like 10 times per second) and then only allow 
 *          the program flow to continue if Ready() returns true
 * 
 * @author Petr (http://www.sallyx.org/)
 */
package common.Time;

import static common.misc.utils.*;

public class Regulator {

    //the time period between updates 
    private double m_dUpdatePeriod;
    //the next time the regulator allows code flow
    private long m_dwNextUpdateTime;

    public Regulator(double NumUpdatesPerSecondRqd) {
        m_dwNextUpdateTime = (long) (System.currentTimeMillis() + RandFloat() * 1000);

        if (NumUpdatesPerSecondRqd > 0) {
            m_dUpdatePeriod = 1000.0 / NumUpdatesPerSecondRqd;
        } else if (isEqual(0.0, NumUpdatesPerSecondRqd)) {
            m_dUpdatePeriod = 0.0;
        } else if (NumUpdatesPerSecondRqd < 0) {
            m_dUpdatePeriod = -1;
        }
    }
    //the number of milliseconds the update period can vary per required
    //update-step. This is here to make sure any multiple clients of this class
    //have their updates spread evenly
    private static final double UpdatePeriodVariator = 10.0;

    /**
     * @return true if the current time exceeds m_dwNextUpdateTime
     */
    public boolean isReady() {
        //if a regulator is instantiated with a zero freq then it goes into
        //stealth mode (doesn't regulate)
        if (isEqual(0.0, m_dUpdatePeriod)) {
            return true;
        }

        //if the regulator is instantiated with a negative freq then it will
        //never allow the code to flow
        if (m_dUpdatePeriod < 0) {
            return false;
        }

        long CurrentTime = System.currentTimeMillis();

        if (CurrentTime >= m_dwNextUpdateTime) {
            m_dwNextUpdateTime = (long) (CurrentTime + m_dUpdatePeriod + RandInRange(-UpdatePeriodVariator, UpdatePeriodVariator));
            return true;
        }

        return false;
    }
}
