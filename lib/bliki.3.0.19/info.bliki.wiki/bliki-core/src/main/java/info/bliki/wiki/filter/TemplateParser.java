package info.bliki.wiki.filter;

import info.bliki.wiki.model.Configuration;
import info.bliki.wiki.model.IWikiModel;
import info.bliki.wiki.tags.util.WikiTagNode;
import info.bliki.wiki.template.ITemplateFunction;
import info.bliki.wiki.template.Safesubst;
import info.bliki.wiki.template.Subst;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * A template parser for the first pass in the parsing of a Wikipedia text
 * 
 * @see WikipediaParser for the second pass
 */
public class TemplateParser extends AbstractParser {
	private static final Pattern HTML_COMMENT_PATTERN = Pattern.compile("<!--(.*?)-->");

	public final boolean fParseOnlySignature;

	private final boolean fRenderTemplate;

	private boolean fOnlyIncludeFlag;

	public TemplateParser(String stringSource) {
		this(stringSource, false, false);
	}

	public TemplateParser(String stringSource, boolean parseOnlySignature, boolean renderTemplate) {
		this(stringSource, parseOnlySignature, renderTemplate, false);
	}

	public TemplateParser(String stringSource, boolean parseOnlySignature, boolean renderTemplate, boolean onlyIncludeFlag) {
		super(stringSource);
		fParseOnlySignature = parseOnlySignature;
		fRenderTemplate = renderTemplate;
		fOnlyIncludeFlag = onlyIncludeFlag;
	}

	public static void parse(String rawWikitext, IWikiModel wikiModel, Appendable writer, boolean renderTemplate) throws IOException {
		parse(rawWikitext, wikiModel, writer, false, renderTemplate);
	}

	/**
	 * Parse the wiki texts templates, comments and signatures into the given
	 * <code>StringBuilder</code>.
	 * 
	 * @param rawWikitext
	 * @param wikiModel
	 * @param writer
	 * @param parseOnlySignature
	 *          change only the signature string and ignore templates and comments
	 *          parsing
	 * @param renderTemplate
	 */
	public static void parse(String rawWikitext, IWikiModel wikiModel, Appendable writer, boolean parseOnlySignature,
			boolean renderTemplate) throws IOException {
		parseRecursive(rawWikitext, wikiModel, writer, parseOnlySignature, renderTemplate);
	}

	protected static void parseRecursive(String rawWikitext, IWikiModel wikiModel, Appendable writer, boolean parseOnlySignature,
			boolean renderTemplate) throws IOException {
		parseRecursive(rawWikitext, wikiModel, writer, parseOnlySignature, renderTemplate, null);
	}

	private void parsePreprocessRecursive(StringBuilder writer, int diff) throws IOException {
		StringBuilder buf = new StringBuilder(fCurrentPosition - fWhiteStartPosition);
		appendContent(buf, fWhiteStart, fWhiteStartPosition, diff, true);
		parsePreprocessRecursive(buf.toString(), fWikiModel, writer, fParseOnlySignature, fRenderTemplate, false, null);
	}

	public static void parsePreprocessRecursive(String rawWikitext, IWikiModel wikiModel, StringBuilder writer,
			boolean parseOnlySignature, boolean renderTemplate, boolean onlyIncludeFlag, Map<String, String> templateParameterMap)
			throws IOException {
		try {
			// int level = wikiModel.incrementRecursionLevel();
			int templateLevel = wikiModel.incrementTemplateRecursionLevel();
			// if (level > Configuration.PARSER_RECURSION_LIMIT) {
			// if (Configuration.DEBUG) {
			// System.out.println("Recursion1 error: " + rawWikitext);
			// }
			// writer.append("Error - recursion limit exceeded parsing templates.");
			// return;
			// }
			// recursion limit on level is not sufficient as it is possible to recurse
			// indefinitely at fixed level upper bound
			if (templateLevel > Configuration.TEMPLATE_RECURSION_LIMIT) {
				writer.append("Error - template recursion limit exceeded parsing templates.");
				return;
			}
			StringBuilder sb = new StringBuilder(rawWikitext.length());
			TemplateParser parser = new TemplateParser(rawWikitext, parseOnlySignature, renderTemplate, onlyIncludeFlag);
			parser.setModel(wikiModel);
			parser.runPreprocessParser(sb, false);

			StringBuilder parameterBuffer = sb;
			StringBuilder plainBuffer = sb;
			if (templateParameterMap != null && (!templateParameterMap.isEmpty())) {
				String preprocessedContent = parameterBuffer.toString();
				WikipediaScanner scanner = new WikipediaScanner(preprocessedContent);
				scanner.setModel(wikiModel);
				parameterBuffer = scanner.replaceTemplateParameters(preprocessedContent, templateParameterMap);
				if (parameterBuffer != null) {
					plainBuffer = parameterBuffer;
				}
			}
			writer.append(plainBuffer);
		} catch (Exception e) {
			e.printStackTrace();
			writer.append(e.getClass().getSimpleName());
		} catch (Error e) {
			e.printStackTrace();
			writer.append(e.getClass().getSimpleName());
		} finally {
			// wikiModel.decrementRecursionLevel();
			wikiModel.decrementTemplateRecursionLevel();
		}
	}

