package yatan.wiki.entities;

public class Article extends BaseEntity {
    private long id;
    private String title;
    private long revisionId;
    private String text;
    private boolean redirect;

    public Article() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getRevisionId() {
        return revisionId;
    }

    public void setRevisionId(long revisionId) {
        this.revisionId = revisionId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return "Article [id=" + id + ", title=" + title + ", revisionId=" + revisionId + ", text=" + text + "]";
    }

    public boolean isRedirect() {
        return redirect;
    }

    public void setRedirect(boolean redirect) {
        this.redirect = redirect;
    }
}
