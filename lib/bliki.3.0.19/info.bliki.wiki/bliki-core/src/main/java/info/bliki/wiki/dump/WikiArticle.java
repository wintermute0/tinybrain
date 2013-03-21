package info.bliki.wiki.dump;

import info.bliki.wiki.namespaces.INamespace;

/**
 * Represents a single wiki page from a Mediawiki dump.
 * 
 */
public class WikiArticle {
	private String id = null;
	private Integer integerNamespace = 0;
	private String namespace = "";
	private String revisionId = null;
	private String text;
	private String timeStamp;

	private String title;

	public WikiArticle() {

	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * Get the integer key of the namespace or <code>0</code> if no namespace is
	 * associated. For example in an english Mediawiki installation <i>10</i> is
	 * typically the <i>Template</i> namespace and <i>14</i> is typically the
	 * <i>Category</i> namespace.
	 * 
	 * @return the integerNamespace
	 */
	public Integer getIntegerNamespace() {
		return integerNamespace;
	}

	/**
	 * @return the namespace.
	 */
	public String getNamespace() {
		return namespace;
	}

	/**
	 * @return the revisionId
	 */
	public String getRevisionId() {
		return revisionId;
	}

	public String getText() {
		return text;
	}

	public String getTimeStamp() {
		return timeStamp;
	}

	public String getTitle() {
		return title;
	}

	/**
	 * Does the title belong to the <i>Category</i> namespace?
	 * 
	 * @return
	 */
	public boolean isCategory() {
		return integerNamespace.equals(INamespace.CATEGORY_NAMESPACE_KEY);
	}

	public boolean isFile() {
		return integerNamespace.equals(INamespace.FILE_NAMESPACE_KEY);
	}

	/**
	 * &quot;Real&quot; content articles (i.e. the title has no namespace prefix)?
	 * 
	 * @return
	 */
	public boolean isMain() {
		return integerNamespace.equals(INamespace.MAIN_NAMESPACE_KEY);
	}

	public boolean isProject() {
		return integerNamespace.equals(INamespace.PROJECT_NAMESPACE_KEY);
	}

	/**
	 * Does the title belong to the <i>Template</i> namespace?
	 * 
	 * @return
	 */
	public boolean isTemplate() {
		return integerNamespace.equals(INamespace.TEMPLATE_NAMESPACE_KEY);
	}

	/**
	 * The ID of the wiki article to set.
	 * 
	 * @param id
	 *          the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @param integerNamespace
	 *          the integerNamespace to set
	 */
	public void setIntegerNamespace(Integer integerNamespace) {
		this.integerNamespace = integerNamespace;
	}

	/**
	 * @param namespace
	 *          the namespace to set
	 */
	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	/**
	 * The ID of the revision of the wiki article to set.
	 * 
	 * @param revisionId
	 *          the revisisonId to set
	 */
	public void setRevisionId(String revisionId) {
		this.revisionId = revisionId;
	}

	public void setText(String newText) {
		text = newText;
	}

	public void setTimeStamp(String timeStamp) {
		this.timeStamp = timeStamp;
	}

	/**
	 * 
	 * @param newTitle
	 * @deprecated
	 */
	@Deprecated
	public void setTitle(String newTitle) {
		setTitle(newTitle, null);
	}

	public void setTitle(String newTitle, Siteinfo siteinfo) {
		title = newTitle;
		if (siteinfo != null) {
			int index = newTitle.indexOf(":");
			if (index > 0) {
				Integer key = siteinfo.getIntegerNamespace(newTitle.substring(0, index));
				if (key != null) {
					integerNamespace = key;
					setNamespace(siteinfo.getNamespace(key));
				}
			}

		}
	}

	@Override
	public String toString() {
		return title + "\n" + text;
	}
}
