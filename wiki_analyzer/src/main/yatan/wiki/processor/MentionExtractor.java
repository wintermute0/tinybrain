package yatan.wiki.processor;

import java.util.ArrayList;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import yatan.wiki.entities.Article;
import yatan.wiki.entities.Mention;

public class MentionExtractor {
    private static final String NO_WIKI_START = "<nowiki>";
    private static final String NO_WIKI_END = "</nowiki>";
    private static final Pattern PATTERN = Pattern.compile("\\[\\[([^:]+?)\\]\\]([a-zA-Z0-9]*)");
    private static final Pattern LINK_PATTERN_SIMPLE = Pattern.compile("(.+?)\\|(.+)");
    private static final Pattern LINK_PATTERN_BRACKET = Pattern.compile("(.+)\\((.+)\\)\\|");
    private static final Pattern LINK_PATTERN_COMMA = Pattern.compile("(.+),(.+)\\|");

    public List<Mention> extract(Article article) {
        List<Mention> mentions;

        if (isRedirect(article)) {
            mentions = extractLinks(article, true, false);
        } else if (isDisambiguation(article)) {
            mentions = extractLinks(article, false, true);
        } else {
            mentions = extractLinks(article, false, false);

            // construct mention for the title of this page
            Mention mention = parseMention(article, article.getTitle(), null);
            mention.setMentionType(Mention.Type.TITLE);
            mentions.add(mention);
        }

        return mentions;
    }

    private boolean isRedirect(Article article) {
        return (article.getText().toLowerCase().startsWith("#redirect [["));
    }

    private boolean isDisambiguation(Article article) {
        return (article.getTitle().endsWith("(disambiguation)"));
    }

    private List<Mention> extractLinks(Article article, boolean redirect, boolean disambiguation) {
        List<Mention> mentions = new ArrayList<Mention>();

        String text = removeNowikiTag(article.getText());
        Matcher matcher = PATTERN.matcher(text);
        while (matcher.find()) {
            String mentionText = matcher.group(1).trim().replaceAll("\r", " ").replaceAll("\n", " ");
            String mentionSuffix = matcher.group(2).trim();
            if (mentionSuffix.trim().isEmpty()) {
                mentionSuffix = null;
            }

            Mention mention;
            if (redirect) {
                mention = parseMention(article, mentionText, mentionSuffix);
                if (mention != null) {
                    mention.setMention(article.getTitle());
                    mention.setMentionType(Mention.Type.REDIRECT);
                }
            } else if (disambiguation) {
                mention = parseMention(article, mentionText, mentionSuffix);
                if (mention != null) {
                    mention.setMention(article.getTitle().substring(0,
                            article.getTitle().length() - " (disambiguation)".length()));
                    mention.setMentionType(Mention.Type.DISAMBIGUATION);
                }
            } else if ((!mentionText.contains("|") || (!mentionText.contains(",") && !mentionText.contains("(")))
                    && mentionSuffix == null) {
                // if the page is neither redirect nor disambiguation, then we ignore mention use the same text as the
                // title of the target page
                continue;
            } else {
                mention = parseMention(article, mentionText, mentionSuffix);
            }

            // ignore mention longer than 500
            if (mention == null || mention.getMention().length() > 500 || mention.getTargetTitle().length() > 512) {
                continue;
            }
            mentions.add(mention);
        }

        return mentions;
    }

    private Mention parseMention(Article article, String mentionString, String suffix) {
        // convert simple mention to the structure xxxx|xxxx
        if (!mentionString.contains("|")) {
            mentionString += "|" + mentionString;
        }

        // deal with some special link grammar, refer to http://meta.wikimedia.org/wiki/Help:Link
        Matcher matcher = LINK_PATTERN_BRACKET.matcher(mentionString);
        if (matcher.find()) {
            mentionString += matcher.group(1);
        } else {
            matcher = LINK_PATTERN_COMMA.matcher(mentionString);
            if (matcher.find()) {
                mentionString += matcher.group(1);
            }
        }

        // deal with suffix, refer to http://meta.wikimedia.org/wiki/Help:Link
        if (suffix != null) {
            mentionString += suffix;
        }

        // now the mention text should be a normal one
        matcher = LINK_PATTERN_SIMPLE.matcher(mentionString);
        if (!matcher.find()) {
            // FIMXE: log error here
            System.out.println(mentionString);
            return null;
        }

        Mention mention = new Mention();
        mention.setArticleId(article.getId());
        mention.setMentionType(Mention.Type.LINK);
        mention.setMention(matcher.group(2));
        mention.setTargetTitle(matcher.group(1));

        return mention;
    }

    private String removeNowikiTag(String text) {
        StringBuilder sb = new StringBuilder();
        int index = 0;
        int tagIndex = text.indexOf(NO_WIKI_START);
        while (tagIndex != -1) {
            sb.append(text.substring(index, tagIndex));
            index = text.indexOf(NO_WIKI_END, tagIndex);
            if (index != -1) {
                index += NO_WIKI_END.length();
            } else {
                index = text.length() - 1;
            }
            tagIndex = text.indexOf(NO_WIKI_START, index);
        }
        sb.append(text.substring(index));

        return sb.toString();
    }
}
