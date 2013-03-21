/**
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE, version 2.1, dated February 1999.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the latest version of the GNU Lesser General
 * Public License as published by the Free Software Foundation;
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program (LICENSE.txt); if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package info.bliki.wiki.filter;

import info.bliki.wiki.model.IWikiModel;
import info.bliki.wiki.namespaces.INamespace;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * See <a href="http://www.mediawiki.org/wiki/Help:Magic_words">Help:Magic
 * words</a> for a list of Mediawiki magic words.
 */
public class MagicWord {

	// current date values
	public static final String MAGIC_CURRENT_DAY = "CURRENTDAY";

	public static final String MAGIC_CURRENT_DAY2 = "CURRENTDAY2";

	public static final String MAGIC_CURRENT_DAY_NAME = "CURRENTDAYNAME";

	public static final String MAGIC_CURRENT_DAY_OF_WEEK = "CURRENTDOW";

	public static final String MAGIC_CURRENT_MONTH = "CURRENTMONTH";

	public static final String MAGIC_CURRENT_MONTH_ABBR = "CURRENTMONTHABBREV";

	public static final String MAGIC_CURRENT_MONTH_NAME = "CURRENTMONTHNAME";

	public static final String MAGIC_CURRENT_TIME = "CURRENTTIME";

	public static final String MAGIC_CURRENT_HOUR = "CURRENTHOUR";

	public static final String MAGIC_CURRENT_WEEK = "CURRENTWEEK";

	public static final String MAGIC_CURRENT_YEAR = "CURRENTYEAR";

	public static final String MAGIC_CURRENT_TIMESTAMP = "CURRENTTIMESTAMP";

	// local date values
	public static final String MAGIC_LOCAL_DAY = "LOCALDAY";

	public static final String MAGIC_LOCAL_DAY2 = "LOCALDAY2";

	public static final String MAGIC_LOCAL_DAY_NAME = "LOCALDAYNAME";

	public static final String MAGIC_LOCAL_DAY_OF_WEEK = "LOCALDOW";

	public static final String MAGIC_LOCAL_MONTH = "LOCALMONTH";

	public static final String MAGIC_LOCAL_MONTH_ABBR = "LOCALMONTHABBREV";

	public static final String MAGIC_LOCAL_MONTH_NAME = "LOCALMONTHNAME";

	public static final String MAGIC_LOCAL_TIME = "LOCALTIME";

	public static final String MAGIC_LOCAL_HOUR = "LOCALHOUR";

	public static final String MAGIC_LOCAL_WEEK = "LOCALWEEK";

	public static final String MAGIC_LOCAL_YEAR = "LOCALYEAR";

	public static final String MAGIC_LOCAL_TIMESTAMP = "LOCALTIMESTAMP";

	// statistics
	public static final String MAGIC_CURRENT_VERSION = "CURRENTVERSION";

	public static final String MAGIC_NUMBER_ARTICLES = "NUMBEROFARTICLES";

	public static final String MAGIC_NUMBER_PAGES = "NUMBEROFPAGES";

	public static final String MAGIC_NUMBER_FILES = "NUMBEROFFILES";

	public static final String MAGIC_NUMBER_USERS = "NUMBEROFUSERS";

	public static final String MAGIC_NUMBER_ADMINS = "NUMBEROFADMINS";

	public static final String MAGIC_PAGES_IN_CATEGORY = "PAGESINCATEGORY";

	public static final String MAGIC_PAGES_IN_CAT = "PAGESINCAT";

	public static final String MAGIC_PAGES_IN_NAMESPACE = "PAGESINNAMESPACE";

	public static final String MAGIC_PAGES_IN_NAMESPACE_NS = "PAGESINNS";

	public static final String MAGIC_PAGE_SIZE = "PAGESIZE";

	// page values
	public static final String MAGIC_PAGE_NAME = "PAGENAME";

	public static final String MAGIC_PAGE_NAME_E = "PAGENAMEE";

	public static final String MAGIC_SUB_PAGE_NAME = "SUBPAGENAME";

	public static final String MAGIC_SUB_PAGE_NAME_E = "SUBPAGENAMEE";

