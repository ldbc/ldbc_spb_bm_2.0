package eu.ldbc.semanticpublishing.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

/**
 * A utility class for performing various operations on strings. 
 */
public class StringUtil {
	
	/**
	 * Prepares a string from an input strem, used when needed to log a detailed query result
	 * @param is
	 * @return
	 * @throws IOException
	 */
	public static String iostreamToString(InputStream is) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
		StringBuilder sb = new StringBuilder();
	    String str;
	    while (null != (str = br.readLine())) {
	        sb.append(str).append("\r\n"); 
	    }
	    br.close();	    
	    return sb.toString();
	}	
	
	/**
	 * Returns an InputStream from a given string using UTF-8 encoding
	 * @param str
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public static InputStream stringToIostream(String str) throws UnsupportedEncodingException {
		return new ByteArrayInputStream(str.getBytes("UTF-8"));
	}
	
	public static String toOneLine(String input) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < input.length(); i++) {
			char ch = input.charAt(i);
			if (ch == '\n') {
				sb.append(" ");
			} else {
				sb.append(ch);
			}
		}
		return sb.toString();
	}
	
	/*	
	//Non UTF-8 compatible !!!
	private String readResultString(InputStream is) throws IOException {
		int length = 0;
		byte[] buffer = new byte[10000];
		StringBuilder sb = new StringBuilder();
		while((length = is.read(buffer)) != -1) {
			String s = new String(buffer, 0, length);
			sb.append(s);
		}
		return sb.toString();		
	}
	*/	
}
