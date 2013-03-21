package yatan.wiki.dao;

import java.util.List;

import yatan.wiki.entities.BaseEntity;

public interface BaseDao<T extends BaseEntity> {
    public void save(T entity) throws DaoException;

    public void save(List<T> entities) throws DaoException;

    public T get(long id) throws DaoException;
    
    public void delete(long id) throws DaoException;
}