	public static final String MAGIC_BASE_PAGE_NAME = "BASEPAGENAME";

	public static final String MAGIC_BASE_PAGE_NAME_E = "BASEPAGENAMEE";

	public static final String MAGIC_NAMESPACE = "NAMESPACE";

	public static final String MAGIC_NAMESPACE_E = "NAMESPACEE";

	public static final String MAGIC_FULL_PAGE_NAME = "FULLPAGENAME";

	public static final String MAGIC_FULL_PAGE_NAME_E = "FULLPAGENAMEE";

	public static final String MAGIC_TALK_SPACE = "TALKSPACE";

	public static final String MAGIC_TALK_SPACE_E = "TALKSPACEE";

	public static final String MAGIC_SUBJECT_SPACE = "SUBJECTSPACE";

	public static final String MAGIC_SUBJECT_SPACE_E = "SUBJECTSPACEE";

	public static final String MAGIC_ARTICLE_SPACE = "ARTICLESPACE";

	public static final String MAGIC_ARTICLE_SPACE_E = "ARTICLESPACEE";

	public static final String MAGIC_TALK_PAGE_NAME = "TALKPAGENAME";

	public static final String MAGIC_TALK_PAGE_NAME_E = "TALKPAGENAMEE";

	public static final String MAGIC_SUBJECT_PAGE_NAME = "SUBJECTPAGENAME";

	public static final String MAGIC_SUBJECT_PAGE_NAME_E = "SUBJECTPAGENAMEE";

	public static final String MAGIC_ARTICLE_PAGE_NAME = "ARTICLEPAGENAME";

	public static final String MAGIC_ARTICLE_PAGE_NAME_E = "ARTICLEPAGENAMEE";

	public static final String MAGIC_REVISION_ID = "REVISIONID";

	public static final String MAGIC_REVISION_DAY = "REVISIONDAY";

	public static final String MAGIC_REVISION_DAY2 = "REVISIONDAY2";

	public static final String MAGIC_REVISION_MONTH = "REVISIONMONTH";

	public static final String MAGIC_REVISION_MONTH1 = "REVISIONMONTH1";

	public static final String MAGIC_REVISION_YEAR = "REVISIONYEAR";

	public static final String MAGIC_REVISION_TIMESTAMP = "REVISIONTIMESTAMP";

	public static final String MAGIC_REVISION_USER = "REVISIONUSER";

	public static final String MAGIC_PROTECTION_LEVEL = "PROTECTIONLEVEL";

	public static final String MAGIC_DISPLAY_TITLE = "DISPLAYTITLE";

	public static final String MAGIC_DEFAULT_SORT = "DEFAULTSORT";

	public static final String MAGIC_DEFAULT_SORT_KEY = "DEFAULTSORTKEY";

	public static final String MAGIC_DEFAULT_CATEGORY_SORT = "DEFAULTCATEGORYSORT";

	public static final String MAGIC_SITE_NAME = "SITENAME";

	public static final String MAGIC_SERVER = "SERVER";

	public static final String MAGIC_SCRIPT_PATH = "SCRIPTPATH";

	public static final String MAGIC_SERVER_NAME = "SERVERNAME";

	public static final String MAGIC_STYLE_PATH = "STYLEPATH";

	public static final String MAGIC_CONTENT_LANGUAGE = "CONTENTLANGUAGE";

	public static final String MAGIC_CONTENT_LANG = "CONTENTLANG";

	protected final static Set<String> MAGIC_WORDS = new HashSet<String>(100, 0.75f);

	protected static final String TEMPLATE_INCLUSION = "template-inclusion";

