package eu.ldbc.semanticpublishing.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.util.List;

/**
 * Purpose of this class will be to generate test.properties file from a given template.
 * Templated parameters start with {1}, {2}...{N}
 * Source of parameters is another file with semi-column (;) separated values - one line per template.
 * Amount of generated templates will be equal to the number of lines in the 'source of paramters' file
 */
public class TestPropertiesGenerator {
	private List<List<String>> templateParametersList = new LinkedList<List<String>>();
	private static final String DELIMITER = ",";	
	
	private int initializeTmeplateParameterValues(String templateValuesSourceFile) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(templateValuesSourceFile), "UTF-8"));
		
		String line;
		while ((line = br.readLine()) != null) {
			if (line.startsWith("#")) {
				continue;
			}
			
			String[] tokens = line.split(DELIMITER);
			if (tokens == null) {
				continue;
			}
			
			LinkedList<String> parametersList = new LinkedList<String>();
			for (int i = 0; i < tokens.length; i++) {
				parametersList.add(tokens[i]);
			}
			
			templateParametersList.add(parametersList);
		}
		
		br.close();
		return templateParametersList.size();
	}
	
	private List<String> initializeSourceTemplate(String templateSourceFile) throws IOException {
		List<String> initialized = new LinkedList<String>();
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(templateSourceFile), "UTF-8"));
		
		String line;
		while ((line = br.readLine()) != null) {
			initialized.add(line);
		}
		br.close();
		return initialized;
	}
	
	private int generate(String templateSource) throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(templateSource), "UTF-8"));
		BufferedWriter out;
		List<String> template = initializeSourceTemplate(templateSource);
		
		int generated = 0;
		
		for (List<String> parametersList : templateParametersList) {		
			String fileName = String.format("%s-%03d-%s", templateSource, ++generated, generateFileName(parametersList, ".properties"));
			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName), "UTF-8"));
			String generatedTempalte = replaceInTemplate(template, parametersList);
			out.write(generatedTempalte);
			out.close();
		}
		in.close();
		
		return generated;
	}
	
	private String generateFileName(List<String> parameters, String suffix) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < parameters.size(); i++) {
			sb.append(parameters.get(i));
			if (i < parameters.size() - 1) {
				sb.append("-");
			}
		}
		sb.append(suffix);
		return sb.toString();
	}
	
	private String replaceInTemplate(List<String> sourceTemplate, List<String> parameters) {
		StringBuilder sb = new StringBuilder();
		int currentParameter = 0;
		int startPos = -1;
		int endPos = -1;
		
		for (int i = 0; i < sourceTemplate.size(); i++) {
			String s = sourceTemplate.get(i);
			
			startPos = s.indexOf("{");
			endPos = s.indexOf("}");
			
			if (startPos >= 0 && endPos >= 0) {
				sb.append(s.substring(0, startPos));
				sb.append(parameters.get(currentParameter++));
				sb.append(s.substring(endPos + 1, s.length()));
			} else {
				sb.append(s);
			}
			sb.append("\n");
		}
		
		return sb.toString();
	}
	
	public static void showHelp() {
		System.out.println("\tUSAGE: java -cp spb.jar eu.ldbc.semanticpublishing.tools.TestPropertiesGenerator <templateSourceFile> <templateParameterValuesFile>");
	}
	
	public static void main(String[] args) throws IOException {
		if (args.length != 2) {
			TestPropertiesGenerator.showHelp();
			return;
		}
		
		TestPropertiesGenerator tpg = new TestPropertiesGenerator();
		tpg.initializeTmeplateParameterValues(args[1]);
		int generated = tpg.generate(args[0]);
		System.out.println("Finished. " + generated + " files generated.");
	}
}