	public static void parseRecursive(String rawWikitext, IWikiModel wikiModel, Appendable writer, boolean parseOnlySignature,
			boolean renderTemplate, Map<String, String> templateParameterMap) throws IOException {
		StringBuilder sb = new StringBuilder(rawWikitext.length());
		parsePreprocessRecursive(rawWikitext, wikiModel, sb, parseOnlySignature, renderTemplate, false, templateParameterMap);
		if (parseOnlySignature) {
			writer.append(sb);
			return;
		}
		try {
			// int level = wikiModel.incrementRecursionLevel();
			int templateLevel = wikiModel.incrementTemplateRecursionLevel();
			// if (level > Configuration.PARSER_RECURSION_LIMIT) {
			// if (Configuration.DEBUG) {
			// System.out.println("Recursion2 error: " + rawWikitext);
			// }
			// writer.append("Error - recursion limit exceeded parsing templates.");
			// return;
			// }

			// recursion limit on level is not sufficient as it is possible to recurse
			// indefinitely at fixed level upper bound
			if (templateLevel > Configuration.TEMPLATE_RECURSION_LIMIT) {
				writer.append("Error - template recursion limit exceeded parsing templates.");
				return;
			}

			TemplateParser parser = new TemplateParser(sb.toString(), false, renderTemplate);
			parser.setModel(wikiModel);
			sb = new StringBuilder(sb.length());
			parser.runPreprocessParser(sb, true);

			parser = new TemplateParser(sb.toString(), parseOnlySignature, renderTemplate);
			parser.setModel(wikiModel);
			// parser.initialize(plainBuffer.toString());
			sb = new StringBuilder(sb.length());
			parser.runParser(sb);

			if (!renderTemplate) {
				String redirectedLink = AbstractParser.parseRedirect(sb.toString(), wikiModel);
				if (redirectedLink != null) {
					String redirectedContent = AbstractParser.getRedirectedTemplateContent(wikiModel, redirectedLink, null);
					if (redirectedContent != null) {
						parseRecursive(redirectedContent, wikiModel, writer, parseOnlySignature, renderTemplate);
						return;
					}
				}
			}
			writer.append(sb);
		} catch (Exception e) {
			e.printStackTrace();
			writer.append(e.getClass().getSimpleName());
		} catch (Error e) {
			e.printStackTrace();
			writer.append(e.getClass().getSimpleName());
		} finally {
			// wikiModel.decrementRecursionLevel();
			wikiModel.decrementTemplateRecursionLevel();
		}
	}

