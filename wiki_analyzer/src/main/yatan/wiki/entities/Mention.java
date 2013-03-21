package yatan.wiki.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "mention")
public class Mention extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private long articleId;
    private String mention;
    @Column(name = "target_title")
    private String targetTitle;
    private int mentionType;

    public static enum Type {
        TITLE(1), LINK(2), REDIRECT(3), DISAMBIGUATION(4);

        private final int value;

        public static Type fromValue(int value) {
            if (value == TITLE.value) {
                return TITLE;
            } else if (value == LINK.value) {
                return LINK;
            } else if (value == REDIRECT.value) {
                return REDIRECT;
            } else if (value == DISAMBIGUATION.value) {
                return DISAMBIGUATION;
            }

            throw new IllegalArgumentException("No such Mention.Type for value '" + value + "'.");
        }

        private Type(int value) {
            this.value = value;
        }
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getArticleId() {
        return articleId;
    }

    public void setArticleId(long articleId) {
        this.articleId = articleId;
    }

    public String getMention() {
        return mention;
    }

    public void setMention(String mention) {
        this.mention = mention;
    }

    public String getTargetTitle() {
        return targetTitle;
    }

    public void setTargetTitle(String targetTitle) {
        this.targetTitle = targetTitle;
    }

    public void setMentionType(Type type) {
        this.mentionType = type.value;
    }

    public Type getMentionType() {
        return Type.fromValue(this.mentionType);
    }

    @Override
    public String toString() {
        return "Mention [id=" + id + ", articleId=" + articleId + ", mention=" + mention + ", targetTitle="
                + targetTitle + ", mentionType=" + mentionType + "]";
    }
}
