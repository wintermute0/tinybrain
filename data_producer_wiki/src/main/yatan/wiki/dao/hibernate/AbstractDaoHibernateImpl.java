package yatan.wiki.dao.hibernate;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;

import yatan.wiki.dao.AbstractDao;
import yatan.wiki.dao.DaoException;
import yatan.wiki.entities.Abstract;

public class AbstractDaoHibernateImpl implements AbstractDao {
    @Override
    public void save(Abstract entity) throws DaoException {
    }

    @Override
    public void save(List<Abstract> entities) throws DaoException {
        EntityManager entityManager = getEntityManager();
        entityManager.getTransaction().begin();
        try {
            for (Abstract entity : entities) {
                entityManager.persist(entity);
            }
            entityManager.getTransaction().commit();
        } catch (PersistenceException e) {
            entityManager.getTransaction().rollback();
            throw e;
        } finally {
            entityManager.close();
        }
    }

    @Override
    public Abstract get(long id) throws DaoException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void delete(long id) throws DaoException {
        // TODO Auto-generated method stub

    }

    private static EntityManager getEntityManager() {
        return EntityManagerFactoryContainer.getEntityManagerFactory().createEntityManager();
    }
}
