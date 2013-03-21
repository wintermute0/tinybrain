package info.bliki.wiki.model;

import info.bliki.Messages;
import info.bliki.htmlcleaner.BaseToken;
import info.bliki.htmlcleaner.ContentToken;
import info.bliki.htmlcleaner.TagNode;
import info.bliki.htmlcleaner.TagToken;
import info.bliki.htmlcleaner.Utils;
import info.bliki.wiki.filter.AbstractParser;
import info.bliki.wiki.filter.Encoder;
import info.bliki.wiki.filter.HTMLConverter;
import info.bliki.wiki.filter.ITextConverter;
import info.bliki.wiki.filter.MagicWord;
import info.bliki.wiki.filter.PDFConverter;
import info.bliki.wiki.filter.SectionHeader;
import info.bliki.wiki.filter.TemplateParser;
import info.bliki.wiki.filter.WikipediaParser;
import info.bliki.wiki.namespaces.INamespace;
import info.bliki.wiki.namespaces.Namespace;
import info.bliki.wiki.tags.TableOfContentTag;
import info.bliki.wiki.tags.WPATag;
import info.bliki.wiki.tags.WPTag;
import info.bliki.wiki.tags.code.SourceCodeFormatter;
import info.bliki.wiki.tags.util.TagStack;
import info.bliki.wiki.template.AbstractTemplateFunction;
import info.bliki.wiki.template.ITemplateFunction;
import info.bliki.wiki.template.extension.AttributeList;
import info.bliki.wiki.template.extension.AttributeRenderer;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TimeZone;
import java.util.Map.Entry;

/**
 * Standard model implementation for the Wikipedia syntax
 * 
 */
public abstract class AbstractWikiModel implements IWikiModel, IContext {
	private static int fNextNumberCounter = 0;

	protected ArrayList<Reference> fReferences;

	protected Map<String, Integer> fReferenceNames;

	protected int fRecursionLevel;

	protected int fTemplateRecursionCount;

	protected TagStack fTagStack;

	private boolean fInitialized;

	protected Locale fLocale;

	private IConfiguration fConfiguration;

	private IEventListener fWikiListener = null;

	protected INamespace fNamespace;

	// private ResourceBundle fResourceBundle;

	protected String fRedirectLink = null;

	protected String fPageTitle = "PAGENAME";

	protected String fNamespaceName = "";

	protected int fSectionCounter;

	protected boolean fTemplateTopic = false;

	protected int fExternalLinksCounter;

	/**
	 * A tag that manages the &quot;table of content&quot;
	 * 
	 */
	protected TableOfContentTag fTableOfContentTag = null;

	protected SimpleDateFormat fFormatter = null;

	/**
	 * &quot;table of content&quot;
	 * 
	 */
	protected List<Object> fTableOfContent = null;

	/**
	 * Contains all anchor strings to create unique anchors
	 */
	protected HashSet<String> fToCSet;

	/**
	 * Map an attribute name to its value(s). These values are set by outside code
	 * via st.setAttribute(name, value). StringTemplate is like self in that a
	 * template is both the "class def" and "instance". When you create a
	 * StringTemplate or setTemplate, the text is broken up into chunks (i.e.,
	 * compiled down into a series of chunks that can be evaluated later). You can
	 * have multiple
	 */
	protected Map<String, Object> attributes;

	/**
	 * A Map<Class,Object> that allows people to register a renderer for a
	 * particular kind of object to be displayed in this template. This overrides
	 * any renderer set for this template's group.
	 * 
	 * Most of the time this map is not used because the StringTemplateGroup has
	 * the general renderer map for all templates in that group. Sometimes though
	 * you want to override the group's renderers.
	 */
	protected Map<Class, Object> attributeRenderers;

	public AbstractWikiModel() {
		this(Configuration.DEFAULT_CONFIGURATION);
	}

	public AbstractWikiModel(Configuration configuration) {
		this(configuration, Locale.ENGLISH);
	}

	public AbstractWikiModel(Configuration configuration, Locale locale) {
		this(configuration, locale, Messages.getResourceBundle(locale), new Namespace(locale));
	}

	/**
	 * 
	 * @deprecated use the
	 *             <code>(Configuration configuration, Locale locale,...)</code>
	 *             constructors instead.
	 * @param configuration
	 * @param resourceBundle
	 * @param namespace
	 */
	@Deprecated
	public AbstractWikiModel(Configuration configuration, ResourceBundle resourceBundle, INamespace namespace) {
		this(configuration, Locale.ENGLISH, resourceBundle, namespace);
	}

	public AbstractWikiModel(Configuration configuration, Locale locale, ResourceBundle resourceBundle, INamespace namespace) {
		fLocale = locale;
		fInitialized = false;
		fConfiguration = configuration;
		// fResourceBundle = resourceBundle;
		fNamespace = namespace;
		// initializeNamespaces();
		initialize();
	}

	// private void initializeNamespaces() {
	// String ns1, ns2;
	//
	// ns1 = Messages.getString(fResourceBundle, Messages.WIKI_API_MEDIA1);
	// if (ns1 != null) {
	// fImageNamespaces[0] = ns1;
	// ns2 = Messages.getString(fResourceBundle, Messages.WIKI_API_MEDIA2);
	// if (ns2 != null) {
	// fImageNamespaces[1] = ns2;
	// }
	// }
	//
	// ns1 = Messages.getString(fResourceBundle, Messages.WIKI_API_IMAGE1);
	// if (ns1 != null) {
	// fImageNamespaces[0] = ns1;
	// ns2 = Messages.getString(fResourceBundle, Messages.WIKI_API_IMAGE2);
	// if (ns2 != null) {
	// fImageNamespaces[1] = ns2;
	// }
	// }
	//
	// ns1 = Messages.getString(fResourceBundle, Messages.WIKI_API_TEMPLATE1);
	// if (ns1 != null) {
	// fTemplateNamespaces[0] = ns1;
	// ns2 = Messages.getString(fResourceBundle, Messages.WIKI_API_TEMPLATE2);
	// if (ns2 != null) {
	// fTemplateNamespaces[1] = ns2;
	// }
	// }
	//
	// ns1 = Messages.getString(fResourceBundle, Messages.WIKI_API_CATEGORY1);
	// if (ns1 != null) {
	// fCategoryNamespaces[0] = ns1;
	// ns2 = Messages.getString(fResourceBundle, Messages.WIKI_API_CATEGORY2);
	// if (ns2 != null) {
	// fCategoryNamespaces[1] = ns2;
	// }
	// }
	//
	// }

	/**
	 * {@inheritDoc}
	 */
	public void addCategory(String categoryName, String sortKey) {

	}

	/**
	 * {@inheritDoc}
	 */
	public SourceCodeFormatter addCodeFormatter(String key, SourceCodeFormatter value) {
		return fConfiguration.addCodeFormatter(key, value);
	}

	/**
	 * {@inheritDoc}
	 */
	public String addInterwikiLink(String key, String value) {
		return fConfiguration.addInterwikiLink(key, value);
	}

