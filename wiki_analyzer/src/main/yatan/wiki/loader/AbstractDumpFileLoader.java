package yatan.wiki.loader;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import yatan.wiki.dao.AbstractDao;
import yatan.wiki.dao.DaoException;
import yatan.wiki.dao.hibernate.AbstractDaoHibernateImpl;
import yatan.wiki.entities.Abstract;

public class AbstractDumpFileLoader {
    private static final int FLUSH_THRESHOLD = 10000;
    private static final Logger LOGGER = Logger.getLogger(ArticlesDumpFileLoader.class);

    private final List<Abstract> buffer = new ArrayList<Abstract>();
    private AbstractDao abstractDao;

    private int total = 0;

    public static void main(String[] args) throws Exception {
        AbstractDumpFileLoader loader = new AbstractDumpFileLoader();

        loader.setAbstractDao(new AbstractDaoHibernateImpl());
        loader.loadAbstractDumpFile("F:\\master_projects\\irica\\data\\wiki\\zhwiki-latest-abstract.xml");
    }

    public void loadAbstractDumpFile(String file) throws Exception {
        if (abstractDao == null) {
            throw new IllegalStateException("Abstract Dao not set.");
        }

        LOGGER.debug("Start reading...");
        parseAbstractDumpFile(file);
        flush();
        LOGGER.debug("All done.");
    }

    private void flush() {
        LOGGER.debug("Flushing " + this.buffer.size() + " abstracts...");
        int retryCount = 0;
        boolean success = false;
        while (!success && retryCount < 5) {
            try {
                this.abstractDao.save(this.buffer);
                success = true;
            } catch (DaoException e) {
                LOGGER.error("Error occurred while flushing abstracts. Retry " + ++retryCount, e);
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
        this.total += this.buffer.size();
        this.buffer.clear();
        LOGGER.debug("Done. Total " + this.total + ". Reading some more...");
    }

    private void parseAbstractDumpFile(String file) throws Exception {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();

        DefaultHandler handler = new DefaultHandler() {
            private Abstract abstractEntity;

            private boolean title;
            private boolean abstractText;

            private int depth;
            private int docDepth;

            private StringBuilder textBuilder;

            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes)
                    throws SAXException {
                super.startElement(uri, localName, qName, attributes);

                this.depth++;

                if (qName.equals("doc")) {
                    this.docDepth = this.depth;
                    this.abstractEntity = new Abstract();
                } else if (qName.equals("title") && this.depth == this.docDepth + 1) {
                    this.title = true;
                } else if (qName.equals("abstract") && this.depth == this.docDepth + 1) {
                    this.abstractText = true;
                    this.textBuilder = new StringBuilder();
                }
            };

            @Override
            public void endElement(String uri, String localName, String qName) throws SAXException {
                super.endElement(uri, localName, qName);

                this.depth--;

                if (qName.equals("doc")) {
                    if (this.abstractEntity.getTitle() != null) {
                        if (this.abstractEntity.getAbstractText().length() >= 10
                                && !this.abstractEntity.getAbstractText().endsWith("：")) {
                            char c = this.abstractEntity.getAbstractText().charAt(0);
                            if ((Character.isDigit(c) || Character.isLetter(c) || c == '《' || c == '“')
                                    && findLetterOtherThanEnglish(this.abstractEntity.getAbstractText())) {
                                AbstractDumpFileLoader.this.buffer.add(this.abstractEntity);
                                if (AbstractDumpFileLoader.this.buffer.size() > FLUSH_THRESHOLD) {
                                    flush();
                                }
                            } else {
                                // System.out.println(this.abstractEntity.getAbstractText());
                            }
                        } else {
                            // System.out.println(this.abstractEntity.getAbstractText());
                        }
                    }
                } else if (qName.equals("title")) {
                    this.title = false;
                } else if (qName.equals("abstract")) {
                    this.abstractText = false;
                    this.abstractEntity.setAbstractText(this.textBuilder.toString());
                }
            }

            private boolean findLetterOtherThanEnglish(String str) {
                for (int i = 0; i < str.length(); i++) {
                    char c = str.charAt(i);
                    if (Character.isLetter(c) && !(Character.isLowerCase(c) || Character.isUpperCase(c))) {
                        return true;
                    }
                }

                return false;
            }

            @Override
            public void characters(char[] ch, int start, int length) throws SAXException {
                super.characters(ch, start, length);

                String value = new String(ch, start, length);
                if (this.title) {
                    if (!value.startsWith("Wikipedia: ")) {
                        this.abstractEntity.setTitle(null);
                    } else {
                        this.abstractEntity.setTitle(value.substring("Wikipedia: ".length()).trim());
                    }
                } else if (this.abstractText) {
                    this.textBuilder.append(value);
                }
            }

            @Override
            public void endDocument() throws SAXException {
                flush();
            }
        };

        FileReader reader = new FileReader(file);
        BufferedReader bufferedReader = new BufferedReader(reader);
        InputSource is = new InputSource(bufferedReader);
        is.setEncoding("UTF-8");

        parser.parse(is, handler);
    }

    public AbstractDao getAbstractDao() {
        return abstractDao;
    }

    public void setAbstractDao(AbstractDao abstractDao) {
        this.abstractDao = abstractDao;
    }
}
