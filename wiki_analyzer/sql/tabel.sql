CREATE TABLE ARTICLE (
    id BIGINT,
    title VARCHAR(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    revisionId BIGINT,
    text MEDIUMTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    PRIMARY KEY (id)
);

CREATE INDEX TITLE_INDEX ON ARTICLE (TITLE);

CREATE TABLE MENTION (
    id BIGINT AUTO_INCREMENT,
    articleId BIGINT,
    mention VARCHAR(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    target_title VARCHAR(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    mentionType TINYINT,
    PRIMARY KEY (id)
);

CREATE INDEX MENTION_MENTION_INDEX ON MENTION (mention);

CREATE INDEX MENTION_TARGET_TITLE ON MENTION (target_title);

CREATE TABLE ABSTRACT (
    id BIGINT AUTO_INCREMENT,
    title VARCHAR(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    abstract MEDIUMTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    PRIMARY KEY (id)
);

CREATE INDEX ABSTRACT_TITLE_INDEX ON ABSTRACT (TITLE);

/*redirect MEDIUMINT,
    disambiguation MEDIUMINT,
    link MEDIUMINT,
    count MEDIUMINT,*/