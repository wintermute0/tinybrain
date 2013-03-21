package yatan.wiki.dispatcher;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import yatan.wiki.dao.ArticleDao;
import yatan.wiki.dao.DaoException;
import yatan.wiki.dao.mysql.ArticleDaoImpl;
import yatan.wiki.entities.Article;

public class WikiArticleDispatcher {
    public static interface ArticleProcessor {
        public void processArticles(List<Article> articles) throws Exception;
    }

    public void run(long startId, ArticleProcessor processor) throws WikiArticleDispatcherException {
        Logger logger = Logger.getLogger(WikiArticleDispatcher.class);

        ArticleDao articleDao = new ArticleDaoImpl();
        logger.debug("Connecting to article database...");
        Connection connection;
        try {
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/wiki", "root", "topcoder");
        } catch (SQLException e) {
            throw new WikiArticleDispatcherException("Cannot connect to database.", e);
        }
        logger.debug("Connected.");
        ((ArticleDaoImpl) articleDao).setConnection(connection);

        long startTime = new Date().getTime();
        int total = 0;
        int batchSize = 3000;
        List<Article> articles = new ArrayList<Article>();
        while (true) {
            logger.debug("Loading article from id " + startId + ", limit " + batchSize);
            try {
                articles = articleDao.getArticlesByIdRange(startId, Long.MAX_VALUE, batchSize);
            } catch (DaoException e) {
                throw new WikiArticleDispatcherException("Cannot load articles. DaoException: " + e.getMessage(), e);
            }
            if (articles.isEmpty()) {
                logger.debug("No more articles.");
                break;
            }
            total += articles.size();
            logger.debug("Articles loaded. Processing...");
            try {
                processor.processArticles(articles);
            } catch (Exception e) {
                throw new WikiArticleDispatcherException("Error occurred while processing articles: " + e.getMessage(),
                        e);
            }
            logger.debug("Process completed. Total " + total + " articles has been processed. Average "
                    + (new Date().getTime() - startTime) / (total / 10000.0) / 1000 + " seconds per 10000 articles.");
            startId = articles.get(articles.size() - 1).getId() + 1;
        }
    }
}
