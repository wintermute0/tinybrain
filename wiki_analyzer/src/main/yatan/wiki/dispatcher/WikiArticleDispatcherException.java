package yatan.wiki.dispatcher;

@SuppressWarnings("serial")
public class WikiArticleDispatcherException extends Exception {
    public WikiArticleDispatcherException(String message) {
        super(message);
    }

    public WikiArticleDispatcherException(String message, Throwable cause) {
        super(message, cause);
    }
}
