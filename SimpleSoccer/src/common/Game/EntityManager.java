/**
 * Desc:   Singleton class to handle the  management of Entities.          
 * 
 * @author Petr (http://www.sallyx.org/)
 */
package common.Game;

import SimpleSoccer.BaseGameEntity;
import java.util.HashMap;

public class EntityManager {
    //provide easy access

    public static final EntityManager EntityMgr = new EntityManager();

    private class EntityMap extends HashMap<Integer, BaseGameEntity> {
    }
    //to facilitate quick lookup the entities are stored in a std::map, in which
    //pointers to entities are cross referenced by their identifying number
    private EntityMap m_EntityMap = new EntityMap();

    private EntityManager() {
    }

//copy ctor and assignment should be private
    private EntityManager(final EntityManager m) {
    }

//--------------------------- Instance ----------------------------------------
//   this class is a singleton
//-----------------------------------------------------------------------------
    public static EntityManager Instance() {
        return EntityMgr;
    }

    /**
     * this method stores a pointer to the entity in the std::vector
     * m_Entities at the index position indicated by the entity's ID
     * (makes for faster access)
     */
    public void RegisterEntity(BaseGameEntity NewEntity) {
        m_EntityMap.put(NewEntity.ID(), NewEntity);
    }

    /**
     * @return a pointer to the entity with the ID given as a parameter
     */
    public BaseGameEntity GetEntityFromID(int id) {
        //find the entity
        BaseGameEntity ent = m_EntityMap.get(id);

        //assert that the entity is a member of the map
        assert (ent != null) : "<EntityManager::GetEntityFromID>: invalid ID";

        return ent;
    }

    /**
     * this method removes the entity from the list
     */
    public void RemoveEntity(BaseGameEntity pEntity) {
        m_EntityMap.remove(pEntity.ID());
    }

    /**
     * clears all entities from the entity map
     */
    public void Reset() {
        m_EntityMap.clear();
    }
}
