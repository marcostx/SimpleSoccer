/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package SimpleSoccer;

import static SimpleSoccer.Main.buffer;
import static SimpleSoccer.Main.CheckAllMenuItemsAppropriately;
import static SimpleSoccer.Main.createPanel;
import static SimpleSoccer.Main.g_SoccerPitch;
import static SimpleSoccer.Main.hdcBackBuffer;
import static SimpleSoccer.Main.SoccerPitchLock;
import static SimpleSoccer.Main.timer;
import static SimpleSoccer.constants.WindowHeight;
import static SimpleSoccer.constants.WindowWidth;
import static SimpleSoccer.resource.IDR_MENU1;
import java.awt.image.BufferedImage;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.swing.JApplet;

/**
 *
 * @author Petr
 */
public class Applet extends JApplet {

    private int cxClient;
    private int cyClient;
    Thread thread;
    final private Lock threadLock = new ReentrantLock();

    /**
     * Initialization method that will be called after the applet is loaded into
     * the browser.
     */
    public void init() {
        buffer = new BufferedImage(WindowWidth, WindowHeight, BufferedImage.TYPE_INT_RGB);
        hdcBackBuffer = buffer.createGraphics();
        //these hold the dimensions of the client window area
        cxClient = buffer.getWidth();
        cyClient = buffer.getHeight();
        //seed random number generator
        common.misc.utils.setSeed(0);
        Script1.MyMenuBar menu = Script1.createMenu(IDR_MENU1);
        this.setJMenuBar(menu);
        g_SoccerPitch = new SoccerPitch(cxClient, cyClient);

        CheckAllMenuItemsAppropriately(menu);

        createPanel();

        this.add(Main.panel);

        Runnable r = new Runnable() {
            @Override
            public void run() {
                runThread();
            }
        };
        thread = new Thread(r);
        thread.start();
        threadLock.lock();
    }

    @Override
    public void start() {
        timer.TimeElapsed();
        threadLock.unlock();
    }

    @Override
    public void stop() {
        threadLock.lock();
    }

    private void runThread() {

        timer.Start();

        while (true) {
            //update
            if (timer.ReadyForNextFrame()) {
                SoccerPitchLock.lock();
                g_SoccerPitch.Update();
                SoccerPitchLock.unlock();
                //render
                //panel.revalidate();
                Main.panel.repaint();

                try {
                    //System.out.println(timer.TimeElapsed());
                    Thread.sleep(2);
                } catch (InterruptedException ex) {
                }
            }
        }//end while
    }
}