	/**
	 * Preprocess parsing of the <code>&lt;includeonly&gt;</code>,
	 * <code>&lt;onlyinclude&gt;</code> and <code>&lt;noinclude&gt;</code> tags
	 * 
	 * @param writer
	 * @param ignoreTemplateTags
	 *          TODO
	 * @throws IOException
	 */
	protected void runPreprocessParser(StringBuilder writer, boolean ignoreTemplateTags) throws IOException {
		fWhiteStart = true;
		fWhiteStartPosition = fCurrentPosition;
		int oldPosition;
		try {
			while (true) {
				fCurrentCharacter = fSource[fCurrentPosition++];

				// ---------Identify the next token-------------
				switch (fCurrentCharacter) {
				case '{': // wikipedia subst: and safesubst: handling
					if (fSource[fCurrentPosition] == '{' && (fSource.length > fCurrentPosition + 6) && fSource[fCurrentPosition + 1] == 's') {
						oldPosition = fCurrentPosition;
						if (parseSubstOrSafesubst(writer)) {
							fWhiteStart = true;
							fWhiteStartPosition = fCurrentPosition;
							continue;
						} else {
							fCurrentPosition = oldPosition;
						}
					}
					break;
				case '<':
					int htmlStartPosition = fCurrentPosition;
					if (!fParseOnlySignature && parseIncludeWikiTags(writer, ignoreTemplateTags)) {
						continue;
					}
					fCurrentPosition = htmlStartPosition;
					break;
				case '~':
					int tildeCounter = 0;
					if (fSource[fCurrentPosition] == '~' && fSource[fCurrentPosition + 1] == '~') {
						// parse signatures '~~~', '~~~~' or '~~~~~'
						tildeCounter = 3;
						try {
							if (fSource[fCurrentPosition + 2] == '~') {
								tildeCounter = 4;
								if (fSource[fCurrentPosition + 3] == '~') {
									tildeCounter = 5;
								}
							}
						} catch (IndexOutOfBoundsException e1) {
							// end of scanner text
						}
						appendContent(writer, fWhiteStart, fWhiteStartPosition, 1, true);
						fWikiModel.appendSignature(writer, tildeCounter);
						fCurrentPosition += (tildeCounter - 1);
						fWhiteStart = true;
						fWhiteStartPosition = fCurrentPosition;
					}
				}

				if (!fWhiteStart) {
					fWhiteStart = true;
					fWhiteStartPosition = fCurrentPosition - 1;
				}

			}
			// -----------------end switch while try--------------------
		} catch (IndexOutOfBoundsException e) {
			// end of scanner text
		}
		try {
			if (!fOnlyIncludeFlag) {
				appendContent(writer, fWhiteStart, fWhiteStartPosition, 1, true);
			}
		} catch (IndexOutOfBoundsException e) {
			// end of scanner text
		}
	}

	protected void runParser(Appendable writer) throws IOException {
		fWhiteStart = true;
		fWhiteStartPosition = fCurrentPosition;
		try {
			while (true) {
				fCurrentCharacter = fSource[fCurrentPosition++];

				// ---------Identify the next token-------------
				switch (fCurrentCharacter) {
				case '{': // wikipedia template handling
					// if (Configuration.TEMPLATE_NAMES) {
					// int level = fWikiModel.getRecursionLevel();
					// if (level == 1) {
					// try {
					// String temp = fStringSource.substring(fCurrentPosition,
					// fCurrentPosition + 10);
					// System.out.println("==>" + temp);
					// } catch (Exception ex) {
					//
					// }
					// }
					// }
					if (!fParseOnlySignature && parseTemplateOrTemplateParameter(writer)) {
						fWhiteStart = true;
						fWhiteStartPosition = fCurrentPosition;
						continue;
					}
					break;
				case '<':
					int htmlStartPosition = fCurrentPosition;
					if (!fParseOnlySignature && parseSpecialWikiTags(writer)) {
						continue;
					}
					fCurrentPosition = htmlStartPosition;
					break;
				case '~':
					int tildeCounter = 0;
					if (fSource[fCurrentPosition] == '~' && fSource[fCurrentPosition + 1] == '~') {
						// parse signatures '~~~', '~~~~' or '~~~~~'
						tildeCounter = 3;
						try {
							if (fSource[fCurrentPosition + 2] == '~') {
								tildeCounter = 4;
								if (fSource[fCurrentPosition + 3] == '~') {
									tildeCounter = 5;
								}
							}
						} catch (IndexOutOfBoundsException e1) {
							// end of scanner text
						}
						appendContent(writer, fWhiteStart, fWhiteStartPosition, 1, true);
						fWikiModel.appendSignature(writer, tildeCounter);
						fCurrentPosition += (tildeCounter - 1);
						fWhiteStart = true;
						fWhiteStartPosition = fCurrentPosition;
					}
				}

				if (!fWhiteStart) {
					fWhiteStart = true;
					fWhiteStartPosition = fCurrentPosition - 1;
				}

			}
			// -----------------end switch while try--------------------
		} catch (IndexOutOfBoundsException e) {
			// end of scanner text
		}
		try {
			appendContent(writer, fWhiteStart, fWhiteStartPosition, 1, true);
		} catch (IndexOutOfBoundsException e) {
			// end of scanner text
		}
	}

