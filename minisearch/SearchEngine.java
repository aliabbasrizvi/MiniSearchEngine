package minisearch;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class SearchEngine {
	RankPage rp;
	TreeMap<Integer, PageInfo> pageInfo;
	TreeMap<String, ArrayList<Integer>> index;
	int snippetSize = 20;
	
	/**
	 * 
	 * @param metdataFilePath  Path to the metadata file from where index can be prepared
	 * @throws IOException
	 */
	SearchEngine(String metdataFilePath) throws IOException {
		rp = new RankPage();
		rp.readMetadataFile(metdataFilePath);
	}
	
	/**
	 * 
	 * @param linksFilePath    Path to the file which consists of all the links in the system
	 * @param metadataDirPath  Path to the directory where the metadata file metadata.txt would be created
	 * @throws IOException
	 */
	SearchEngine(String linksFilePath, String metadataDirPath) throws IOException {
		rp = new RankPage(linksFilePath);
		rp.generatePageRanks();
		rp.generateMetadataFile(metadataDirPath + "/metadata.txt");
	}
	
	/**
	 * Function to handle the user's query by finding out relevant links and ranking them
	 * @param query The user's search term. If there are multiple terms then only the first term is used. 
	 * @throws IOException
	 */
	public void handleUserQuery(String query) throws IOException {
		ArrayList<Integer> pageLists;
		String splitArgs[] = query.split("\\s");
		int i;
		
		if (splitArgs.length == 0) {
			System.out.println("No query entered. Enter some query.");
			return;
		}
			
		query = splitArgs[0];
		pageLists = getRelevantPages(query);
		if (pageLists == null) {
			System.out.println("Term does not exist. Please modify your search query and try again.");
		} else {
			TreeMap<Integer, Double> pageRanks = new TreeMap<Integer, Double>();
			for (i = 0; i < pageLists.size(); i++) {
				pageRanks.put(pageLists.get(i), pageInfo.get(pageLists.get(i)).getPageRank());
			}
			
			for (i = 0; i < pageLists.size(); i++) {
				int page = getMaxRank(pageRanks);
				System.out.println((i + 1) + ". " + pageInfo.get(page).getTitle() + "\n" + pageInfo.get(page).getLink() + "\nPageRank: " + pageInfo.get(page).getPageRank() + "\n");
				displayContent(pageInfo.get(page).getLink(), query);
				System.out.println();
				pageRanks.remove(page);
			}
		}
	}
	
	/**
	 * Function to retrieve page with highest rank
	 * @param pageRanks Map consisting of PageRanks
	 * @return
	 */
	private int getMaxRank(TreeMap<Integer, Double>pageRanks) {
		double maxPageRank = -1;
		int pageNo = -1;
		for (Integer pageNum : pageRanks.keySet()) {
			if (pageRanks.get(pageNum) > maxPageRank) {
				maxPageRank = pageRanks.get(pageNum);
				pageNo = pageNum;
			}
		}
		return pageNo;
	}
	
	/**
	 * 
	 * @param query Term representing the user's query
	 * @return List of numbers representing the various pages which are relevant to the query
	 */
	private ArrayList<Integer> getRelevantPages(String query) {
		return index.get(query);
	}
	
	/**
	 * 
	 * @param link URL of the page whose content is to be displayed
	 * @throws IOException
	 */
	private void displayContent(String link, String query) throws IOException {
		System.setProperty("http.agent", "Mozilla/5.0 (compatible)");
		Document doc = (Document) Jsoup.connect(link).get();
		String content = doc.body().text();
		int i, j, k;
		
		if (!content.toLowerCase().contains(query.toLowerCase())) {
			String displayContent[] = content.split("\\s");
			for (i = 0; i < displayContent.length && i < snippetSize; i++)
				System.out.print(displayContent[i] + " ");
		} else {
			k = snippetSize;
			String splitArgs[] = content.toLowerCase().split(query.toLowerCase());
			for (i = 0; i < splitArgs.length && k > 0; i++) {
				String displayContent[] = splitArgs[i].split("\\s");
				if (displayContent.length > snippetSize/2) {
					for (j = snippetSize/2; j >= 0 && k > 0; j--, k--)
						System.out.print(displayContent[displayContent.length - j - 1] + " ");
					if (j == -1)
						System.out.print(query + " ");
				} else {
					for (j = 0; j < displayContent.length && k > 0; j++, k--)
						System.out.print(displayContent[j] + " ");
					if (k != 0)
						System.out.print(query + " ");
				}	
			}
		}
		System.out.println();
	}
	
	/**
	 * Function to prepare a mapping between the various terms and the list of links which are relevant to them
	 */
	public void prepareIndex() {
		String title, splitArgs[];
		HashMap<Integer, String> anchorTexts;
		pageInfo = rp.getPageInfoMap();
		index = new TreeMap<String, ArrayList<Integer>>();
		int i;
		
		for (Integer pageNum : pageInfo.keySet()) {
			title = pageInfo.get(pageNum).getTitle();
			anchorTexts = pageInfo.get(pageNum).getAnchor();
			
			splitArgs = title.split("\\s");
			
			for (i = 0; i < splitArgs.length; i++) {
				if (!index.containsKey(splitArgs[i].toLowerCase())) {
					ArrayList<Integer> termList = new ArrayList<Integer>();
					termList.add(pageNum);
					index.put(splitArgs[i].toLowerCase(), termList);
				} else {
					if (!index.get(splitArgs[i].toLowerCase()).contains(pageNum))
						index.get(splitArgs[i].toLowerCase()).add(pageNum);
				}
			}
			
			for (Integer pageNo : anchorTexts.keySet()) {
				splitArgs = anchorTexts.get(pageNo).split("\\s");

				for (i = 0; i < splitArgs.length; i++) {
					if (!index.containsKey(splitArgs[i].toLowerCase())) {
						ArrayList<Integer> termList = new ArrayList<Integer>();
						termList.add(pageNum);
						index.put(splitArgs[i].toLowerCase(), termList);
					} else {
						if (!index.get(splitArgs[i].toLowerCase()).contains(pageNum))
							index.get(splitArgs[i].toLowerCase()).add(pageNum);
					}
				}				
			}
		}
	}
	
	public static void main (String args[]) throws IOException {
		SearchEngine srchEng = null;
		if (args.length == 1) {
			if (new File(args[0]).isFile())
				srchEng = new SearchEngine(args[0]);
			else {
				System.out.println("Invalid file. EXITING.");
				System.exit(1);
			}
		}
		else if (args.length == 2) {
			if (new File(args[0]).isFile() && new File(args[1]).isDirectory())
				srchEng = new SearchEngine(args[0], args[1]);
			else {
				System.out.println("Invalid file and/or directory. EXITING.");
				System.exit(1);
			}
		}
		else {
			System.out.println("Incorrect number parameters entered. EXITING.");
			System.exit(1);
		}
		
		srchEng.prepareIndex();
		System.out.println("System is now ready to accept queries");
		
		while (true) {
			System.out.print("\n\nEnter your query: ");
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			String userQuery = br.readLine();
			if (userQuery.equals("ZZZ"))
				break;
			if (userQuery.equals("")) {
				System.out.println("No query entered. Enter some query.");
				continue;
			}
			srchEng.handleUserQuery(userQuery.toLowerCase());
		}
		
		System.out.println("\nThank you for trying out the system.");
	}
}
