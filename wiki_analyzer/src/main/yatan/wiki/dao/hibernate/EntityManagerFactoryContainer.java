package yatan.wiki.dao.hibernate;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class EntityManagerFactoryContainer {
    private static EntityManagerFactory ENTITY_MANAGER_FACTORY;

    public static EntityManagerFactory getEntityManagerFactory() {
        if (ENTITY_MANAGER_FACTORY == null) {
            ENTITY_MANAGER_FACTORY = Persistence.createEntityManagerFactory("wiki");
        }

        return ENTITY_MANAGER_FACTORY;
    }
}
