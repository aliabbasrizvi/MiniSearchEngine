package minisearch;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import Jama.Matrix;

public class LinkExtractor {
	String linkFilePath;
	HashMap<String, Integer> links;
	TreeMap<Integer, PageInfo> pageInfoMap;
	ArrayList<String> linksList;
	double linkConnection[][]; 
	int linkCount;

	/**
	 * 
	 * @param linksFile Path to the links file
	 */
	public LinkExtractor(String linksFile) {
		linkFilePath = linksFile;
		linkCount = 0;
	}
	
	/**
	 * 
	 * @return Returns the number of links read
	 */
	public int getLinkCount() {
		return linkCount;
	}
	
	/**
	 * 
	 * @return Matrix representing connection between the different links
	 */
	public Matrix getLinkConnections() {
		return (new Matrix(linkConnection));
	}
	
	/**
	 * 
	 * @return Returns the map consisting of all page relevant information useful in building the index
	 */
	public TreeMap<Integer, PageInfo> getPageInformationMap() {
		return pageInfoMap;
	}

	/**
	 * Function to parse the various links
	 * @throws IOException
	 */
	public void parseLinks() throws IOException {
		FileInputStream fstream = null;
		String line;
		int count = 0;

		fstream = new FileInputStream(linkFilePath);
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));

		links = new HashMap<String, Integer>();
		linksList = new ArrayList<String>();
		while((line = br.readLine()) != null) {
			String temp[] = line.trim().split(",");
			
			if (temp.length != 2) {
				continue;
			}
			
			if (temp[1].endsWith("/"))
				temp[1] = temp[1].substring(0, temp[1].length() - 1);

			links.put(temp[1], linkCount++);
			linksList.add(temp[1]);
		}

		br.close();
		linkConnection = new double[linkCount][linkCount];
		pageInfoMap = new TreeMap<Integer, PageInfo>();
		
		for (count = 0; count < linkCount; count++) {
			PageInfo pageInfo = new PageInfo(count);
			pageInfoMap.put(count, pageInfo);
		}
		
		count = 0;
		for (String link: linksList) {
			if (link != null) {
				readContent(link, count++);
			} else {
				count++;
			}
		}
	}

	/**
	 * 
	 * @param link           Link whose content is to be read
	 * @param pageNum		 Number representing the link. Used to retrieve the corresponding PageInfo object.
	 * @throws IOException
	 */
	private void readContent(String link, int pageNum) throws IOException {
		String httpTag = "http", hashTag = "#";
		System.setProperty("http.agent", "Mozilla/5.0 (compatible)");
		String extractedLink = null;
		Document doc = (Document) Jsoup.connect(link).get();
		
		Elements pageLinks = doc.select("a[href]");
		pageInfoMap.get(pageNum).setTitle(doc.title());
		pageInfoMap.get(pageNum).setLink(link);
		
		for (Element referenceLink : pageLinks) {
			extractedLink = referenceLink.attr("abs:href");
			extractedLink = extractedLink.replace(httpTag+"s", httpTag);
			extractedLink = extractedLink.replace(httpTag+"://m", httpTag+"://www");
			if (extractedLink.endsWith("/"))
				extractedLink = extractedLink.substring(0, extractedLink.length() - 1);
			if (!extractedLink.startsWith(httpTag + "://www"))
				extractedLink = extractedLink.replace(httpTag + "://", (httpTag + "://www."));
			if (extractedLink.contains(hashTag)) {
				String splitArgs[] = extractedLink.split(hashTag);
				extractedLink = splitArgs[0];
			}
			if (!link.equals(extractedLink) && links.containsKey(extractedLink)) {
				String anchorText = referenceLink.text();
				pageInfoMap.get(links.get(extractedLink)).setAnchor(pageNum, anchorText);					
				linkConnection[links.get(extractedLink)][links.get(link)] = 1;
			}
		}
		
		Elements pageMedia = doc.select("[src]");
		for (Element referenceMedia : pageMedia) {
			if (referenceMedia.tagName().equals("img")) {
				extractedLink = referenceMedia.attr("abs:src");
				extractedLink = extractedLink.replace(httpTag+"s", httpTag);
				extractedLink = extractedLink.replace(httpTag+"://m", httpTag+"://www");
				if (extractedLink.endsWith("/"))
					extractedLink = extractedLink.substring(0, extractedLink.length() - 1);
				if (!extractedLink.startsWith(httpTag + "://www"))
					extractedLink = extractedLink.replace(httpTag + "://", (httpTag + "://www."));
				if (!link.equals(extractedLink) && links.containsKey(extractedLink)) {
					String anchorText = referenceMedia.attr("alt");
					pageInfoMap.get(links.get(extractedLink)).setAnchor(pageNum, anchorText);					
					linkConnection[links.get(extractedLink)][links.get(link)] = 1;
				}
			}
		}
	}
}
