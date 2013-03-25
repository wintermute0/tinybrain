package yatan.wiki.dao.mysql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import yatan.wiki.dao.ArticleDao;
import yatan.wiki.dao.DaoException;
import yatan.wiki.entities.Article;

public class ArticleDaoImpl extends BaseDaoImpl<Article> implements ArticleDao {
    @Override
    public void save(Article entity) throws DaoException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void save(List<Article> entities) throws DaoException {
        PreparedStatement ps = null;
        try {
            ps =
                    getConnection().prepareStatement(
                            "INSERT INTO ARTICLE (ID, TITLE, REVISIONID, TEXT) VALUES (?, ?, ?, ?)");
            for (Article article : entities) {
                int index = 1;
                ps.setLong(index++, article.getId());
                ps.setString(index++, article.getTitle());
                ps.setLong(index++, article.getRevisionId());
                ps.setString(index++, article.getText());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            throw new DaoException("Cannot save entities. SQLException: " + e.getMessage(), e);
        } finally {
            close(null, ps);
        }
    }

    @Override
    public Article get(long id) throws DaoException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = getConnection().prepareStatement("SELECT TITLE, REVISIONID, TEXT FROM ARTICLE WHERE ID = ?");
            ps.setLong(1, id);
            rs = ps.executeQuery();
            if (rs.next()) {
                Article article = new Article();
                int index = 1;
                article.setId(id);
                article.setTitle(rs.getString(index++));
                article.setRevisionId(rs.getLong(index++));
                article.setText(rs.getString(index++));
                return article;
            }

            return null;
        } catch (SQLException e) {
            throw new DaoException("");
        } finally {
            close(rs, ps);
        }
    }

    @Override
    public void delete(long id) throws DaoException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Article> getArticlesByIdRange(long start, long end, int limit) throws DaoException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps =
                    getConnection()
                            .prepareStatement(
                                    "SELECT ID, TITLE, REVISIONID, TEXT FROM ARTICLE WHERE ID >= ? AND ID < ? ORDER BY ID LIMIT ?");
            ps.setLong(1, start);
            ps.setLong(2, end);
            ps.setLong(3, limit);
            rs = ps.executeQuery();

            List<Article> articles = new ArrayList<Article>();
            while (rs.next()) {
                Article article = new Article();
                int index = 1;
                article.setId(rs.getLong(index++));
                article.setTitle(rs.getString(index++));
                article.setRevisionId(rs.getLong(index++));
                article.setText(rs.getString(index++));
                articles.add(article);
            }

            return articles;
        } catch (SQLException e) {
            throw new DaoException("Can't get articles by id range. SQLException: " + e.getMessage(), e);
        } finally {
            close(rs, ps);
        }
    }

    @Override
    public void markRedirect(List<Long> ids) throws DaoException {
        throw new UnsupportedOperationException();
    }
}
