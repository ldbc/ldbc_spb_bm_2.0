package eu.ldbc.semanticpublishing.util;

import java.io.File;
import java.io.IOException;

public class ShellUtil {
	private static String createFilePath(String path, String fileName) {
		StringBuilder sb = new StringBuilder();
		sb.append(path);
		if (!path.endsWith(File.separator)) {
			sb.append(File.separator);
		}
		sb.append(fileName);
		return sb.toString();
	}
	
	public static int execute(String path, String fileName, boolean executeSynchronous) throws IOException, InterruptedException {
		int returnCode = 0;
		
		String toExecute = createFilePath(path, fileName);
		Process p = Runtime.getRuntime().exec(toExecute, null, new File(path));
		if (executeSynchronous) {
			returnCode = p.waitFor();
		}
		return returnCode;
	}	
}
