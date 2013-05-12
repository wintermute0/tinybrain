package yatan.wiki.dao;

import java.util.List;

import yatan.wiki.entities.Article;

public interface ArticleDao extends BaseDao<Article> {
    /**
     * Include start, exclude end.
     * @param start
     * @param end
     * @return
     * @throws DaoException
     */
    public List<Article> getArticlesByIdRange(long start, long end, int limit) throws DaoException;

    public void markRedirect(List<Long> ids) throws DaoException;
}