	/**
	 * See <a href=
	 * "http://en.wikipedia.org/wiki/Help:Template#Controlling_what_gets_transcluded"
	 * >Help:Template#Controlling what gets transcluded</a>
	 * 
	 * @param writer
	 * @param ignoreTemplateTags
	 *          TODO
	 * @return
	 * @throws IOException
	 */
	protected boolean parseIncludeWikiTags(StringBuilder writer, boolean ignoreTemplateTags) throws IOException {
		try {
			switch (fSource[fCurrentPosition]) {
			case '!': // <!-- html comment -->
				if (parseHTMLCommentTags(writer)) {
					return true;
				}
				break;
			default:

				if (fSource[fCurrentPosition] != '/') {
					// starting tag
					int lessThanStart = fCurrentPosition - 1;
					int startPosition = fCurrentPosition;
					int diff = 0;
					WikiTagNode tagNode = parseTag(fCurrentPosition);
					if (tagNode != null && !tagNode.isEmptyXmlTag()) {
						fCurrentPosition = tagNode.getEndPosition();
						int tagStart = fCurrentPosition;
						String tagName = tagNode.getTagName();
						if (tagName.equals("nowiki")) {
							readUntilIgnoreCase("</", "nowiki>");
							return true;
						} else if (tagName.equals("source")) {
							readUntilIgnoreCase("</", "source>");
							appendContentWithComment(writer, startPosition);
							return true;
						} else if (tagName.equals("math")) {
							readUntilIgnoreCase("</", "math>");
							return true;
						}
						if (ignoreTemplateTags) {
							return false;
						}
						if (!isTemplate()) {
							// not rendering a Template namespace directly
							if (tagName.equals("includeonly")) {
								diff = readUntilNestedIgnoreCase("includeonly>");
								if (!fOnlyIncludeFlag) {
									appendContent(writer, fWhiteStart, fWhiteStartPosition, fCurrentPosition - lessThanStart, true);
								}
								fWhiteStart = true;
								fWhiteStartPosition = tagStart;

								if (!fOnlyIncludeFlag) {
									parsePreprocessRecursive(writer, diff);
								}
								fWhiteStart = true;
								fWhiteStartPosition = fCurrentPosition;
								return true;

							} else if (tagName.equals("noinclude")) {
								diff = readUntilNestedIgnoreCase("noinclude>");
								if (!fOnlyIncludeFlag) {
									appendContent(writer, fWhiteStart, fWhiteStartPosition, fCurrentPosition - lessThanStart, true);
								}
								fWhiteStart = true;
								fWhiteStartPosition = fCurrentPosition;
								return true;
							} else if (tagName.equals("onlyinclude")) {
								diff = readUntilNestedIgnoreCase("onlyinclude>");
								if (!fOnlyIncludeFlag) {
									// delete the content, which is already added
									writer.delete(0, writer.length());
									fOnlyIncludeFlag = true;
								}

								fWhiteStart = true;
								fWhiteStartPosition = tagStart;

								parsePreprocessRecursive(writer, diff);
								fWhiteStart = true;
								fWhiteStartPosition = fCurrentPosition;
								return true;
							}
						} else {
							if (tagName.equals("noinclude")) {
								diff = readUntilNestedIgnoreCase("noinclude>");
								appendContent(writer, fWhiteStart, fWhiteStartPosition, fCurrentPosition - lessThanStart, true);
								fWhiteStart = true;
								fWhiteStartPosition = tagStart;

								parsePreprocessRecursive(writer, diff);
								fWhiteStart = true;
								fWhiteStartPosition = fCurrentPosition;
								return true;
							} else if (tagName.equals("includeonly")) {
								readUntilNestedIgnoreCase("includeonly>");
								appendContent(writer, fWhiteStart, fWhiteStartPosition, fCurrentPosition - lessThanStart, true);
								fWhiteStart = true;
								fWhiteStartPosition = fCurrentPosition;
								return true;
							} else if (tagName.equals("onlyinclude")) {
								diff = readUntilNestedIgnoreCase("onlyinclude>");
								appendContent(writer, fWhiteStart, fWhiteStartPosition, fCurrentPosition - lessThanStart, true);
								fWhiteStart = true;
								fWhiteStartPosition = tagStart;
								parsePreprocessRecursive(writer, diff);
								fWhiteStart = true;
								fWhiteStartPosition = fCurrentPosition;
								return true;
							}
						}
						fCurrentPosition = startPosition;
					}
				}
			}
		} catch (IndexOutOfBoundsException e) {
			// do nothing
		}
		return false;
	}

