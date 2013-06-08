package minisearch;

import java.util.HashMap;

public class PageInfo {
	String pageTitle, pageLink;
	int pageID;
	HashMap<Integer, String> anchorText = null;
	double pageRank;
	
	/**
	 * 
	 * @param pageNum A page number used to recognize each link. Assigned in order in which the links are read.
	 */
	public PageInfo(int pageNum) {
		pageID = pageNum;
		anchorText = new HashMap<Integer, String>();
	}

	/**
	 * 
	 * @param title The page title for the link
	 */
	public void setTitle(String title) {
		pageTitle = title;
	}
	
	/**
	 * 
	 * @return Page title
	 */
	public String getTitle() {
		return pageTitle;
	}
	
	/**
	 * 
	 * @param pageNum		Link number denoting the page from which a link exists to this page
	 * @param anchorString  String representing the anchor text
	 */
	public void setAnchor(int pageNum, String anchorString) {
		if (!anchorText.containsKey(pageNum))
			anchorText.put(pageNum, anchorString);
	}
	
	/**
	 * 
	 * @return Mapping between the page number and the anchor text
	 */
	public HashMap<Integer, String> getAnchor() {
		return anchorText;
	}
		
	/**
	 * 
	 * @param pageRankVal PageRank value which is set in this method
	 */
	public void setPageRank(double pageRankVal) {
		pageRank = pageRankVal;
	}
	
	/**
	 * 
	 * @return PageRank value for the link
	 */
	public double getPageRank() {
		return pageRank;
	}
		
	/**
	 * 
	 * @param link URL for the page
	 */
	public void setLink(String link) {
		pageLink = link;
	}
	
	/**
	 * 
	 * @return URL for the page
	 */
	public String getLink() {
		return pageLink;
	}
}
