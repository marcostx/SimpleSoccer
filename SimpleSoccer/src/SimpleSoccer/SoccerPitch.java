/**
 *  Desc:   A SoccerPitch is the main game object. It owns instances of
 *          two soccer teams, two goals, the playing area, the ball
 *          etc. This is the root class for all the game updates and
 *          renders etc
 * 
 * @author Petr (http://www.sallyx.org/)
 */
package SimpleSoccer;

import SimpleSoccer.TeamStates.PrepareForKickOff;
import common.misc.Cgdi;
import common.D2.Vector2D;
import common.Game.Region;
import common.D2.Wall2D;
import java.lang.reflect.Array;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import static SimpleSoccer.ParamLoader.Prm;
import static common.misc.Cgdi.gdi;
import static common.misc.Stream_Utility_function.ttos;

public class SoccerPitch {

    public static final int NumRegionsHorizontal = 6;
    public static final int NumRegionsVertical = 3;
    
    private SoccerBall m_pBall;
    private SoccerTeam m_pRedTeam;
    private SoccerTeam m_pBlueTeam;
    
    private Goal m_pRedGoal;
    private Goal m_pBlueGoal;
    //container for the boundary walls
    private List<Wall2D> m_vecWalls = new ArrayList<Wall2D>();
    //defines the dimensions of the playing area
    private Region m_pPlayingArea;
    //the playing field is broken up into regions that the team
    //can make use of to implement strategies.
    private List<Region> m_Regions;
    //true if a goal keeper has possession
    private boolean m_bGoalKeeperHasBall;
    //true if the game is in play. Set to false whenever the players
    //are getting ready for kickoff
    private boolean m_bGameOn;
    //set true to pause the motion
    private boolean m_bPaused;
    //local copy of client window dimensions
    private int m_cxClient,
            m_cyClient;

    /**
     ** this instantiates the regions the players utilize to  position
     ** themselves
     */
    private void CreateRegions(double width, double height) {
        //index into the vector
        int idx = m_Regions.size() - 1;

        for (int col = 0; col < NumRegionsHorizontal; ++col) {
            for (int row = 0; row < NumRegionsVertical; ++row) {
                m_Regions.set(idx, new Region(PlayingArea().Left() + col * width,
                        PlayingArea().Top() + row * height,
                        PlayingArea().Left() + (col + 1) * width,
                        PlayingArea().Top() + (row + 1) * height,
                        idx));
                --idx;
            }
        }
    }

//------------------------------- ctor -----------------------------------
//------------------------------------------------------------------------
    public SoccerPitch(int cx, int cy) {
        m_cxClient = cx;
        m_cyClient = cy;
        m_bPaused = false;
        m_bGoalKeeperHasBall = false;
        m_Regions = Arrays.asList((Region[]) Array.newInstance(Region.class, NumRegionsHorizontal * NumRegionsVertical));
        m_bGameOn = true;
        //define the playing area
        m_pPlayingArea = new Region(20, 20, cx - 20, cy - 20);

        //create the regions  
        CreateRegions(PlayingArea().Width() / (double) NumRegionsHorizontal,
                PlayingArea().Height() / (double) NumRegionsVertical);

        //create the goals
        m_pRedGoal = new Goal(new Vector2D(m_pPlayingArea.Left(), (cy - Prm.GoalWidth) / 2),
                new Vector2D(m_pPlayingArea.Left(), cy - (cy - Prm.GoalWidth) / 2),
                new Vector2D(1, 0));



        m_pBlueGoal = new Goal(new Vector2D(m_pPlayingArea.Right(), (cy - Prm.GoalWidth) / 2),
                new Vector2D(m_pPlayingArea.Right(), cy - (cy - Prm.GoalWidth) / 2),
                new Vector2D(-1, 0));


        //create the soccer ball
        m_pBall = new SoccerBall(new Vector2D((double) m_cxClient / 2.0, (double) m_cyClient / 2.0),
                Prm.BallSize,
                Prm.BallMass,
                m_vecWalls);


        //create the teams 
        m_pRedTeam = new SoccerTeam(m_pRedGoal, m_pBlueGoal, this, SoccerTeam.red);
        m_pBlueTeam = new SoccerTeam(m_pBlueGoal, m_pRedGoal, this, SoccerTeam.blue);

        //make sure each team knows who their opponents are
        m_pRedTeam.SetOpponents(m_pBlueTeam);
        m_pBlueTeam.SetOpponents(m_pRedTeam);

        //create the walls
        Vector2D TopLeft = new Vector2D(m_pPlayingArea.Left(), m_pPlayingArea.Top());
        Vector2D TopRight = new Vector2D(m_pPlayingArea.Right(), m_pPlayingArea.Top());
        Vector2D BottomRight = new Vector2D(m_pPlayingArea.Right(), m_pPlayingArea.Bottom());
        Vector2D BottomLeft = new Vector2D(m_pPlayingArea.Left(), m_pPlayingArea.Bottom());

        m_vecWalls.add(new Wall2D(BottomLeft, m_pRedGoal.RightPost()));
        m_vecWalls.add(new Wall2D(m_pRedGoal.LeftPost(), TopLeft));
        m_vecWalls.add(new Wall2D(TopLeft, TopRight));
        m_vecWalls.add(new Wall2D(TopRight, m_pBlueGoal.LeftPost()));
        m_vecWalls.add(new Wall2D(m_pBlueGoal.RightPost(), BottomRight));
        m_vecWalls.add(new Wall2D(BottomRight, BottomLeft));

        ParamLoader p = ParamLoader.Instance(); // WTF??
    }

//-------------------------------- dtor ----------------------------------
//------------------------------------------------------------------------
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        m_pBall = null;