	protected boolean parseSpecialWikiTags(Appendable writer) throws IOException {
		try {
			switch (fSource[fCurrentPosition]) {
			case '!': // <!-- html comment -->
				if (parseHTMLCommentTags(writer)) {
					return true;
				}
				break;
			default:

				if (fSource[fCurrentPosition] != '/') {
					int startPosition = fCurrentPosition;
					// starting tag
					WikiTagNode tagNode = parseTag(fCurrentPosition);
					if (tagNode != null && !tagNode.isEmptyXmlTag()) {
						fCurrentPosition = tagNode.getEndPosition();
						String tagName = tagNode.getTagName();
						if (tagName.equals("nowiki")) {
							readUntilIgnoreCase("</", "nowiki>");
							return true;

						} else if (tagName.equals("source")) {
							readUntilIgnoreCase("</", "source>");
							appendContentWithComment(writer, startPosition);
							return true;

						} else if (tagName.equals("math")) {
							readUntilIgnoreCase("</", "math>");
							return true;
						}
					}
					fCurrentPosition = startPosition;
				}
			}
		} catch (IndexOutOfBoundsException e) {
			// do nothing
		}
		return false;
	}

	protected void appendContent(Appendable writer, boolean whiteStart, final int whiteStartPosition, final int diff,
			boolean stripHTMLComments) throws IOException {
		if (whiteStart) {
			try {
				final int whiteEndPosition = fCurrentPosition - diff;
				int count = whiteEndPosition - whiteStartPosition;
				if (count > 0) {
					if (stripHTMLComments) {
						writer.append(HTML_COMMENT_PATTERN.matcher(fStringSource.substring(whiteStartPosition, whiteEndPosition))
								.replaceAll(""));
					} else {
						writer.append(fStringSource, whiteStartPosition, whiteEndPosition);
					}
				}
			} finally {
				fWhiteStart = false;
			}
		}
	}

	private void appendContentWithComment(Appendable writer, int startPosition) throws IOException {
		if (fWhiteStartPosition < startPosition - 1) {
			appendContent(writer, fWhiteStart, fWhiteStartPosition, fCurrentPosition - startPosition + 1, true);
		}
		appendContent(writer, true, startPosition - 1, 0, false);
		fWhiteStart = true;
		fWhiteStartPosition = fCurrentPosition;
	}

	private boolean parseSubstOrSafesubst(Appendable writer) throws IOException {
		appendContent(writer, fWhiteStart, fWhiteStartPosition, 1, true);
		int startTemplatePosition = ++fCurrentPosition;

		int templateEndPosition = findNestedTemplateEnd(fSource, fCurrentPosition);
		if (templateEndPosition < 0) {
			fCurrentPosition--;
		} else {
			return parseSubst(writer, startTemplatePosition, templateEndPosition);
		}

		return false;
	}

