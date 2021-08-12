package eu.ldbc.semanticpublishing.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A utility class for various operations on file names or files.
 */
public class FileUtils {

	/**
	 * A recursive method for collecting a list of all files with a given file extension.
	 * Will scan all sub-folders recursively.
	 * 
	 * @param startFolder - where to start from
	 * @param collectedFilesList - collected list of <String>
	 * @param extensionFilter - if set or "*" or empty - no filtering is applied
	 */
	public static void collectFilesList(String startFolder, List<String> collectedFilesList, String fileExtFilter, boolean recurseFolders) throws IOException {
		File file = new File(startFolder);
		File[] filesList = file.listFiles();
		
		for (File f : filesList) {
			if (f.isDirectory() && recurseFolders) {
				collectFilesList(f.getAbsolutePath(), collectedFilesList, fileExtFilter, recurseFolders);
			} else {
				//no filter
				if (fileExtFilter.isEmpty() || fileExtFilter.equals("*")) {
					collectedFilesList.add(f.getCanonicalPath());
				} else {
					if (fileExtFilter.equalsIgnoreCase(getFileExtension(f))) {
						collectedFilesList.add(f.getCanonicalPath());
					}
				}
			}
		}
	}
	/**
	 * A recursive method for collecting a list of all files with a given file extension.
	 * Will scan all sub-folders recursively.
	 * 
	 * @param startFolder - where to start from
	 * @param collectedFilesList - collected list of <File>
	 * @param extensionFilter - if set or "*" or empty - no filtering is applied
	 */	
	public static void collectFilesList2(String startFolder, List<File> collectedFilesList, String fileExtFilter, boolean recurseFolders) throws IOException {
		File file = new File(startFolder);
		File[] filesList = file.listFiles();
		
		for (File f : filesList) {
			if (f.isDirectory() && recurseFolders) {
				collectFilesList2(f.getAbsolutePath(), collectedFilesList, fileExtFilter, recurseFolders);
			} else {
				//no filter
				if (fileExtFilter.isEmpty() || fileExtFilter.equals("*")) {
					collectedFilesList.add(f);
				} else {
					if (fileExtFilter.equalsIgnoreCase(getFileExtension(f))) {
						collectedFilesList.add(f);
					}
				}
			}
		}
	}
	
	private static String getFileExtension(File f) {
		String fileName = f.getName();
		String fileExtension = fileName;
		
		int lastPos = fileName.lastIndexOf('.');
		
		if (lastPos > 0 && lastPos < (fileName.length() - 1)) {
			fileExtension = fileName.substring(lastPos + 1).toLowerCase();
		}
		
		return fileExtension;
	}
	
	public static String[] readTextFile(String filePath) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(filePath));
		ArrayList<String> fileContents = new ArrayList<String>();
		String line = br.readLine();
		while (line != null) {
			fileContents.add(line);
			line = br.readLine();
		}
		br.close();
		
		return fileContents.toArray(new String[fileContents.size()]);
	}
	
	public static void writeToTextFile(String filePath, String text) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(filePath)); 
		bw.write(text);
		bw.close();
	}
	
	public static void deleteFile(String filePath) {
		File f = new File(filePath);
		f.delete();
	}
	
	/**
	 * Create a directory structure
	 * @param path The path to create
	 * @throws IOException If the path could not be created or the path is a file
	 */
	public static void makeDirectories(String path) throws IOException {
		// 
		File target = new File(path);
		if( target.exists() ) {
			if( ! target.isDirectory()) {
				throw new IOException("Target directory for generating CreativeWorks files is actually a file");
			}
		}
		else {
			target.mkdirs();
		}
	}
	
	public static boolean fileExists(String filePath) {
		File f = new File(filePath);
		return (f.exists() && !f.isDirectory());
	}
	
	/**
	 * Removes last folder separator if found. e.g. : /a1/b1/ -> /a1/b1
	 * 
	 * @param path
	 * @return result path, see example
	 */
	public static String normalizePath(String path) {
		if (path.endsWith(File.separator)) {
			return path.substring(0, path.length() - 2);
		}
		return path;
	}
	
	public static boolean isWindowsOS() {
		String osName = System.getProperty("os.name");
		return osName.toLowerCase().contains("windows");
	}
}