	/**
	 * {@inheritDoc}
	 */
	public void addLink(String topicName) {

	}

	/**
	 * {@inheritDoc}
	 */
	public boolean addSemanticAttribute(String attribute, String attributeValue) {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean addSemanticRelation(String relation, String relationValue) {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public void addTemplate(String template) {

	}

	/**
	 * {@inheritDoc}
	 */
	public ITemplateFunction addTemplateFunction(String key, ITemplateFunction value) {
		return fConfiguration.addTemplateFunction(key, value);
	}

	/**
	 * {@inheritDoc}
	 */
	public TagToken addTokenTag(String key, TagToken value) {
		return fConfiguration.addTokenTag(key, value);
	}

	/**
	 * {@inheritDoc}
	 */
	public String[] addToReferences(String reference, String nameAttribute) {
		String[] result = new String[2];
		result[1] = null;
		if (fReferences == null) {
			fReferences = new ArrayList<Reference>();
			fReferenceNames = new HashMap<String, Integer>();
		}
		if (nameAttribute != null) {
			Integer index = fReferenceNames.get(nameAttribute);
			if (index != null) {
				result[0] = index.toString();
				Reference ref = fReferences.get(index - 1);
				int count = ref.incCounter();
				if (count >= Reference.CHARACTER_REFS.length()) {
					result[1] = nameAttribute + '_' + 'Z';
				} else {
					if (count == 0) {
						result[1] = nameAttribute;
					} else {
						result[1] = nameAttribute + '_' + Reference.CHARACTER_REFS.charAt(count);
					}
				}
				return result;
			}
		}

		if (nameAttribute != null) {
			fReferences.add(new Reference(reference, nameAttribute));
			Integer index = Integer.valueOf(fReferences.size());
			fReferenceNames.put(nameAttribute, index);
			result[1] = nameAttribute;
		} else {
			fReferences.add(new Reference(reference));
		}
		result[0] = Integer.toString(fReferences.size());
		return result;
	}

	/**
	 * Add a section header with the given <code>headLevel</code> to the
	 * &quot;table of content&quot;
	 * 
	 * @param toc
	 *          the &quot;table of content list&quot;
	 * @param strPair
	 *          a new section header
	 * @param headLevel
	 *          the level of the new section header
	 */
	protected void addToTableOfContent(List<Object> toc, SectionHeader strPair, int headLevel) {
		if (headLevel == 1) {
			toc.add(strPair);
		} else {
			if (toc.size() > 0) {
				if (toc.get(toc.size() - 1) instanceof List) {
					addToTableOfContent((List<Object>) toc.get(toc.size() - 1), strPair, --headLevel);
					return;
				}
			}
			ArrayList<Object> list = new ArrayList<Object>();
			toc.add(list);
			addToTableOfContent(list, strPair, --headLevel);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void append(BaseToken contentNode) {
		fTagStack.append(contentNode);
	}

	/**
	 * {@inheritDoc}
	 */
	public void appendExternalImageLink(String imageSrc, String imageAltText) {
		TagNode spanTagNode = new TagNode("span");
		append(spanTagNode);
		spanTagNode.addAttribute("class", "image", true);
		TagNode imgTagNode = new TagNode("img");
		spanTagNode.addChild(imgTagNode);
		imgTagNode.addAttribute("src", imageSrc, true);
		imgTagNode.addAttribute("alt", imageAltText, true);
		// "nofollow" keyword is not allowed for XHTML
		// imgTagNode.addAttribute("rel", "nofollow", true);
	}

	/**
	 * Append an external link (starting with http, https, ftp,...) as described
	 * in <a href="http://en.wikipedia.org/wiki/Help:Link#External_links">Help
	 * Links</a>
	 * 
	 * @param link
	 *          the external link with <code>http://, https:// or ftp://</code>
	 *          prefix
	 * @param linkName
	 *          the link name which is separated from the URL by a space
	 * @param withoutSquareBrackets
	 *          if <code>true</code> a link with no square brackets around the
	 *          link was parsed
	 * @deprecated use
	 *             {@link IWikiModel#appendExternalLink(String, String, String, boolean)}
	 *             instead.
	 */
	@Deprecated
	public void appendExternalLink(String link, String linkName, boolean withoutSquareBrackets) {
		appendExternalLink("", link, linkName, withoutSquareBrackets);
	}

	/**
	 * Append an external link (starting with http, https, ftp,...) as described
	 * in <a href="http://en.wikipedia.org/wiki/Help:Link#External_links">Help
	 * Links</a>
	 * 
	 * @param uriSchemeName
	 *          the top level URI (Uniform Resource Identifier) scheme name
	 *          (without the following colon character ":"). Example "ftp",
	 *          "http", "https". See <a
	 *          href="http://en.wikipedia.org/wiki/URI_scheme">URI scheme</a>
	 * @param link
	 *          the external link with <code>http://, https:// or ftp://</code>
	 *          prefix
	 * @param linkName
	 *          the link name which is separated from the URL by a space
	 * @param withoutSquareBrackets
	 *          if <code>true</code> a link with no square brackets around the
	 *          link was parsed
	 */
	public void appendExternalLink(String uriSchemeName, String link, String linkName, boolean withoutSquareBrackets) {
		link = Utils.escapeXml(link, true, false, false);
		// is the given link an image?
		// int indx = link.lastIndexOf(".");
		// if (indx > 0 && indx < (link.length() - 3)) {
		// String ext = link.substring(indx + 1);
		// if (ext.equalsIgnoreCase("gif") || ext.equalsIgnoreCase("png") ||
		// ext.equalsIgnoreCase("jpg") || ext.equalsIgnoreCase("jpeg")
		// || ext.equalsIgnoreCase("bmp")) {
		// appendExternalImageLink(link, linkName);
		// return;
		// }
		// }
		TagNode aTagNode = new TagNode("a");
		aTagNode.addAttribute("href", link, true);
		aTagNode.addAttribute("rel", "nofollow", true);
		if (withoutSquareBrackets) {
			aTagNode.addAttribute("class", "externallink", true);
			aTagNode.addAttribute("title", link, true);
			append(aTagNode);
			aTagNode.addChild(new ContentToken(linkName));
		} else {
			String trimmedText = linkName.trim();
			if (trimmedText.length() > 0) {
				pushNode(aTagNode);
				if (linkName.equals(link)) {
					if (withoutSquareBrackets) {
						aTagNode.addAttribute("class", "externallink", true);
						aTagNode.addAttribute("title", link, true);
						aTagNode.addChild(new ContentToken(trimmedText));
					} else {
						aTagNode.addAttribute("class", "external autonumber", true);
						aTagNode.addChild(new ContentToken("[" + (++fExternalLinksCounter) + "]"));
					}
				} else {
					aTagNode.addAttribute("class", "externallink", true);
					aTagNode.addAttribute("title", link, true);
					WikipediaParser.parseRecursive(trimmedText, this, false, true);
				}
				popNode();
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public ITableOfContent appendHead(String rawHead, int headLevel, boolean noToC, int headCounter) {
		return appendHead(rawHead, headLevel, noToC, headCounter, 0, 0);
	}

	/**
	 * Append a new head to the table of content
	 * 
	 * @param rawHead
	 * @param headLevel
	 */
	public ITableOfContent appendHead(String rawHead, int headLevel, boolean noToC, int headCounter, int startPosition,
			int endPosition) {
		TagStack localStack = WikipediaParser.parseRecursive(rawHead.trim(), this, true, true);

		WPTag headTagNode = new WPTag("h" + headLevel);
		TagNode spanTagNode = new TagNode("span");
		// Example:
		// <h2><span class="mw-headline" id="Header_level_2">Header level
		// 2</span></h2>
		spanTagNode.addChildren(localStack.getNodeList());
		headTagNode.addChild(spanTagNode);
		String tocHead = headTagNode.getBodyString();
		String anchor = Encoder.encodeDotUrl(tocHead);
		createTableOfContent(false);
		if (!noToC && (headCounter > 3)) {
			fTableOfContentTag.setShowToC(true);
		}
		if (fToCSet.contains(anchor)) {
			String newAnchor = anchor;
			for (int i = 2; i < Integer.MAX_VALUE; i++) {
				newAnchor = anchor + '_' + Integer.toString(i);
				if (!fToCSet.contains(newAnchor)) {
					break;
				}
			}
			anchor = newAnchor;
		}
		fToCSet.add(anchor);
		SectionHeader strPair = new SectionHeader(headLevel, startPosition, endPosition, tocHead, anchor);
		addToTableOfContent(fTableOfContent, strPair, headLevel);
		if (getRecursionLevel() == 1) {
			buildEditLinkUrl(fSectionCounter++);
		}
		spanTagNode.addAttribute("class", "mw-headline", true);
		spanTagNode.addAttribute("id", anchor, true);

		append(headTagNode);
		return fTableOfContentTag;
	}

	/**
	 * {@inheritDoc}
	 */
	public void appendInternalImageLink(String hrefImageLink, String srcImageLink, ImageFormat imageFormat) {
		// int pxWidth = imageFormat.getWidth();
		// int pxHeight = imageFormat.getHeight();
		String caption = imageFormat.getCaption();
		String imageType = imageFormat.getType();
		TagNode divInnerTagNode = new TagNode("div");
		divInnerTagNode.addAttribute("id", "image", false);
		// String link = imageFormat.getLink();
		// if (link != null) {
		// String href = encodeTitleToUrl(link, true);
		// divTagNode.addAttribute("href", href, false);
		// } else {
		if (hrefImageLink.length() != 0) {
			divInnerTagNode.addAttribute("href", hrefImageLink, false);
		}
		// }

		divInnerTagNode.addAttribute("src", srcImageLink, false);
		setDefaultThumbWidth(imageFormat);
		divInnerTagNode.addObjectAttribute("wikiobject", imageFormat);
		// if (pxHeight != -1) {
		// if (pxWidth != -1) {
		// divInnerTagNode.addAttribute("style", "height:" + pxHeight + "px; " +
		// "width:" + pxWidth + "px", false);
		// } else {
		// divInnerTagNode.addAttribute("style", "height:" + pxHeight + "px",
		// false);
		// }
		// } else {
		// if (pxWidth != -1) {
		// divInnerTagNode.addAttribute("style", "width:" + pxWidth + "px", false);
		// }
		// }
		pushNode(divInnerTagNode);
		try {
			// TODO: test all these cases
			if (caption != null && caption.length() > 0
					&& ("frame".equals(imageType) || "thumb".equals(imageType) || "thumbnail".equals(imageType))) {

				TagNode captionTagNode = new TagNode("div");
				String clazzValue = "caption";
				String type = imageFormat.getType();
				if (type != null) {
					clazzValue = type + clazzValue;
				}
				captionTagNode.addAttribute("class", clazzValue, false);
				//			
				TagStack localStack = WikipediaParser.parseRecursive(caption, this, true, true);
				captionTagNode.addChildren(localStack.getNodeList());
				String altAttribute = imageFormat.getAlt();
				if (altAttribute == null) {
					altAttribute = captionTagNode.getBodyString();
					imageFormat.setAlt(Encoder.encodeHtml(altAttribute));// see issue #25
				}
				pushNode(captionTagNode);
				popNode();
			}
		} finally {
			popNode(); // div
		}
	}

	/**
	 * Set the default thumb format width. This method sets a &quot;default
	 * width&quot; (220px) for images of type &quot;thumb&quot;, if no width is
	 * set in the image format string.
	 * 
	 * @param imageFormat
	 */
	protected void setDefaultThumbWidth(ImageFormat imageFormat) {
		int pxWidth = imageFormat.getWidth();
		String imageType = imageFormat.getType();
		if (pxWidth == -1 && (imageType == null || "thumb".equals(imageType))) {
			// set the default thumb format width
			imageFormat.setWidth(220);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void appendInternalLink(String topic, String hashSection, String topicDescription, String cssClass, boolean parseRecursive) {
		WPATag aTagNode = new WPATag();
		// append(aTagNode);
		// aTagNode.addAttribute("id", "w", true);
		String href = encodeTitleToUrl(topic, true);
		if (hashSection != null) {
			href = href + '#' + encodeTitleDotUrl(hashSection, true);
		}
		aTagNode.addAttribute("href", href, true);
		if (cssClass != null) {
			aTagNode.addAttribute("class", cssClass, true);
		}
		aTagNode.addObjectAttribute("wikilink", topic);

		pushNode(aTagNode);
		if (parseRecursive) {
			WikipediaParser.parseRecursive(topicDescription.trim(), this, false, true);
		} else {
			aTagNode.addChild(new ContentToken(topicDescription));
		}
		popNode();

		// ContentToken text = new ContentToken(topicDescription);
		// aTagNode.addChild(text);
	}

	/**
	 * {@inheritDoc}
	 */
	public void appendInterWikiLink(String namespace, String title, String linkText) {
		String hrefLink = getInterwikiMap().get(namespace);
		if (hrefLink == null) {
			// shouldn't really happen
			hrefLink = "#";
		}

		// false -> don't convert first character to uppercase for interwiki links
		String encodedtopic = encodeTitleToUrl(title, false);
		if (replaceColon()) {
			encodedtopic = encodedtopic.replace(':', '/');
		}
		hrefLink = hrefLink.replace("${title}", encodedtopic);

		TagNode aTagNode = new TagNode("a");
		// append(aTagNode);
		aTagNode.addAttribute("href", hrefLink, true);
		// aTagNode.addChild(new ContentToken(linkText));
		pushNode(aTagNode);
		WikipediaParser.parseRecursive(linkText.trim(), this, false, true);
		popNode();
	}

	/**
	 * {@inheritDoc}
	 */
	public void appendISBNLink(String isbnPureText) {
		StringBuffer isbnUrl = new StringBuffer(isbnPureText.length() + 100);
		isbnUrl.append("http://www.amazon.com/exec/obidos/ASIN/");

		for (int index = 0; index < isbnPureText.length(); index++) {
			if (isbnPureText.charAt(index) >= '0' && isbnPureText.charAt(index) <= '9') {
				isbnUrl.append(isbnPureText.charAt(index));
			}
		}

		String isbnString = isbnUrl.toString();
		TagNode aTagNode = new TagNode("a");
		append(aTagNode);
		aTagNode.addAttribute("href", isbnString, true);
		aTagNode.addAttribute("class", "external text", true);
		aTagNode.addAttribute("title", isbnString, true);
		aTagNode.addAttribute("rel", "nofollow", true);
		aTagNode.addChild(new ContentToken(isbnPureText));
	}

	/**
	 * {@inheritDoc}
	 */
	public void appendMailtoLink(String link, String linkName, boolean withoutSquareBrackets) {
		// is it an image?
		// link = Utils.escapeXml(link, true, false, false);
		// int indx = link.lastIndexOf(".");
		// if (indx > 0 && indx < (link.length() - 3)) {
		// String ext = link.substring(indx + 1);
		// if (ext.equalsIgnoreCase("gif") || ext.equalsIgnoreCase("png") ||
		// ext.equalsIgnoreCase("jpg") || ext.equalsIgnoreCase("jpeg")
		// || ext.equalsIgnoreCase("bmp")) {
		// appendExternalImageLink(link, linkName);
		// return;
		// }
		// }
		TagNode aTagNode = new TagNode("a");
		append(aTagNode);
		aTagNode.addAttribute("href", link, true);
		aTagNode.addAttribute("class", "external free", true);
		aTagNode.addAttribute("title", link, true);
		aTagNode.addAttribute("rel", "nofollow", true);
		aTagNode.addChild(new ContentToken(linkName));
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean appendRawNamespaceLinks(String rawNamespaceTopic, String viewableLinkDescription, boolean containsNoPipe) {
		int colonIndex = rawNamespaceTopic.indexOf(':');

		if (colonIndex != (-1)) {
			String nameSpace = rawNamespaceTopic.substring(0, colonIndex);

			if (isSemanticWebActive() && (rawNamespaceTopic.length() > colonIndex + 1)) {
				// See <a
				// href="http://en.wikipedia.org/wiki/Semantic_MediaWiki">Semantic
				// MediaWiki</a> for more information.
				if (rawNamespaceTopic.charAt(colonIndex + 1) == ':') {
					// found an SMW relation
					String relationValue = rawNamespaceTopic.substring(colonIndex + 2);

					if (addSemanticRelation(nameSpace, relationValue)) {
						if (containsNoPipe) {
							viewableLinkDescription = relationValue;
						}
						if (viewableLinkDescription.trim().length() > 0) {
							appendInternalLink(relationValue, null, viewableLinkDescription, "interwiki", true);
						}
						return true;
					}
				} else if (rawNamespaceTopic.charAt(colonIndex + 1) == '=') {
					// found an SMW attribute
					String attributeValue = rawNamespaceTopic.substring(colonIndex + 2);
					if (addSemanticAttribute(nameSpace, attributeValue)) {
						append(new ContentToken(attributeValue));
						return true;
					}
				}

			}
			if (isCategoryNamespace(nameSpace)) {
				// add the category to this texts metadata
				String category = rawNamespaceTopic.substring(colonIndex + 1).trim();
				if (category != null && category.length() > 0) {
					// TODO implement more sort-key behaviour
					// http://en.wikipedia.org/wiki/Wikipedia:Categorization#
					// Category_sorting
					addCategory(category, viewableLinkDescription);
					return true;
				}
			} else if (isInterWiki(nameSpace)) {
				String title = rawNamespaceTopic.substring(colonIndex + 1);
				if (title != null && title.length() > 0) {
					appendInterWikiLink(nameSpace, title, viewableLinkDescription);
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean appendRawWikipediaLink(String rawLinkText, String suffix) {
		String rawTopicName = rawLinkText;
		if (rawTopicName != null) {
			// trim the name for whitespace characters on the left side
			int trimLeftIndex = 0;
			while ((trimLeftIndex < rawTopicName.length()) && (rawTopicName.charAt(trimLeftIndex) <= ' ')) {
				trimLeftIndex++;
			}
			if (trimLeftIndex > 0) {
				rawTopicName = rawTopicName.substring(trimLeftIndex);
			}
			// Is there an alias like [alias|link] ?
			int pipeIndex = rawTopicName.lastIndexOf('|');
			String alias = "";
			if (-1 != pipeIndex) {
				alias = rawTopicName.substring(pipeIndex + 1);
				rawTopicName = rawTopicName.substring(0, pipeIndex);
				if (alias.length() == 0) {
					// special cases like: [[Test:hello world|]] or [[Test(hello
					// world)|]]
					// or [[Test, hello world|]]
					alias = rawTopicName;
					int index = alias.indexOf(':');
					if (index != -1) {
						alias = alias.substring(index + 1).trim();
					} else {
						index = alias.indexOf('(');
						if (index != -1) {
							alias = alias.substring(0, index).trim();
						} else {
							index = alias.indexOf(',');
							if (index != -1) {
								alias = alias.substring(0, index).trim();
							}
						}
					}
				}
			}

			int hashIndex = rawTopicName.lastIndexOf('#');

			String hash = "";
			if (-1 != hashIndex && hashIndex != rawTopicName.length() - 1) {
				hash = rawTopicName.substring(hashIndex + 1);
				rawTopicName = rawTopicName.substring(0, hashIndex);
			}

			// trim the name for whitespace characters on the right side
			int trimRightIndex = rawTopicName.length() - 1;
			while ((trimRightIndex >= 0) && (rawTopicName.charAt(trimRightIndex) <= ' ')) {
				trimRightIndex--;
			}
			if (trimRightIndex != rawTopicName.length() - 1) {
				rawTopicName = rawTopicName.substring(0, trimRightIndex + 1);
			}

			// rawTopicName = Encoder.encodeHtml(rawTopicName); // see issue #25
			String viewableLinkDescription;
			if (-1 != pipeIndex) {
				viewableLinkDescription = alias + suffix;
			} else {
				if (rawTopicName.length() > 0 && rawTopicName.charAt(0) == ':') {
					viewableLinkDescription = rawTopicName.substring(1) + suffix;
				} else {
					viewableLinkDescription = rawTopicName + suffix;
				}
			}

			if (appendRawNamespaceLinks(rawTopicName, viewableLinkDescription, pipeIndex == (-1))) {
				return true;
			}

			int indx = rawTopicName.indexOf(':');
			String namespace = null;
			if (indx >= 0) {
				namespace = rawTopicName.substring(0, indx);
			}
			if (namespace != null && isImageNamespace(namespace)) {
				parseInternalImageLink(namespace, rawLinkText);
				return false;
			} else {
				if (rawTopicName.length() > 0 && rawTopicName.charAt(0) == ':') {
					rawTopicName = rawTopicName.substring(1);
				}
				if (rawTopicName.length() > 0 && rawTopicName.charAt(0) == ':') {
					rawTopicName = rawTopicName.substring(1);
				}
				addLink(rawTopicName);
				if (-1 != hashIndex) {
					appendInternalLink(rawTopicName, hash, viewableLinkDescription, null, true);
				} else {
					appendInternalLink(rawTopicName, null, viewableLinkDescription, null, true);
				}
				return true;
			}
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean appendRedirectLink(String redirectLink) {
		fRedirectLink = redirectLink;
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	public void appendSignature(Appendable writer, int numberOfTildes) throws IOException {
		switch (numberOfTildes) {
		case 3:
			writer.append("~~~");
			break;
		case 4:
			writer.append("~~~~");
			break;
		case 5:
			writer.append("~~~~~");
			break;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void appendStack(TagStack stack) {
		if (stack != null) {
			fTagStack.append(stack);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public TagNode appendToCAnchor(String anchor) {
		TagNode aTagNode = new TagNode("a");
		aTagNode.addAttribute("name", anchor, true);
		aTagNode.addAttribute("id", anchor, true);
		return aTagNode;
	}

	/**
	 * {@inheritDoc}
	 */
	public void buildEditLinkUrl(int section) {
	}

	/**
	 * {@inheritDoc}
	 */
	public AbstractParser createNewInstance(String rawWikitext) {
		return new WikipediaParser(rawWikitext, isTemplateTopic(), getWikiListener());
	}

	/**
	 * 
	 * @param isTOCIdentifier
	 *          <code>true</code> if the __TOC__ keyword was parsed
	 */
	public ITableOfContent createTableOfContent(boolean isTOCIdentifier) {
		if (fTableOfContentTag == null) {
			TableOfContentTag tableOfContentTag = new TableOfContentTag("div");
			tableOfContentTag.addAttribute("id", "tableofcontent", true);
			tableOfContentTag.setShowToC(false);
			tableOfContentTag.setTOCIdentifier(isTOCIdentifier);
			fTableOfContentTag = tableOfContentTag;
			this.append(fTableOfContentTag);
		} else {
			if (isTOCIdentifier) {
				TableOfContentTag tableOfContentTag = (TableOfContentTag) fTableOfContentTag.clone();
				fTableOfContentTag.setShowToC(false);
				tableOfContentTag.setShowToC(true);
				tableOfContentTag.setTOCIdentifier(isTOCIdentifier);
				fTableOfContentTag = tableOfContentTag;
				fTableOfContent = null;
				this.append(fTableOfContentTag);
			}
		}

		if (fTableOfContentTag != null) {
			if (fTableOfContent == null) {
				fTableOfContent = fTableOfContentTag.getTableOfContent();
			}
		}
		if (fToCSet == null) {
			fToCSet = new HashSet<String>();
		}
		return fTableOfContentTag;
	}

	/**
	 * {@inheritDoc}
	 */
	public int decrementRecursionLevel() {
		return --fRecursionLevel;
	}

	/**
	 * {@inheritDoc}
	 */
	public int decrementTemplateRecursionLevel() {
		return --fTemplateRecursionCount;
	}

	/**
	 * {@inheritDoc}
	 */
	public String encodeTitleDotUrl(String wikiTitle, boolean firstCharacterAsUpperCase) {
		return Encoder.encodeTitleDotUrl(wikiTitle, firstCharacterAsUpperCase);
	}

	/**
	 * {@inheritDoc}
	 */
	public String encodeTitleToUrl(String wikiTitle, boolean firstCharacterAsUpperCase) {
		return Encoder.encodeTitleToUrl(wikiTitle, firstCharacterAsUpperCase);
	}

	/**
	 * {@inheritDoc}
	 */
	public String get2ndCategoryNamespace() {
		return fNamespace.getCategory2();
	}

	/**
	 * {@inheritDoc}
	 */
	public String get2ndImageNamespace() {
		return fNamespace.getImage2();
	}

	/**
	 * {@inheritDoc}
	 */
	public String get2ndTemplateNamespace() {
		return fNamespace.getTemplate2();
	}

	/**
	 * Resolve an attribute reference. It can be in four possible places:
	 * 
	 * 1. the attribute list for the current template 2. if self is an embedded
	 * template, somebody invoked us possibly with arguments--check the argument
	 * context 3. if self is an embedded template, the attribute list for the
	 * enclosing instance (recursively up the enclosing instance chain) 4. if
	 * nothing is found in the enclosing instance chain, then it might be a map
	 * defined in the group or the its supergroup etc...
	 * 
	 * Attribute references are checked for validity. If an attribute has a value,
	 * its validity was checked before template rendering. If the attribute has no
	 * value, then we must check to ensure it is a valid reference. Somebody could
	 * reference any random value like $xyz$; formal arg checks before rendering
	 * cannot detect this--only the ref can initiate a validity check. So, if no
	 * value, walk up the enclosed template tree again, this time checking formal
	 * parameters not attributes Map. The formal definition must exist even if no
	 * value.
	 * 
	 * To avoid infinite recursion in toString(), we have another condition to
	 * check regarding attribute values. If your template has a formal argument,
	 * foo, then foo will hide any value available from "above" in order to
	 * prevent infinite recursion.
	 * 
	 * This method is not static so people can override functionality.
	 */
	public Object getAttribute(String attribute) { // StringTemplate self, String
		// attribute) {
		// System.out.println("### get("+self.getEnclosingInstanceStackString()+", "+
		// attribute+")");
		// System.out.println("attributes="+(self.attributes!=null?self.attributes.
		// keySet().toString():"none"));
		// if ( self==null ) {
		// return null;
		// }
		//
		// if ( lintMode ) {
		// self.trackAttributeReference(attribute);
		// }

		// is it here?
		Object o = null;
		if (attributes != null) {
			o = attributes.get(attribute);
		}

		// // nope, check argument context in case embedded
		// if ( o==null ) {
		// Map argContext = self.getArgumentContext();
		// if ( argContext!=null ) {
		// o = argContext.get(attribute);
		// }
		// }
		//
		// if ( o==null &&
		// !self.passThroughAttributes &&
		// self.getFormalArgument(attribute)!=null )
		// {
		// // if you've defined attribute as formal arg for this
		// // template and it has no value, do not look up the
		// // enclosing dynamic scopes. This avoids potential infinite
		// // recursion.
		// return null;
		// }
		//
		// // not locally defined, check enclosingInstance if embedded
		// if ( o==null && self.enclosingInstance!=null ) {
		// /*
		// System.out.println("looking for "+getName()+"."+attribute+" in super="+
		// enclosingInstance.getName());
		// */
		// Object valueFromEnclosing = get(self.enclosingInstance, attribute);
		// if ( valueFromEnclosing==null ) {
		// checkNullAttributeAgainstFormalArguments(self, attribute);
		// }
		// o = valueFromEnclosing;
		// }
		//
		// // not found and no enclosing instance to look at
		// else if ( o==null && self.enclosingInstance==null ) {
		// // It might be a map in the group or supergroup...
		// o = self.group.getMap(attribute);
		// }

		return o;
	}

	/**
	 * What renderer is registered for this attributeClassType for this template.
	 * If not found, the template's group is queried.
	 */
	public AttributeRenderer getAttributeRenderer(Class attributeClassType) {
		AttributeRenderer renderer = null;
		if (attributeRenderers != null) {
			renderer = (AttributeRenderer) attributeRenderers.get(attributeClassType);
		}
		if (renderer != null) {
			// found it!
			return renderer;
		}

		// we have no renderer overrides for the template or none for class arg
		// check parent template if we are embedded
		// if ( enclosingInstance!=null ) {
		// return enclosingInstance.getAttributeRenderer(attributeClassType);
		// }
		// // else check group
		// return group.getAttributeRenderer(attributeClassType);
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getCategoryNamespace() {
		return fNamespace.getCategory();
	}

	/**
	 * {@inheritDoc}
	 */
	public Map<String, SourceCodeFormatter> getCodeFormatterMap() {
		return fConfiguration.getCodeFormatterMap();
	}

	/**
	 * {@inheritDoc}
	 */
	public Date getCurrentTimeStamp() {
		return new Date(System.currentTimeMillis());
	}

	/**
	 * {@inheritDoc}
	 */
	public String getImageNamespace() {
		return fNamespace.getImage();
	}

	/**
	 * {@inheritDoc}
	 */
	public Map<String, String> getInterwikiMap() {
		return fConfiguration.getInterwikiMap();
	}

	/**
	 * {@inheritDoc}
	 */
	public Locale getLocale() {
		return fLocale;
	}

	/**
	 * {@inheritDoc}
	 */
	public synchronized int getNextNumber() {
		return fNextNumberCounter++;
	}

	/**
	 * {@inheritDoc}
	 */
	public TagToken getNode(int offset) {
		return fTagStack.get(offset);
	}

	// public TableOfContentTag getTableOfContentTag(boolean isTOCIdentifier) {
	// if (fTableOfContentTag == null) {
	// TableOfContentTag tableOfContentTag = new TableOfContentTag("div");
	// tableOfContentTag.addAttribute("id", "tableofcontent", true);
	// tableOfContentTag.setShowToC(false);
	// tableOfContentTag.setTOCIdentifier(isTOCIdentifier);
	// fTableOfContentTag = tableOfContentTag;
	// } else {
	// if (isTOCIdentifier) {
	// // try {
	// TableOfContentTag tableOfContentTag = (TableOfContentTag)
	// fTableOfContentTag.clone();
	// fTableOfContentTag.setShowToC(false);
	// tableOfContentTag.setShowToC(true);
	// tableOfContentTag.setTOCIdentifier(isTOCIdentifier);
	// fTableOfContentTag = tableOfContentTag;
	// // } catch (CloneNotSupportedException e) {
	// // e.printStackTrace();
	// // }
	// } else {
	// return fTableOfContentTag;
	// }
	// }
	// this.append(fTableOfContentTag);
	// return fTableOfContentTag;
	// }

	/**
	 * {@inheritDoc}
	 */
	public String getPageName() {
		return fPageTitle;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getRawWikiContent(String namespace, String templateName, Map<String, String> templateParameters) {
		if (Configuration.RAW_CONTENT) {
			System.out.println("AbstractWikiModel raw: " + " " + namespace + " " + templateName);
		}
		if (isTemplateNamespace(namespace)) {
			String magicWord = templateName;
			String parameter = "";
			int index = magicWord.indexOf(':');
			if (index > 0) {
				parameter = magicWord.substring(index + 1);
				if (parameter.length() != 0) {
					parameter = AbstractTemplateFunction.parseTrim(parameter, this);
				}
				magicWord = magicWord.substring(0, index);
			}
			if (MagicWord.isMagicWord(magicWord)) {
				return MagicWord.processMagicWord(magicWord, parameter, this);
			}
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public int getRecursionLevel() {
		return fRecursionLevel;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getRedirectLink() {
		return fRedirectLink;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Reference> getReferences() {
		return fReferences;
	}

	/**
	 * {@inheritDoc}
	 */
	public ResourceBundle getResourceBundle() {
		return fNamespace.getResourceBundle();
	}

	/**
	 * {@inheritDoc}
	 */
	public List<SemanticAttribute> getSemanticAttributes() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<SemanticRelation> getSemanticRelations() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public SimpleDateFormat getSimpleDateFormat() {
		if (fFormatter != null) {
			return fFormatter;
		}
		fFormatter = new SimpleDateFormat();
		TimeZone utc = TimeZone.getTimeZone("GMT+00");
		fFormatter.setTimeZone(utc);
		return fFormatter;
	}

	/**
	 * {@inheritDoc}
	 */
	public ITableOfContent getTableOfContent() {
		return fTableOfContentTag;
	}

	/**
	 * {@inheritDoc}
	 */
	public Map<String, String> getTemplateCallsCache() {
		return fConfiguration.getTemplateCallsCache();
	}

	/**
	 * {@inheritDoc}
	 */
	public ITemplateFunction getTemplateFunction(String name) {
		return getTemplateMap().get(name);
	}

	/**
	 * {@inheritDoc}
	 */
	public Map<String, ITemplateFunction> getTemplateMap() {
		return fConfiguration.getTemplateMap();
	}

	/**
	 * {@inheritDoc}
	 */
	public String getTemplateNamespace() {
		return fNamespace.getTemplate();
	}

	/**
	 * {@inheritDoc}
	 */
	public Map<String, TagToken> getTokenMap() {
		return fConfiguration.getTokenMap();
	}

	/**
	 * {@inheritDoc}
	 */
	public Set<String> getUriSchemeSet() {
		return fConfiguration.getUriSchemeSet();
	}

	/**
	 * {@inheritDoc}
	 */
	public IEventListener getWikiListener() {
		return fWikiListener;
	}

	/**
	 * {@inheritDoc}
	 */
	public int incrementRecursionLevel() {
		return ++fRecursionLevel;
	}

	/**
	 * {@inheritDoc}
	 */
	public int incrementTemplateRecursionLevel() {
		return ++fTemplateRecursionCount;
	}

	/**
	 * Initialize the internal class attributes
	 */
	protected void initialize() {
		if (!fInitialized) {
			fWikiListener = null;
			fFormatter = null;
			fToCSet = null;
			fTableOfContent = null;
			fTableOfContentTag = null;
			fTagStack = new TagStack();
			fReferences = null;
			fReferenceNames = null;
			fRecursionLevel = 0;
			fTemplateRecursionCount = 0;
			fSectionCounter = 0;
			fExternalLinksCounter = 0;
			fInitialized = true;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isCamelCaseEnabled() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isCategoryNamespace(String namespace) {
		return namespace.equalsIgnoreCase(fNamespace.getCategory()) || namespace.equalsIgnoreCase(fNamespace.getCategory2());
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isEditorMode() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isImageNamespace(String namespace) {
		return namespace.equalsIgnoreCase(fNamespace.getImage()) || namespace.equalsIgnoreCase(fNamespace.getImage2());
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isInterWiki(String namespace) {
		return getInterwikiMap().containsKey(namespace);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isMathtranRenderer() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isNamespace(String namespace) {
		return isImageNamespace(namespace) || isTemplateNamespace(namespace) || isCategoryNamespace(namespace);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isPreviewMode() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isSemanticWebActive() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isTemplateNamespace(String namespace) {
		return namespace.equalsIgnoreCase(fNamespace.getTemplate()) || namespace.equalsIgnoreCase(fNamespace.getTemplate2());
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isTemplateTopic() {
		return fTemplateTopic;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isValidUriScheme(String uriScheme) {
		return getUriSchemeSet().contains(uriScheme);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isValidUriSchemeSpecificPart(String uriScheme, String uriSchemeSpecificPart) {
		if (uriScheme.equals("ftp") || uriScheme.equals("http") || uriScheme.equals("https")) {
			return uriSchemeSpecificPart.length() >= 2 && uriSchemeSpecificPart.substring(0, 2).equals("//");
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean parseBBCodes() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean parseBehaviorSwitch(String identifier) {
		for (int i = 0; i < WikipediaParser.TOC_IDENTIFIERS.length; i++) {
			if (WikipediaParser.TOC_IDENTIFIERS[i].equals(identifier)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	public void parseEvents(IEventListener listener, String rawWikiText) {
		initialize();
		if (rawWikiText == null) {
			return;
		}
		fWikiListener = listener;
		WikipediaParser.parse(rawWikiText, this, false, null);
		fInitialized = false;
	}

	/**
	 * {@inheritDoc}
	 */
	public String parseTemplates(String rawWikiText) {
		return parseTemplates(rawWikiText, false);
	}

	/**
	 * {@inheritDoc}
	 */
	public String parseTemplates(String rawWikiText, boolean parseOnlySignature) {
		if (rawWikiText == null) {
			return "";
		}
		if (!parseOnlySignature) {
			initialize();
		}
		StringBuilder buf = new StringBuilder(rawWikiText.length() + rawWikiText.length() / 10);
		try {
			TemplateParser.parse(rawWikiText, this, buf, parseOnlySignature, true);
		} catch (Exception ioe) {
			ioe.printStackTrace();
			buf.append("<span class=\"error\">TemplateParser exception: " + ioe.getClass().getSimpleName() + "</span>");
		}
		return buf.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	public TagToken peekNode() {
		return fTagStack.peek();
	}

	/**
	 * {@inheritDoc}
	 */
	public TagToken popNode() {
		return fTagStack.pop();
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean pushNode(TagToken node) {
		return fTagStack.push(node);
	}

	/**
	 * Map a value to a named attribute. Throw NoSuchElementException if the named
	 * attribute is not formally defined in self's specific template and a formal
	 * argument list exists.
	 */
	protected void rawSetAttribute(Map attributes, String name, Object value) {
		// if ( formalArguments!=FormalArgument.UNKNOWN &&
		// getFormalArgument(name)==null )
		// {
		// // a normal call to setAttribute with unknown attribute
		// throw new NoSuchElementException("no such attribute: "+name+
		// " in template context "+
		// getEnclosingInstanceStackString());
		// }
		if (value == null) {
			return;
		}
		attributes.put(name, value);
	}

	/**
	 * Register a renderer for all objects of a particular type. This overrides
	 * any renderer set in the group for this class type.
	 */
	public void registerRenderer(Class attributeClassType, AttributeRenderer renderer) {
		if (attributeRenderers == null) {
			attributeRenderers = new HashMap();
		}
		attributeRenderers.put(attributeClassType, renderer);
	}

	/**
	 * {@inheritDoc}
	 */
	public String render(ITextConverter converter, String rawWikiText) {
		return render(converter, rawWikiText, false);
	}

	/**
	 * {@inheritDoc}
	 */
	public String render(ITextConverter converter, String rawWikiText, boolean templateTopic) {
		initialize();
		if (rawWikiText == null) {
			return "";
		}
		StringBuilder buf = new StringBuilder(rawWikiText.length() + rawWikiText.length() / 10);
		try {
			render(converter, rawWikiText, buf, templateTopic, true);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return buf.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	public void render(ITextConverter converter, String rawWikiText, Appendable buf, boolean templateTopic, boolean parseTemplates)
			throws IOException {
		initialize();
		if (rawWikiText == null) {
			return;
		}
		fTemplateTopic = templateTopic;
		WikipediaParser.parse(rawWikiText, this, parseTemplates, null);
		if (converter != null) {
			List<BaseToken> list = fTagStack.getNodeList();
			try {
				converter.nodesToText(list, buf, this);
			} finally {
				fInitialized = false;
			}
			return;
		}
		fInitialized = false;
	}

	/**
	 * {@inheritDoc}
	 */
	public String render(String rawWikiText) {
		return render(rawWikiText, false);
	}

	/**
	 * {@inheritDoc}
	 */
	public String render(String rawWikiText, boolean templateTopic) {
		return render(new HTMLConverter(), rawWikiText, templateTopic);
	}

	/**
	 * {@inheritDoc}
	 */
	public String renderPDF(String rawWikiText) {
		return render(new PDFConverter(), rawWikiText, false);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean replaceColon() {
		return true;
	}

	/**
	 * Set an attribute for this template. If you set the same attribute more than
	 * once, you get a multi-valued attribute. If you send in a StringTemplate
	 * object as a value, it's enclosing instance (where it will inherit values
	 * from) is set to 'this'. This would be the normal case, though you can set
	 * it back to null after this call if you want. If you send in a List plus
	 * other values to the same attribute, they all get flattened into one List of
	 * values. This will be a new list object so that incoming objects are not
	 * altered. If you send in an array, it is converted to an ArrayIterator.
	 */
	public void setAttribute(String name, Object value) {
		if (value == null || name == null) {
			return;
		}
		if (name.indexOf('.') >= 0) {
			throw new IllegalArgumentException("cannot have '.' in attribute names");
		}
		if (attributes == null) {
			attributes = new HashMap<String, Object>();
		}

		// if (value instanceof StringTemplate) {
		// ((StringTemplate) value).setEnclosingInstance(this);
		// } else {
		// // convert value if array
		// value = ASTExpr.convertArrayToList(value);
		// }

		// convert plain collections
		// get exactly in this scope (no enclosing)

		// it will be a multi-value attribute
		// System.out.println("exists: "+name+"="+o);
		AttributeList v = null;

		Object o = this.attributes.get(name);
		if (o == null) { // new attribute
			if (value instanceof List) {
				v = new AttributeList(((List) value).size());
				// flatten incoming list into existing
				v.addAll((List) value);
				rawSetAttribute(this.attributes, name, v);
				return;
			}
			rawSetAttribute(this.attributes, name, value);
			return;
		}

		if (o.getClass() == AttributeList.class) { // already a list made by ST
			v = (AttributeList) o;
		} else if (o instanceof List) { // existing attribute is non-ST List
			// must copy to an ST-managed list before adding new attribute
			List listAttr = (List) o;
			v = new AttributeList(listAttr.size());
			v.addAll(listAttr);
			rawSetAttribute(this.attributes, name, v); // replace attribute w/list
		} else {
			// non-list second attribute, must convert existing to ArrayList
			v = new AttributeList(); // make list to hold multiple values
			// make it point to list now
			rawSetAttribute(this.attributes, name, v); // replace attribute w/list
			v.add(o); // add previous single-valued attribute
		}
		if (value instanceof List) {
			// flatten incoming list into existing
			if (v != value) { // avoid weird cyclic add
				v.addAll((List) value);
			}
		} else {
			v.add(value);
		}
	}

	/**
	 * Specify a complete map of what object classes should map to which renderer
	 * objects.
	 */
	public void setAttributeRenderers(Map renderers) {
		this.attributeRenderers = renderers;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setPageName(String pageTitle) {
		fPageTitle = pageTitle;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setSemanticWebActive(boolean semanticWeb) {

	}

	/**
	 * {@inheritDoc}
	 */
	public void setTemplateCallsCache(Map<String, String> map) {
		fConfiguration.setTemplateCallsCache(map);
	}

	/**
	 * {@inheritDoc}
	 */
	public void setUp() {
		fFormatter = null;
		fToCSet = null;
		fTableOfContent = null;
		fTableOfContentTag = null;
		fTagStack = new TagStack();
		fReferences = null;
		fReferenceNames = null;
		fRecursionLevel = 0;
		fTemplateRecursionCount = 0;
		fRedirectLink = null;
		fSectionCounter = 0;
		fExternalLinksCounter = 0;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean showSyntax(String tagName) {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	public int stackSize() {
		return fTagStack.size();
	}

	/**
	 * Substitute the template name by the template content and parameters and
	 * append the new content to the writer.
	 * 
	 * @param templateName
	 *          the name of the template
	 * @param parameterMap
	 *          the templates parameter <code>java.util.SortedMap</code>
	 * @param writer
	 *          the buffer to append the substituted template content
	 * @param cacheKey
	 *          a key for using in a cache
	 * @throws IOException
	 */
	public void substituteTemplateCall(String templateName, Map<String, String> parameterMap, Appendable writer) throws IOException {

		Map<String, String> templateCallsCache = null;
		String cacheKey = null;
		int cacheKeyLength = 0;
		templateCallsCache = fConfiguration.getTemplateCallsCache();
		if (templateCallsCache != null) {
			cacheKeyLength += templateName.length() + 1;
			for (Entry<String, String> entry : parameterMap.entrySet()) {
				cacheKeyLength += entry.getKey().length() + entry.getValue().length() + 2;
			}
			if (cacheKeyLength < Configuration.MAX_CACHE_KEY_LENGTH) {
				StringBuilder cacheKeyBuffer = new StringBuilder(cacheKeyLength);
				cacheKeyBuffer.append(templateName);
				cacheKeyBuffer.append("|");
				for (Entry<String, String> entry : parameterMap.entrySet()) {
					cacheKeyBuffer.append(entry.getKey());
					cacheKeyBuffer.append("=");
					cacheKeyBuffer.append(entry.getValue());
					cacheKeyBuffer.append("|");
				}
				cacheKey = cacheKeyBuffer.toString();

				String value = templateCallsCache.get(cacheKey);
				if (value != null) {
					// System.out.println("Cache key: " + cacheKey);
					writer.append(value);
					if (Configuration.TEMPLATE_NAMES) {
						System.out.println("Cached: " + templateName + "-" + cacheKey);
					}
					return;
				}
			}
			if (Configuration.TEMPLATE_NAMES) {
				System.out.println("Not Cached: " + templateName + "-" + cacheKeyLength);
			}
		}

		String plainContent;
		if (templateName.length() > 0 && templateName.charAt(0) == ':') {
			// invalidate cache:
			templateCallsCache = null;
			plainContent = getRawWikiContent("", templateName.substring(1), parameterMap);
		} else {
			addTemplate(templateName);
			plainContent = getRawWikiContent(getTemplateNamespace(), templateName, parameterMap);
		}

		if (plainContent != null) {
			StringBuilder templateBuffer = new StringBuilder(plainContent.length());
			TemplateParser.parseRecursive(plainContent.trim(), this, templateBuffer, false, false, parameterMap);
			if (templateCallsCache != null && cacheKey != null) {
				// save this template call in the cache
				String cacheValue = templateBuffer.toString();
				templateCallsCache.put(cacheKey, cacheValue);
				writer.append(cacheValue);
			} else {
				writer.append(templateBuffer);
			}
			return;
		}
		// if no template found insert plain template name string:
		writer.append("{{");
		writer.append(templateName);
		writer.append("}}");
	}

	/**
	 * {@inheritDoc}
	 */
	public TagStack swapStack(TagStack stack) {
		TagStack temp = fTagStack;
		fTagStack = stack;
		return temp;
	}

	/**
	 * {@inheritDoc}
	 */
	public void tearDown() {

	}

	/**
	 * Initialize the wiki model and parse the <code>rawWikiText</code> and return
	 * the parsed node list.
	 * 
	 * @param rawWikiText
	 * @return
	 * @deprecated
	 */
	@Deprecated
	public List<BaseToken> toNodeList(String rawWikiText) {
		initialize();
		if (rawWikiText == null) {
			return new ArrayList<BaseToken>();
		}
		WikipediaParser.parse(rawWikiText, this, true, null);
		fInitialized = false;
		return fTagStack.getNodeList();
	}

	/**
	 * Reduce the current token stack until an allowed parent is at the top of the
	 * stack
	 */
	public void reduceTokenStack(TagToken node) {
		String allowedParents = node.getParents();
		if (allowedParents != null) {
			TagToken tag;
			int index = -1;

			while (stackSize() > 0) {
				tag = peekNode();
				index = allowedParents.indexOf("|" + tag.getName() + "|");
				if (index < 0) {
					popNode();
					if (tag.getName().equals(node.getName())) {
						// for wrong nested HTML tags like <table> <tr><td>number
						// 1<tr><td>number 2</table>
						break;
					}
				} else {
					break;
				}
			}
		} else {
			while (stackSize() > 0) {
				popNode();
			}
		}
	}

	public String getNamespaceName() {
		return fNamespaceName;
	}

	public void setNamespaceName(String namespaceLowercase) {
		if (namespaceLowercase == null) {
			fNamespaceName = "";
			return;
		}
		fNamespaceName = fNamespace.getNamespaceByLowercase(namespaceLowercase);
		if (fNamespaceName == null) {
			fNamespaceName = namespaceLowercase;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public String getImageBaseURL() {
		return "/wiki/${image}";
	}

	/**
	 * {@inheritDoc}
	 */
	public String getWikiBaseURL() {
		return "/wiki/${title}";
	}

	/**
	 * {@inheritDoc}
	 */
	public String getWikiBaseEditURL() {
		if (fLocale != null) {
			String lang = fLocale.getLanguage();
			return "http://" + lang + ".wikipedia.org/w/index.php?title=${title}";
		}
		return "http://en.wikipedia.org/w/index.php?title=${title}";
	}
}
