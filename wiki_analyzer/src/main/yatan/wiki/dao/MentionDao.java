package yatan.wiki.dao;

import java.util.List;

import yatan.wiki.entities.Mention;

public interface MentionDao extends BaseDao<Mention> {
    public List<Mention> getMentions(long fromId, int limit) throws DaoException;
}
