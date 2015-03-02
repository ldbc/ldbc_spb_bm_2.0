package eu.ldbc.semanticpublishing.util;

import java.io.File;

/**
 * A utility class for performing various operations on strings. 
 */
public class StringUtil {
	
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
}
