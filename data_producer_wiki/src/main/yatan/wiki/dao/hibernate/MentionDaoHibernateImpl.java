package yatan.wiki.dao.hibernate;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.Query;

import yatan.wiki.dao.DaoException;
import yatan.wiki.dao.MentionDao;
import yatan.wiki.entities.Mention;

public class MentionDaoHibernateImpl implements MentionDao {
    @Override
    public void save(Mention entity) throws DaoException {
        // TODO Auto-generated method stub
    }

    @Override
    public void save(List<Mention> entities) throws DaoException {
        EntityManager entityManager = getEntityManager();
        entityManager.getTransaction().begin();
        try {
            for (Mention entity : entities) {
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
    public Mention get(long id) throws DaoException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void delete(long id) throws DaoException {
        // TODO Auto-generated method stub
    }

    @SuppressWarnings("unchecked")
    public List<Mention> getMentions(long fromId, int limit) throws DaoException {
        EntityManager entityManager = getEntityManager();
        try {
            Query query = entityManager.createQuery("SELECT m FROM Mention m WHERE m.id >= ?1");
            query.setParameter(1, fromId);
            query.setMaxResults(limit);
            return new ArrayList<Mention>(query.getResultList());
        } catch (PersistenceException e) {
            throw new DaoException("Cannot get mentions. PersistenceException: " + e.getMessage(), e);
        } finally {
            entityManager.close();
        }
    }

    private static EntityManager getEntityManager() {
        return EntityManagerFactoryContainer.getEntityManagerFactory().createEntityManager();
    }
}