        m_pRedTeam = null;
        m_pBlueTeam = null;

        m_pRedGoal = null;
        m_pBlueGoal = null;

        m_pPlayingArea = null;

        for (int i = 0; i < m_Regions.size(); ++i) {
            m_Regions.set(i, null);
        }
    }
    static int tick = 0;

    /**
     *  this demo works on a fixed frame rate (60 by default) so we don't need
     *  to pass a time_elapsed as a parameter to the game entities
     */
    public void Update() {
        if (m_bPaused) {
            return;
        }

        //update the balls
        m_pBall.Update();

        //update the teams
        m_pRedTeam.Update();
        m_pBlueTeam.Update();

        //if a goal has been detected reset the pitch ready for kickoff
        if (m_pBlueGoal.Scored(m_pBall) || m_pRedGoal.Scored(m_pBall)) {
            m_bGameOn = false;

            //reset the ball                                                      
            m_pBall.PlaceAtPosition(new Vector2D((double) m_cxClient / 2.0, (double) m_cyClient / 2.0));

            //get the teams ready for kickoff
            m_pRedTeam.GetFSM().ChangeState(PrepareForKickOff.Instance());
            m_pBlueTeam.GetFSM().ChangeState(PrepareForKickOff.Instance());
        }
    }

//------------------------------ Render ----------------------------------
//------------------------------------------------------------------------
    public boolean Render() {
        //draw the grass
        gdi.DarkGreenPen();
        gdi.DarkGreenBrush();
        gdi.Rect(0, 0, m_cxClient, m_cyClient);

        //render regions
        if (Prm.bRegions) {
            for (int r = 0; r < m_Regions.size(); ++r) {
                m_Regions.get(r).Render(true);
            }
        }

        //render the goals
        gdi.HollowBrush();
        gdi.RedPen();
        gdi.Rect(m_pPlayingArea.Left(), (m_cyClient - Prm.GoalWidth) / 2, m_pPlayingArea.Left() + 40,
                m_cyClient - (m_cyClient - Prm.GoalWidth) / 2);

        gdi.BluePen();
        gdi.Rect(m_pPlayingArea.Right(), (m_cyClient - Prm.GoalWidth) / 2, m_pPlayingArea.Right() - 40,
                m_cyClient - (m_cyClient - Prm.GoalWidth) / 2);

        //render the pitch markings
        gdi.WhitePen();
        gdi.Circle(m_pPlayingArea.Center(), m_pPlayingArea.Width() * 0.125);
        gdi.Line(m_pPlayingArea.Center().x, m_pPlayingArea.Top(), m_pPlayingArea.Center().x, m_pPlayingArea.Bottom());
        gdi.WhiteBrush();
        gdi.Circle(m_pPlayingArea.Center(), 2.0);


        //the ball
        gdi.WhitePen();
        gdi.WhiteBrush();
        m_pBall.Render();

        //Render the teams
        m_pRedTeam.Render();
        m_pBlueTeam.Render();

        //render the walls
        gdi.WhitePen();
        for (int w = 0; w < m_vecWalls.size(); ++w) {
            m_vecWalls.get(w).Render();
        }

        //show the score
        gdi.TextColor(Cgdi.red);
        gdi.TextAtPos((m_cxClient / 2) - 50, m_cyClient - 18,
                "Red: " + ttos(m_pBlueGoal.NumGoalsScored()));

        gdi.TextColor(Cgdi.blue);
        gdi.TextAtPos((m_cxClient / 2) + 10, m_cyClient - 18, 
                "Blue: " + ttos(m_pRedGoal.NumGoalsScored()));

        return true;
    }

    public void TogglePause() {
        m_bPaused = !m_bPaused;
    }

    public boolean Paused() {
        return m_bPaused;
    }

    public int cxClient() {
        return m_cxClient;
    }

    public int cyClient() {
        return m_cyClient;
    }

    public boolean GoalKeeperHasBall() {
        return m_bGoalKeeperHasBall;
    }

    public void SetGoalKeeperHasBall(boolean b) {
        m_bGoalKeeperHasBall = b;
    }

    public Region PlayingArea() {
        return m_pPlayingArea;
    }

    public List<Wall2D> Walls() {
        return m_vecWalls;
    }

    SoccerBall Ball() {
        return m_pBall;
    }

    public Region GetRegionFromIndex(int idx) {
        assert ((idx >= 0) && (idx < (int) m_Regions.size()));
        return m_Regions.get(idx);
    }

    public boolean GameOn() {
        return m_bGameOn;
    }

    public void SetGameOn() {
        m_bGameOn = true;
    }

    public void SetGameOff() {
        m_bGameOn = false;
    }
}
