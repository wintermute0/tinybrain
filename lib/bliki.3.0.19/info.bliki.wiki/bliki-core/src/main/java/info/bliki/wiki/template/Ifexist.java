package info.bliki.wiki.template;

import info.bliki.wiki.model.IWikiModel;

import java.util.List;

/**
 * A template parser function for <code>{{ #ifexist: ... }}</code> syntax. See
 * <a href="http://www.mediawiki.org/wiki/Help:Extension:ParserFunctions">
 * Mediwiki's Help:Extension:ParserFunctions</a>
 * 
 */
public class Ifexist extends AbstractTemplateFunction {
	public final static ITemplateFunction CONST = new Ifexist();

	public Ifexist() {

	}

	@Override
	public String parseFunction(List<String> list, IWikiModel model, char[] src, int beginIndex, int endIndex, boolean isSubst) {
		if (list.size() > 1) {
			String wikiTopicName = isSubst ? list.get(0) : parseTrim(list.get(0), model);
			int index = wikiTopicName.indexOf(":");
			String namespace = "";
			String templateName = wikiTopicName;
			if (index != -1) {
				namespace = wikiTopicName.substring(0, index);
				if (!model.isNamespace(namespace)) {
					namespace = "";
				} else {
					templateName = wikiTopicName.substring(index + 1);
				}
			}
			if (model.getRawWikiContent(namespace, templateName, null) != null) {
				return isSubst ? list.get(1) : parseTrim(list.get(1), model);
			} else {
				// the requested templateName doesn't exist
				if (list.size() >= 3) {
					return isSubst ? list.get(2) : parseTrim(list.get(2), model);
				}
			}
		}
		return null;
	}
}
