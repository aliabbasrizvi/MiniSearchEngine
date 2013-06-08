package minisearch;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.TreeMap;

import Jama.Matrix;

public class RankPage {
	Matrix pageConnections, pageWeightMatrix;
	final double dampingFactor = 0.85;
	int numberOfPages;
	LinkExtractor linkExt;
	TreeMap<Integer, PageInfo> pageInfoMap;
	
	/**
	 * 
	 * @param linksFilePath Path to the links file
	 * @throws IOException
	 */
	public RankPage(String linksFilePath) throws IOException {
		linkExt = new LinkExtractor(linksFilePath);
		System.out.println("Initializing....\nSetting up indices and computing PageRanks");
		linkExt.parseLinks();
		numberOfPages = linkExt.getLinkCount();
		
		initializePageWeightMatrix();
		pageConnections = linkExt.getLinkConnections();
		pageConnections = normalizePageConnectionsMatrix(pageConnections);
	}
		
	public RankPage() {
		System.out.println("Constructing indices and PageRanks from the metadata file");
	}

	/**
	 * Function to compute PageRanks by iteration
	 */
	public void generatePageRanks() {
		pageConnections = pageConnections.times(dampingFactor);
		Matrix z0 = new Matrix(numberOfPages, 1, 1);
		while(true) {
			Matrix tempPageWeightMatrix = pageConnections.times(pageWeightMatrix);
			tempPageWeightMatrix = tempPageWeightMatrix.plus(z0.times((double)(1 - dampingFactor)));
			if (hasConverged(tempPageWeightMatrix, pageWeightMatrix)) 
				break;
			
			pageWeightMatrix = tempPageWeightMatrix;
		}
		setPageRanks();
	}
	
	/**
	 * Function to set PageRank in each PageInfo object in the pageInfoMap
	 */
	private void setPageRanks() {
		pageInfoMap = linkExt.getPageInformationMap();
		
		for (int i = 0; i < numberOfPages; i++) {
			pageInfoMap.get(i).setPageRank(pageWeightMatrix.get(i, 0));
		}
	}
	
	/**
	 * 
	 * @param metadataFilePath Path to the metadata file which is written by this function
	 * @throws IOException
	 */
	public void generateMetadataFile(String metadataFilePath) throws IOException {
		FileWriter fstream  = new FileWriter(metadataFilePath);
		BufferedWriter bw = new BufferedWriter(fstream);
		for (int pageNum : pageInfoMap.keySet()) {
			bw.write(Integer.toString(pageNum) + ":" + pageInfoMap.get(pageNum).getTitle() + "\n");
			bw.write("PageRank:" + Double.toString(pageInfoMap.get(pageNum).getPageRank()) + "\n");
			bw.write("Link:" + pageInfoMap.get(pageNum).getLink() + "\n");
			bw.write("Anchors are as under:\n");
			HashMap<Integer, String> anchors = pageInfoMap.get(pageNum).getAnchor();
			for (int pageId : anchors.keySet()) {
				bw.write(Integer.toString(pageId) + ":" + anchors.get(pageId) + "\n");
			}
			
			bw.write("***********************************************************\n");
		}
		bw.close();
	}
	
	/**
	 * 
	 * @param metadataFilePath Path to metadata file which is read in this function to populate PageInfo map
	 * @throws IOException
	 */
	public void readMetadataFile(String metadataFilePath) throws IOException {
		FileReader fstream = new FileReader(metadataFilePath);
		BufferedReader br = new BufferedReader(fstream);
		String line;
		PageInfo pageInfo = null;
		int count = 0, flag = 0, lineCount = 0;
		pageInfoMap = new TreeMap<Integer, PageInfo>();
		
		while((line = br.readLine()) != null) {
			if (line.startsWith("*"))  
				flag = 0;
			else {
				if (flag == 0) {
					flag = 1;
					lineCount = 0;
					pageInfo = new PageInfo(count++);
					pageInfoMap.put(count - 1, pageInfo);
				}
				
				if (lineCount == 0) {
					String temp[] = line.split(":");
					String title = "";
					if (temp.length >= 2) {
						for (int i = 1; i < temp.length; i++)
							if (i == (temp.length - 1))
								title+=temp[i];
							else
								title+=temp[i]+":";
						pageInfoMap.get(count - 1).setTitle(title);
					}
					else
						pageInfoMap.get(count - 1).setTitle("");
				} else if (lineCount == 1) {
					String temp[] = line.split(":");
					pageInfoMap.get(count - 1).setPageRank(Double.parseDouble(temp[1]));
				} else if (lineCount == 2) {
					String temp[] = line.split(":");
					pageInfoMap.get(count - 1).setLink(temp[1] + ":" + temp[2]);
				} else if (lineCount > 3) {
					String temp[] = line.split(":");
					if (temp.length == 2)
						pageInfoMap.get(count - 1).setAnchor(Integer.parseInt(temp[0]), temp[1]);
					else 
						pageInfoMap.get(count - 1).setAnchor(Integer.parseInt(temp[0]), "");
				}
				lineCount++;
			}
		}
		br.close();
	}
	
	/**
	 * 
	 * @param matrixK		  Matrix after the iteration 
	 * @param matrixKminus1   Matrix before the iteration
	 * @return				  Boolean value representing if the matrices have converged or not
	 */
	private boolean hasConverged(Matrix matrixK, Matrix matrixKminus1) {
		int i;
		
		for (i = 0; i < numberOfPages; i++) {
			if (matrixK.get(i, 0) != matrixKminus1.get(i, 0))
				return false;
		}
		
		return true;
	}
		
	/**
	 * Function to set the weights of all pages to 1 initially
	 */
	private void initializePageWeightMatrix() {
		pageWeightMatrix = new Matrix(numberOfPages, 1, 1);
	}
	
	/**
	 * 
	 * @param matrix Path to the matrix consisting of information about connection between the various links
	 * @return Normalized matrix depending upon number of connections from a particular link
	 */
	private Matrix normalizePageConnectionsMatrix(Matrix matrix) {
		int i, j;
		int[] linkCounts = new int[numberOfPages];
		
		for (j = 0; j < matrix.getColumnDimension(); j++) {
			for (i = 0; i < matrix.getRowDimension(); i++) {
				linkCounts[j]+=matrix.get(i, j);
			}
		}
		
		for (j = 0; j < matrix.getColumnDimension(); j++) {
			for (i = 0; i < matrix.getRowDimension(); i++) {
				if (linkCounts[j] != 0)
					matrix.set(i, j, matrix.get(i, j)/linkCounts[j]);
				else {
					if (i != j)
						matrix.set(i, j, 1/numberOfPages);
					else
						matrix.set(i, j, 0);
				}
			}
		}
		
		return matrix;
	}
	
	/**
	 * 
	 * @return TreeMap holding index information about the various pages 
	 */
	public TreeMap<Integer, PageInfo> getPageInfoMap() {
		return pageInfoMap;
	}
}