	static {
		// current date values
		MAGIC_WORDS.add(MAGIC_CURRENT_DAY);
		MAGIC_WORDS.add(MAGIC_CURRENT_DAY2);
		MAGIC_WORDS.add(MAGIC_CURRENT_DAY_NAME);
		MAGIC_WORDS.add(MAGIC_CURRENT_DAY_OF_WEEK);
		MAGIC_WORDS.add(MAGIC_CURRENT_MONTH);
		MAGIC_WORDS.add(MAGIC_CURRENT_MONTH_ABBR);
		MAGIC_WORDS.add(MAGIC_CURRENT_MONTH_NAME);
		MAGIC_WORDS.add(MAGIC_CURRENT_TIME);
		MAGIC_WORDS.add(MAGIC_CURRENT_HOUR);
		MAGIC_WORDS.add(MAGIC_CURRENT_WEEK);
		MAGIC_WORDS.add(MAGIC_CURRENT_YEAR);
		MAGIC_WORDS.add(MAGIC_CURRENT_TIMESTAMP);
		// local date values
		MAGIC_WORDS.add(MAGIC_LOCAL_DAY);
		MAGIC_WORDS.add(MAGIC_LOCAL_DAY2);
		MAGIC_WORDS.add(MAGIC_LOCAL_DAY_NAME);
		MAGIC_WORDS.add(MAGIC_LOCAL_DAY_OF_WEEK);
		MAGIC_WORDS.add(MAGIC_LOCAL_MONTH);
		MAGIC_WORDS.add(MAGIC_LOCAL_MONTH_ABBR);
		MAGIC_WORDS.add(MAGIC_LOCAL_MONTH_NAME);
		MAGIC_WORDS.add(MAGIC_LOCAL_TIME);
		MAGIC_WORDS.add(MAGIC_LOCAL_HOUR);
		MAGIC_WORDS.add(MAGIC_LOCAL_WEEK);
		MAGIC_WORDS.add(MAGIC_LOCAL_YEAR);
		MAGIC_WORDS.add(MAGIC_LOCAL_TIMESTAMP);
		// statistics
		MAGIC_WORDS.add(MAGIC_CURRENT_VERSION);
		MAGIC_WORDS.add(MAGIC_NUMBER_ARTICLES);
		MAGIC_WORDS.add(MAGIC_NUMBER_PAGES);
		MAGIC_WORDS.add(MAGIC_NUMBER_FILES);
		MAGIC_WORDS.add(MAGIC_NUMBER_USERS);
		MAGIC_WORDS.add(MAGIC_NUMBER_ADMINS);
		MAGIC_WORDS.add(MAGIC_PAGES_IN_CATEGORY);
		MAGIC_WORDS.add(MAGIC_PAGES_IN_CAT);
		MAGIC_WORDS.add(MAGIC_PAGES_IN_NAMESPACE);
		// MAGIC_WORDS.add(MAGIC_PAGES_IN_NAMESPACE_NS);
		// MAGIC_WORDS.add(MAGIC_PAGES_IN_NAMESPACE_NS_R);
		MAGIC_WORDS.add(MAGIC_PAGES_IN_NAMESPACE_NS);
		MAGIC_WORDS.add(MAGIC_PAGE_SIZE);
		// page values
		MAGIC_WORDS.add(MAGIC_PAGE_NAME);
		MAGIC_WORDS.add(MAGIC_PAGE_NAME_E);
		MAGIC_WORDS.add(MAGIC_SUB_PAGE_NAME);
		MAGIC_WORDS.add(MAGIC_SUB_PAGE_NAME_E);
		MAGIC_WORDS.add(MAGIC_BASE_PAGE_NAME);
		MAGIC_WORDS.add(MAGIC_BASE_PAGE_NAME_E);
		MAGIC_WORDS.add(MAGIC_NAMESPACE);
		MAGIC_WORDS.add(MAGIC_NAMESPACE_E);
		MAGIC_WORDS.add(MAGIC_FULL_PAGE_NAME);
		MAGIC_WORDS.add(MAGIC_FULL_PAGE_NAME_E);
		MAGIC_WORDS.add(MAGIC_TALK_SPACE);
		MAGIC_WORDS.add(MAGIC_TALK_SPACE_E);
		MAGIC_WORDS.add(MAGIC_SUBJECT_SPACE);
		MAGIC_WORDS.add(MAGIC_SUBJECT_SPACE_E);
		MAGIC_WORDS.add(MAGIC_ARTICLE_SPACE);
		MAGIC_WORDS.add(MAGIC_ARTICLE_SPACE_E);
		MAGIC_WORDS.add(MAGIC_TALK_PAGE_NAME);
		MAGIC_WORDS.add(MAGIC_TALK_PAGE_NAME_E);
		MAGIC_WORDS.add(MAGIC_SUBJECT_PAGE_NAME);
		MAGIC_WORDS.add(MAGIC_SUBJECT_PAGE_NAME_E);
		MAGIC_WORDS.add(MAGIC_ARTICLE_PAGE_NAME);
		MAGIC_WORDS.add(MAGIC_ARTICLE_PAGE_NAME_E);
		MAGIC_WORDS.add(MAGIC_REVISION_ID);
		MAGIC_WORDS.add(MAGIC_REVISION_DAY);
		MAGIC_WORDS.add(MAGIC_REVISION_DAY2);
		MAGIC_WORDS.add(MAGIC_REVISION_MONTH);
		MAGIC_WORDS.add(MAGIC_REVISION_MONTH1);
		MAGIC_WORDS.add(MAGIC_REVISION_YEAR);
		MAGIC_WORDS.add(MAGIC_REVISION_TIMESTAMP);
		MAGIC_WORDS.add(MAGIC_REVISION_USER);
		MAGIC_WORDS.add(MAGIC_PROTECTION_LEVEL);
		MAGIC_WORDS.add(MAGIC_DISPLAY_TITLE);
		MAGIC_WORDS.add(MAGIC_DEFAULT_SORT);
		MAGIC_WORDS.add(MAGIC_DEFAULT_SORT_KEY);
		MAGIC_WORDS.add(MAGIC_DEFAULT_CATEGORY_SORT);
		MAGIC_WORDS.add(MAGIC_SITE_NAME);
		MAGIC_WORDS.add(MAGIC_SERVER);
		MAGIC_WORDS.add(MAGIC_SCRIPT_PATH);
		MAGIC_WORDS.add(MAGIC_SERVER_NAME);
		MAGIC_WORDS.add(MAGIC_STYLE_PATH);
		MAGIC_WORDS.add(MAGIC_CONTENT_LANGUAGE);
		MAGIC_WORDS.add(MAGIC_CONTENT_LANG);
	}