	private boolean parseTemplateOrTemplateParameter(Appendable writer) throws IOException {
		if (fSource[fCurrentPosition] == '{') {
			appendContent(writer, fWhiteStart, fWhiteStartPosition, 1, true);
			int startTemplatePosition = ++fCurrentPosition;
			if (fSource[fCurrentPosition] == '{' && fSource[fCurrentPosition + 1] != '{') {
				// parse template parameters
				int[] templateEndPosition = findNestedParamEnd(fSource, fCurrentPosition + 1);
				if (templateEndPosition[0] < 0) {
					if (templateEndPosition[1] < 0) {
						--fCurrentPosition;
					} else {
						writer.append('{');
						++fCurrentPosition;
						return parseTemplate(writer, startTemplatePosition + 1, templateEndPosition[1]);
					}
				} else {
					return parseTemplateParameter(writer, startTemplatePosition, templateEndPosition[0]);
				}
			} else {
				int templateEndPosition = findNestedTemplateEnd(fSource, fCurrentPosition);
				if (templateEndPosition < 0) {
					fCurrentPosition--;
				} else {
					return parseTemplate(writer, startTemplatePosition, templateEndPosition);
				}
			}
		}
		return false;
	}

	private boolean parseSubst(Appendable writer, int startTemplatePosition, int templateEndPosition) throws IOException {
		fCurrentPosition = templateEndPosition;
		// insert template handling
		int endPosition = fCurrentPosition;
		String plainContent = null;
		int endOffset = fCurrentPosition - 2;
		Object[] objs = createParameterMap(fSource, startTemplatePosition, fCurrentPosition - startTemplatePosition - 2);
		String templateName = ((String) objs[1]);
		List<String> parts = (List<String>) objs[0];
		ITemplateFunction templateFunction = null;
		int currOffset = 0;
		if (templateName.startsWith("subst:")) {
			templateFunction = Subst.CONST;
			currOffset = 6;
		} else if (templateName.startsWith("safesubst:")) {
			templateFunction = Safesubst.CONST;
			currOffset = 10;
		} else {
			return false;
		}

		parts.set(0, templateName.substring(currOffset));
		plainContent = templateFunction.parseFunction(parts, fWikiModel, fSource, startTemplatePosition + currOffset, endOffset, false);
		fCurrentPosition = endPosition;
		if (plainContent != null) {
			writer.append(plainContent);
			return true;
		}
		return false;

	}

	/**
	 * Parse a single template call {{...}}. There are 3 main steps:
	 * <ol>
	 * <li>Check if the call is a parser function in the
	 * <code>checkParserFunction()</code> method; if <code>true</code> execute the
	 * parser function and return</li>
	 * <li>Split the template call in the <code>createParameterMap()</code method
	 * into a <code>templateName</code> and a parameter/value map.</li>
	 * <li>Substitute the raw template text into th existing text and replace all
	 * template parameters with their value in
	 * <code>TemplateParser.parseRecursive()</code method.</li>
	 * </ol>
	 * 
	 * @param writer
	 * @param startTemplatePosition
	 * @param templateEndPosition
	 * @return
	 * @throws IOException
	 */
	private boolean parseTemplate(Appendable writer, int startTemplatePosition, int templateEndPosition) throws IOException {
		fCurrentPosition = templateEndPosition;

		int endPosition = fCurrentPosition;
		String plainContent = null;
		int endOffset = fCurrentPosition - 2;
		Object[] objs = createParameterMap(fSource, startTemplatePosition, fCurrentPosition - startTemplatePosition - 2);
		List<String> parts = (List<String>) objs[0];
		String templateName = ((String) objs[1]);
		StringBuilder buf = new StringBuilder((templateName.length()) + (templateName.length() / 10));
		TemplateParser.parse(templateName, fWikiModel, buf, false);
		templateName = buf.toString();
		int currOffset = TemplateParser.checkParserFunction(templateName);
		if (currOffset > 0) {
			String function = templateName.substring(0, currOffset - 1).trim();
			if (function != null) {
				if (Configuration.PARSER_FUNCTIONS) {
					System.out.println(function);
				}
				ITemplateFunction templateFunction = fWikiModel.getTemplateFunction(function);
				if (templateFunction != null) {
					// if (function.charAt(0) == '#') {
					// #if:, #ifeq:,...
					parts.set(0, templateName.substring(currOffset));
					// if (Configuration.PARSER_FUNCTIONS) {
					// System.out.println(function + ": " + parts);
					// }
					plainContent = templateFunction.parseFunction(parts, fWikiModel, fSource, startTemplatePosition + currOffset, endOffset,
							false);
					fCurrentPosition = endPosition;
					if (plainContent != null) {
						TemplateParser.parseRecursive(plainContent, fWikiModel, writer, false, false);
						return true;
					}
					return true;
				}
				fCurrentPosition = endOffset + 2;
			}
		}
		fCurrentPosition = endPosition;
		LinkedHashMap<String, String> parameterMap = new LinkedHashMap<String, String>();
		List<String> unnamedParameters = new ArrayList<String>();
		for (int i = 1; i < parts.size(); i++) {
			String temp = parts.get(i);
			if (i == parts.size() - 1) {
				createSingleParameter(temp, parameterMap, unnamedParameters);
			} else {
				createSingleParameter(temp, parameterMap, unnamedParameters);
			}
		}
		mergeParameters(parameterMap, unnamedParameters);

		fWikiModel.substituteTemplateCall(templateName, parameterMap, writer);
		return true;
	}

