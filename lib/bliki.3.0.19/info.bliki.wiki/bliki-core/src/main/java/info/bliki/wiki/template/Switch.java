package info.bliki.wiki.template;

import info.bliki.wiki.model.IWikiModel;

import java.util.List;

/**
 * A template parser function for <code>{{ #switch: ... }}</code> syntax.
 * 
 * See <a href
 * ="http://www.mediawiki.org/wiki/Help:Extension:ParserFunctions">Mediwiki's
 * Help:Extension:ParserFunctions</a>
 * 
 */
public class Switch extends AbstractTemplateFunction {
	public final static ITemplateFunction CONST = new Switch();

	public Switch() {

	}

	@Override
	public String parseFunction(List<String> list, IWikiModel model, char[] src, int beginIndex, int endIndex, boolean isSubst) {
		if (list.size() > 2) {
			String defaultResult = null;
			String conditionString = isSubst ? list.get(0) : parseTrim(list.get(0), model);
			boolean checkNumerically = false;
			if (conditionString.length() > 0) {
				if (conditionString.charAt(0) == '+' || 
						conditionString.charAt(0) == '-' ||  
						conditionString.charAt(0) == '0' ||
						conditionString.charAt(0) == '.' || 
						conditionString.charAt(0) == ',' || 
						conditionString.charAt(0) == 'e' || 
						conditionString.charAt(0) == 'E') {
					checkNumerically = true;
				} else {
					for (int i = 0; i < conditionString.length(); i++) {
						if (conditionString.charAt(i) == '+' || conditionString.charAt(i) == '-' || conditionString.charAt(i) == '.'
								|| conditionString.charAt(i) == ',' || conditionString.charAt(i) == 'e' || conditionString.charAt(i) == 'E') {
							checkNumerically = true;
							break;
						}
					}
				}
			}
			boolean valueFound = false;
			for (int i = 1; i < list.size(); i++) {
				String temp = isSubst ? list.get(i) : parseTrim(list.get(i), model);
				int index = temp.indexOf('=');
				String leftHandSide;
				if (index >= 0) {
					if (valueFound) {
						return temp.substring(index + 1).trim();
					}
					leftHandSide = temp.substring(0, index);
				} else {
					leftHandSide = temp;
				}
				String parsedLHS = isSubst ? leftHandSide : parseTrim(leftHandSide, model);
				if (index >= 0 && "#default".equals(parsedLHS)) {
					defaultResult = temp.substring(index + 1).trim();
					continue;
				}
				if (index < 0 && i == list.size() - 1) {
					return parsedLHS;
				}
				if (equalsTypes(conditionString, parsedLHS, checkNumerically)) {
					if (index >= 0) {
						return temp.substring(index + 1).trim();
					} else {
						valueFound = true;
					}
				}

			}
			return defaultResult;
		}
		return null;
	}

	private boolean equalsTypes(String first, String second, boolean checkNumerically) {

		boolean result = false;
		if (first.length() == 0) {
			return second.length() == 0;
		}
		if (second.length() == 0) {
			return first.length() == 0;
		}
		if (first.charAt(0) == '+') {
			first = first.substring(1);
		}
		if (second.charAt(0) == '+') {
			second = second.substring(1);
		}

		if (checkNumerically) {
			try {
				double d1 = Double.parseDouble(first);
				double d2 = Double.parseDouble(second);
				if (d1 == d2) {
					return true;
				}
			} catch (NumberFormatException e) {
			}
		}

		return first.equals(second);
	}
}