	/**
	 * Determine if a template name corresponds to a magic word requiring special
	 * handling. See <a
	 * href="http://www.mediawiki.org/wiki/Help:Magic_words">Help:Magic words</a>
	 * for a list of Mediawiki magic words.
	 */
	public static boolean isMagicWord(String name) {
		return MAGIC_WORDS.contains(name);
	}

	/**
	 * Process a magic word, returning the value corresponding to the magic word
	 * value. See <a
	 * href="http://www.mediawiki.org/wiki/Help:Magic_words">Help:Magic words</a>
	 * for a list of Mediawiki magic words.
	 */
	public static String processMagicWord(String name, String parameter, IWikiModel model) {
		SimpleDateFormat formatter = model.getSimpleDateFormat();
		Date current = model.getCurrentTimeStamp();
		if (current == null) {
			// set a default value
			current = new Date(System.currentTimeMillis());
		}
		// local date values
		if (name.equals(MAGIC_LOCAL_DAY)) {
			formatter.applyPattern("d");
			return formatter.format(current);
		} else if (name.equals(MAGIC_LOCAL_DAY2)) {
			formatter.applyPattern("dd");
			return formatter.format(current);
		} else if (name.equals(MAGIC_LOCAL_DAY_NAME)) {
			formatter.applyPattern("EEEE");
			return formatter.format(current);
		} else if (name.equals(MAGIC_LOCAL_DAY_OF_WEEK)) {
			formatter.applyPattern("F");
			return formatter.format(current);
		} else if (name.equals(MAGIC_LOCAL_MONTH)) {
			formatter.applyPattern("MM");
			return formatter.format(current);
		} else if (name.equals(MAGIC_LOCAL_MONTH_ABBR)) {
			formatter.applyPattern("MMM");
			return formatter.format(current);
		} else if (name.equals(MAGIC_LOCAL_MONTH_NAME)) {
			formatter.applyPattern("MMMM");
			return formatter.format(current);
		} else if (name.equals(MAGIC_LOCAL_TIME)) {
			formatter.applyPattern("HH:mm");
			return formatter.format(current);
		} else if (name.equals(MAGIC_LOCAL_HOUR)) {
			formatter.applyPattern("HH");
			return formatter.format(current);
		} else if (name.equals(MAGIC_LOCAL_WEEK)) {
			formatter.applyPattern("w");
			return formatter.format(current);
		} else if (name.equals(MAGIC_LOCAL_YEAR)) {
			formatter.applyPattern("yyyy");
			return formatter.format(current);
		} else if (name.equals(MAGIC_LOCAL_TIMESTAMP)) {
			formatter.applyPattern("yyyyMMddHHmmss");
			return formatter.format(current);
		}
		// current date values

		if (name.equals(MAGIC_CURRENT_DAY)) {
			formatter.applyPattern("d");
			return formatter.format(current);
		} else if (name.equals(MAGIC_CURRENT_DAY2)) {
			formatter.applyPattern("dd");
			return formatter.format(current);
		} else if (name.equals(MAGIC_CURRENT_DAY_NAME)) {
			formatter.applyPattern("EEEE");
			return formatter.format(current);
		} else if (name.equals(MAGIC_CURRENT_DAY_OF_WEEK)) {
			formatter.applyPattern("F");
			return formatter.format(current);
		} else if (name.equals(MAGIC_CURRENT_MONTH)) {
			formatter.applyPattern("MM");
			return formatter.format(current);
		} else if (name.equals(MAGIC_CURRENT_MONTH_ABBR)) {
			formatter.applyPattern("MMM");
			return formatter.format(current);
		} else if (name.equals(MAGIC_CURRENT_MONTH_NAME)) {
			formatter.applyPattern("MMMM");
			return formatter.format(current);
		} else if (name.equals(MAGIC_CURRENT_TIME)) {
			formatter.applyPattern("HH:mm");
			return formatter.format(current);
		} else if (name.equals(MAGIC_CURRENT_HOUR)) {
			formatter.applyPattern("HH");
			return formatter.format(current);
		} else if (name.equals(MAGIC_CURRENT_WEEK)) {
			formatter.applyPattern("w");
			return formatter.format(current);
		} else if (name.equals(MAGIC_CURRENT_YEAR)) {
			formatter.applyPattern("yyyy");
			return formatter.format(current);
		} else if (name.equals(MAGIC_CURRENT_TIMESTAMP)) {
			formatter.applyPattern("yyyyMMddHHmmss");
			return formatter.format(current);
		}

		if (name.equals(MAGIC_PAGE_NAME)) {
			String temp = model.getPageName();
			if (temp != null) {
				if (parameter.length() > 0) {
					return parameter;
				}
				return temp;
			}
		} else if (name.equals(MAGIC_NAMESPACE)) {
			if (parameter.length() > 0) {
				int indx = parameter.indexOf(':');
				if (indx >= 0) {
					String subStr = parameter.substring(0, indx);
					return model.getNamespace().getNamespace(subStr);
				}
				return "";
			}
			String temp = model.getNamespaceName();
			if (temp != null) {
				return temp;
			}
		} else if (name.equals(MAGIC_FULL_PAGE_NAME)) {
			String temp = model.getPageName();
			if (temp != null) {
				if (parameter.length() > 0) {
					return parameter;
				}
				return temp;
			}
		} else if (name.equals(MAGIC_TALK_PAGE_NAME)) {
			String temp = model.getPageName();
			if (temp != null) {
				INamespace ns = model.getNamespace();
				if (parameter.length() > 0) {
					String namespace = parameter;
					int index = namespace.indexOf(':');
					if (index > 0) {
						// {{TALKPAGENAME:Template:Sandbox}}
						String rest = namespace.substring(index + 1);
						namespace = namespace.substring(0, index);
						String talkspace = ns.getTalkspace(namespace);
						if (talkspace != null) {
							return talkspace + ":" + rest;
						}
					}
					return ns.getTalk() + ":" + parameter;
				}
				return ns.getTalk() + temp;
			}
		}

		return name;
	}
}