	/**
	 * If template calls have a mix between named and unnamed parameters, the
	 * collected <code>unnamedParameters</code> are merge into the
	 * <code>parameterMap</code>.
	 * 
	 * 
	 * See <a href="http://meta.wikimedia.org/wiki/Help:Template#Mix_of_named_and_unnamed_parameters"
	 * >Help:Template#Mix_of_named_and_unnamed_parameters</a>
	 * 
	 * @param parameterMap
	 * @param unnamedParameters
	 */
	public static void mergeParameters(LinkedHashMap<String, String> parameterMap, List<String> unnamedParameters) {
		int unnamedParameterIndex = 1;
		for (String param : unnamedParameters) {
			String key = Integer.toString(unnamedParameterIndex++);
			if (!parameterMap.containsKey(key))
				parameterMap.put(key, param);
		}
	}

	/**
	 * Parse a single template parameter {{{...}}}
	 * 
	 * @param writer
	 * @param startTemplatePosition
	 * @param templateEndPosition
	 * @return
	 * @throws IOException
	 */
	private boolean parseTemplateParameter(Appendable writer, int startTemplatePosition, int templateEndPosition) throws IOException {
		String plainContent = fStringSource.substring(startTemplatePosition - 2, templateEndPosition);

		if (plainContent != null) {
			fCurrentPosition = templateEndPosition;
			WikipediaScanner scanner = new WikipediaScanner(plainContent);
			scanner.setModel(fWikiModel);
			StringBuilder plainBuffer = scanner.replaceTemplateParameters(plainContent, null);
			if (plainBuffer == null) {
				writer.append(plainContent);
				return true;
			}
			TemplateParser.parseRecursive(plainBuffer.toString().trim(), fWikiModel, writer, false, false);
			return true;
		}
		return false;
	}

	/**
	 * Create a map from the parameters defined in a template call
	 * 
	 * @return the templates parameters <code>java.util.List</code> at index [0];
	 *         the template name at index [1]
	 * 
	 */
	public static Object[] createParameterMap(char[] src, int startOffset, int len) {
		Object[] objs = new Object[2];
		int currOffset = startOffset;
		int endOffset = startOffset + len;
		List<String> resultList = new ArrayList<String>();
		objs[0] = resultList;
		resultList = splitByPipe(src, currOffset, endOffset, resultList);
		if (resultList.size() <= 1) {
			// set the template name
			objs[1] = new String(src, startOffset, len).trim();
		} else {
			objs[1] = resultList.get(0).trim();
		}
		return objs;
	}

