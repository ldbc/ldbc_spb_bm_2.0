package eu.ldbc.semanticpublishing.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
//import java.util.zip.ZipEntry;
import java.util.zip.GZIPOutputStream;

/**
 * A Utility class for creating compressed (.gz) files
 */
public class CompressionUtil {
	
	public static enum OutputStreamType {
		FILE_OUTPUT_STREAM, BUFFERED_OUTPUT_STREAM, ZIP_OUTPUT_STREAM
	}
	
	public static void compressFile(String inputFilePath, boolean deleteSource) throws IOException {
		byte[] buffer = new byte[1024];
		FileOutputStream fos = null;
		GZIPOutputStream gzos = null;
		FileInputStream fis = null;
		
		try {			
			fos = new FileOutputStream(inputFilePath + ".gz");
			gzos = new GZIPOutputStream(fos);
			
			fis = new FileInputStream(inputFilePath);			
//			String zipEntryName = inputFilePath.substring(inputFilePath.lastIndexOf(File.separator) + 1);			
//			gzos.putNextEntry(new ZipEntry(zipEntryName));
			
			int length;
			while ((length = fis.read(buffer)) > 0) {
				gzos.write(buffer, 0, length);
			}
		} finally {
			gzos.close();
			fos.close();
			fis.close();
		}		
		
		if (deleteSource) {
			new File(inputFilePath).delete();
		}
	}
}
