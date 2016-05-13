/**
 * Desc: Base class to define a common interface for all game
 *       entities
 * 
 * @author Petr (http://www.sallyx.org/)
 */
package SimpleSoccer;

import common.D2.Vector2D;
import common.Messaging.Telegram;
import static common.misc.utils.*;

public abstract class BaseGameEntity {

    public static int default_entity_type = -1;
    //each entity has a unique ID
    private int m_ID;
    //every entity has a type associated with it (health, troll, ammo etc)
    private int m_iType;
    //this is a generic flag. 
    private boolean m_bTag;
    //this is the next valid ID. Each time a BaseGameEntity is instantiated
    //this value is updated
    private static int m_iNextValidID = 0;

    /**
     *  this must be called within each constructor to make sure the ID is set
     *  correctly. It verifies that the value passed to the method is greater
     *  or equal to the next valid ID, before setting the ID and incrementing
     *  the next valid ID
     */
    private void SetID(int val) {
        //make sure the val is equal to or greater than the next available ID
        assert (val >= m_iNextValidID) : "<BaseGameEntity::SetID>: invalid ID";

        m_ID = val;

        m_iNextValidID = m_ID + 1;
    }
    //its location in the environment
    protected Vector2D m_vPosition = new Vector2D();
    protected Vector2D m_vScale = new Vector2D();
    //the magnitude of this object's bounding radius
    protected double m_dBoundingRadius;

//------------------------------ ctor -----------------------------------------
//-----------------------------------------------------------------------------
    protected BaseGameEntity(int ID) {
        m_dBoundingRadius = 0.0;
        m_vScale = new Vector2D(1.0, 1.0);
        m_iType = default_entity_type;
        m_bTag = false;

        SetID(ID);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    public void Update() {
    }

    abstract public void Render();

    public boolean HandleMessage(Telegram msg) {
        return false;
    }

    //entities should be able to read/write their data to a stream
    //virtual void Write(std::ostream&  os)const{}
    //virtual void Read (std::ifstream& is){}
    //use this to grab the next valid ID
    public static int GetNextValidID() {
        return m_iNextValidID;
    }

    //this can be used to reset the next ID
    public static void ResetNextValidID() {
        m_iNextValidID = 0;
    }

    public Vector2D Pos() {
        return new Vector2D(m_vPosition);
    }

    public void SetPos(Vector2D new_pos) {
        m_vPosition = new Vector2D(new_pos);
    }

    public double BRadius() {
        return m_dBoundingRadius;
    }

    public void SetBRadius(double r) {
        m_dBoundingRadius = r;
    }

    public int ID() {
        return m_ID;
    }

    public boolean IsTagged() {
        return m_bTag;
    }

    public void Tag() {
        m_bTag = true;
    }

    public void UnTag() {
        m_bTag = false;
    }

    public Vector2D Scale() {
        return new Vector2D(m_vScale);
    }

    public void SetScale(Vector2D val) {
        m_dBoundingRadius *= MaxOf(val.x, val.y) / MaxOf(m_vScale.x, m_vScale.y);
        m_vScale = new Vector2D(val);
    }

    public void SetScale(double val) {
        m_dBoundingRadius *= (val / MaxOf(m_vScale.x, m_vScale.y));
        m_vScale = new Vector2D(val, val);
    }

    public int EntityType() {
        return m_iType;
    }

    public void SetEntityType(int new_type) {
        m_iType = new_type;
    }
}
