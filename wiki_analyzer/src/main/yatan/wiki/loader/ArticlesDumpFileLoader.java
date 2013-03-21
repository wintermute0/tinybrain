package yatan.wiki.loader;

import info.bliki.wiki.filter.PlainTextConverter;
import info.bliki.wiki.model.WikiModel;

import java.io.BufferedReader;

import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import yatan.wiki.dao.ArticleDao;
import yatan.wiki.dao.DaoException;
import yatan.wiki.dao.mysql.ArticleDaoImpl;
import yatan.wiki.entities.Article;

public class ArticlesDumpFileLoader {
    private static final int FLUSH_THRESHOLD = 5000;
    private static final Logger LOGGER = Logger.getLogger(ArticlesDumpFileLoader.class);

    private final List<Article> articlesBuffer = new ArrayList<Article>();
    private ArticleDao articleDao;

    private int total = 0;

    public static void main(String[] args) throws Exception {
        Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/wiki_zh", "root", "topcoder");

        try {
            ArticleDaoImpl articleDaoImpl = new ArticleDaoImpl();
            articleDaoImpl.setConnection(connection);

            ArticlesDumpFileLoader loader = new ArticlesDumpFileLoader();
            loader.setArticleDao(articleDaoImpl);
            loader.loadArticlesDumpFile("e:/zhwiki-latest-pages-articles.xml");
        } finally {
            connection.close();
        }
    }

    public void loadArticlesDumpFile(String file) throws Exception {
        if (articleDao == null) {
            throw new IllegalStateException("Article Dao not set.");
        }

        LOGGER.debug("Start reading...");
        parseArticlesDumpFile(file);
        flush();
        LOGGER.debug("All done.");
    }

    private void flush() {
        LOGGER.debug("Flushing " + this.articlesBuffer.size() + " articles...");
        int retryCount = 0;
        boolean success = false;
        while (!success && retryCount < 5) {
            try {
                this.articleDao.save(this.articlesBuffer);
                success = true;
            } catch (DaoException e) {
                LOGGER.error("Error occurred while flushing articles. Retry " + ++retryCount, e);
                LOGGER.debug("Sleep before retry.");
                try {
                    Thread.sleep(60 * 1000);
                } catch (InterruptedException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
                LOGGER.debug("Wake up and retry.");
            }
        }
        this.total += this.articlesBuffer.size();
        this.articlesBuffer.clear();
        LOGGER.debug("Done. Total " + this.total + ". Reading some more...");
    }

    private void parseArticlesDumpFile(String file) throws Exception {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();

        DefaultHandler handler = new DefaultHandler() {
            private WikiModel wikiModel = new WikiModel("http://www.wikipedia.org/wiki/${image}",
                    "http://www.wikipedia.org/wiki/${title}");

            private Article article;

            private boolean title;
            private boolean id;
            private boolean revision;
            private boolean revisionId;
            private boolean text;

            private int depth;
            private int pageDepth;
            private int revisionDepth;

            private StringBuilder textBuilder;

            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes)
                    throws SAXException {
                super.startElement(uri, localName, qName, attributes);

                this.depth++;

                if (qName.equals("page")) {
                    this.pageDepth = this.depth;
                    this.article = new Article();
                } else if (qName.equals("title")) {
                    this.title = true;
                } else if (qName.equals("revision")) {
                    this.revisionDepth = this.depth;
                    this.revision = true;
                } else if (qName.equals("id")) {
                    if (this.revision) {
                        this.revisionId = true;
                    } else {
                        this.id = true;
                    }
                } else if (qName.equals("text")) {
                    this.text = true;
                    this.textBuilder = new StringBuilder();
                }
            };

            @Override
            public void endElement(String uri, String localName, String qName) throws SAXException {
                super.endElement(uri, localName, qName);

                this.depth--;

                if (qName.equals("page")) {
                    ArticlesDumpFileLoader.this.articlesBuffer.add(article);
                    if (ArticlesDumpFileLoader.this.articlesBuffer.size() > FLUSH_THRESHOLD) {
                        flush();
                    }
                } else if (qName.equals("title")) {
                    this.title = false;
                } else if (qName.equals("revision")) {
                    this.revision = false;
                } else if (qName.equals("id")) {
                    if (this.revision) {
                        this.revisionId = false;
                    } else {
                        this.id = false;
                    }
                } else if (qName.equals("text")) {
                    this.text = false;
                    String plainText = wikiModel.render(new PlainTextConverter(), this.textBuilder.toString());
                    this.article.setText(plainText);
                }
            }

            @Override
            public void characters(char[] ch, int start, int length) throws SAXException {
                super.characters(ch, start, length);

                String value = new String(ch, start, length);
                if (this.depth == this.pageDepth + 1) {
                    if (this.title) {
                        article.setTitle((article.getTitle() == null ? "" : article.getTitle()) + value);
                    } else if (this.id) {
                        article.setId(Long.parseLong(value));
                    }
                } else if (this.depth == this.revisionDepth + 1) {
                    if (this.revisionId) {
                        article.setRevisionId(Long.parseLong(value));
                    } else if (this.text) {
                        this.textBuilder.append(StringEscapeUtils.unescapeHtml4(value));
                    }
                }
            }
        };

        FileReader reader = new FileReader(file);
        BufferedReader bufferedReader = new BufferedReader(reader);
        InputSource is = new InputSource(bufferedReader);
        is.setEncoding("UTF-8");

        parser.parse(is, handler);
    }

    public ArticleDao getArticleDao() {
        return articleDao;
    }

    public void setArticleDao(ArticleDao articleDao) {
        this.articleDao = articleDao;
    }
}