	/**
	 * Create a single parameter, defined in a template call, and add it to the
	 * <b>named parmeters map</b> or <b>unnamed parameter list</b>
	 * 
	 * <p>
	 * See <a href="http://en.wikipedia.org/wiki/Help:Template">Help:Template</a>:
	 * <i>Remember that whitespace characters (spaces, tabs, carriage returns and
	 * line feeds) are not automatically stripped from the start and end of
	 * unnamed parameters (as they are from named parameters). Including such
	 * characters (or any other non-visible characters in any parameters) may in
	 * some cases affect the template's behaviour in unexpected ways. (Template
	 * designers can use {{StripWhitespace}} to remove unwanted whitespace in
	 * unnamed parameters.)</i>
	 * </p>
	 * 
	 * @param srcString
	 * @param namedParameterMap
	 *          a key/value pairs for name and value of a template parameter
	 * @param unnamedParams
	 *          a list of unnamed parameter values
	 */
	public static void createSingleParameter(String srcString, Map<String, String> namedParameterMap, List<String> unnamedParams) {
		int currOffset = 0;
		char[] src = srcString.toCharArray();
		int endOffset = srcString.length();
		char ch;
		String parameter = null;
		String value;
		boolean equalCharParsed = false;

		int lastOffset = currOffset;
		int[] temp = new int[] { -1, -1 };
		try {
			while (currOffset < endOffset) {
				ch = src[currOffset++];
				if (ch == '[' && src[currOffset] == '[') {
					currOffset++;
					temp[0] = findNestedEnd(src, '[', ']', currOffset);
					if (temp[0] >= 0) {
						currOffset = temp[0];
					}
				} else if (ch == '{' && src[currOffset] == '{') {
					currOffset++;
					if (src[currOffset] == '{' && src[currOffset + 1] != '{') {
						currOffset++;
						temp = findNestedParamEnd(src, currOffset);
						if (temp[0] >= 0) {
							currOffset = temp[0];
						} else {
							if (temp[1] >= 0) {
								currOffset = temp[1];
							}
						}
					} else {
						temp[0] = findNestedTemplateEnd(src, currOffset);
						if (temp[0] >= 0) {
							currOffset = temp[0];
						}
					}

				} else if (ch == '=') {
					if (!equalCharParsed) {
						parameter = srcString.substring(lastOffset, currOffset - 1).trim();
						lastOffset = currOffset;
					}
					equalCharParsed = true;
				}
			}

		} catch (IndexOutOfBoundsException e) {

		} finally {
			if (currOffset > lastOffset) {
				value = srcString.substring(lastOffset, currOffset);
				if (parameter != null) {
					value = Util.trimNewlineRight(value);
					namedParameterMap.put(parameter, value);
				} else {
					// whitespace characters are not automatically stripped from the start
					// and end of unnamed parameters!
					unnamedParams.add(value);
				}
			}
		}
	}

	

	/**
	 * Check if this template contains a template function
	 * 
	 * Note: repositions this#fCurrentPosition behind the parser function string
	 * if possible
	 * 
	 * @param plainContent
	 * @return the offset behind the &acute;:&acute; character at the end of the
	 *         parser function name or <code>-1</code> if no parser function can
	 *         be found in this template.
	 */
	public static int checkParserFunction(String plainContent) {
		int currOffset = 0;
		int len = plainContent.length();
		char ch;
		while (currOffset < len) {
			ch = plainContent.charAt(currOffset++);
			if (Character.isLetter(ch) || ch == '#' || ch == '$') {
				while (currOffset < len) {
					ch = plainContent.charAt(currOffset++);
					if (ch == ':') {
						return currOffset;
					} else if (!Character.isLetterOrDigit(ch) && ch != '$') {
						return -1;
					}
				}
				break;
			} else if (!Character.isWhitespace(ch)) {
				return -1;
			}
		}
		return -1;
	}

	protected boolean parseHTMLCommentTags(Appendable writer) throws IOException {
		int temp = readWhitespaceUntilStartOfLine(2);
		String htmlCommentString = fStringSource.substring(fCurrentPosition - 1, fCurrentPosition + 3);
		if (htmlCommentString.equals("<!--")) {
			if (temp >= 0) {
				if (!fOnlyIncludeFlag) {
					appendContent(writer, fWhiteStart, fWhiteStartPosition, fCurrentPosition - temp - 1, true);
				}
			} else {
				if (!fOnlyIncludeFlag) {
					appendContent(writer, fWhiteStart, fWhiteStartPosition, 1, true);
				}
			}
			fCurrentPosition += 3;
			if (readUntil("-->")) {
				if (temp >= 0) {
					temp = readWhitespaceUntilEndOfLine(0);
					if (temp >= 0) {
						fCurrentPosition++;
					}
				}
				fWhiteStart = true;
				fWhiteStartPosition = fCurrentPosition;
				return true;
			}
		}
		return false;
	}

	@Override
	public void runParser() {
		// do nothing here
	}

	@Override
	public void setNoToC(boolean noToC) {
		// do nothing here
	}

	public boolean isTemplate() {
		return fRenderTemplate;
	}

}
