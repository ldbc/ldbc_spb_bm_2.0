package eu.ldbc.semanticpublishing.util.validation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashSet;

/**
 * A utility class for converting an input a result of a query, serialized into NT file format into a unique list subject-predicate-object file.
 * For internal use only.
 */
public class ParseNT_ProduceSPO_ResultFile {
	private static final String SUBSTITUTION_PARAMETERS = "[SubstitutionParameters]";
	private static final String RESULTS_SECTION = "[Results]";
	private static final String SUBJECT_SECTION = "[Subject]";
	private static final String PREDICATE_SECTION = "[Predicate]";
	private static final String OBJECT_SECTION = "[Object]";
	
	private enum SPO_Type {SUBJECT, PREDICATE, OBJECT};
	
	protected String getFileName(String filePath) {
		if (filePath.contains(File.separator)) {
			return filePath.substring(filePath.lastIndexOf(File.separator), filePath.length());
		} else {
			return filePath;
		}
	}
	
	public void parseNTproduce(String ntFilePath) throws IOException {
		
		String filePath = ntFilePath;
		System.out.println("Processing: " + getFileName(filePath));
		
		HashSet<String> subjectsSet = new HashSet<String>();
		HashSet<String> predicatesSet = new HashSet<String>();
		HashSet<String> objectsSet = new HashSet<String>();
		
		BufferedReader br = new BufferedReader(new FileReader(filePath));
		Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(ntFilePath + ".txt"), "utf-8"));
		
	    try {
	    	writer.write(SUBSTITUTION_PARAMETERS);
	    	writer.write("\n\n");
	    	writer.write(RESULTS_SECTION);
	    	writer.write("\n");
	    	
	        String line = br.readLine();
	        while (line != null) {
	        	if (line.trim().isEmpty() || line.startsWith("#")) {
	        		continue;
	        	}
	        	
	            subjectsSet.add(getSPO(line, SPO_Type.SUBJECT));
	            predicatesSet.add(getSPO(line, SPO_Type.PREDICATE));
	            objectsSet.add(getSPO(line, SPO_Type.OBJECT));
	            
	            line = br.readLine();
	        }
	        
	        //serialize to file
	        
	        writer.write(SUBJECT_SECTION);
	        writer.write("\n");
	        for (String subject : subjectsSet) {
	        	writer.write(subject);
	        	writer.write("\n");
	        }
	        writer.write("\n");
	        
	        writer.write(PREDICATE_SECTION);
	        writer.write("\n");
	        for (String predicate : predicatesSet) {
	        	writer.write(predicate);
	        	writer.write("\n");
	        }
	        writer.write("\n");

	        writer.write(OBJECT_SECTION);
	        writer.write("\n");
	        for (String object : objectsSet) {
	        	writer.write(object);
	        	writer.write("\n");
	        }       
	        
            System.out.println("Unique Subjects : " + subjectsSet.size());
            System.out.println("Unique Predicates : " + predicatesSet.size());
            System.out.println("Unique Objects : " + objectsSet.size());
	    } finally {
	        br.close();
   			writer.close();	        
	    }	
	}
	
	protected String getSPO(String str, SPO_Type spoType) {
		if (str.isEmpty()) {
			return str;
		}		
		
		int subjectStartPos = 0;
		int subjectEndPos = str.indexOf("> ");
		int predicateStartPos = subjectEndPos + 2;
		int predicateEndPos = str.indexOf("> ", predicateStartPos);
		int objectStartPos = predicateEndPos + 2;
		int objectEndPos = str.indexOf(" .");
				
		switch (spoType) {
		case SUBJECT :
			return str.substring(subjectStartPos, subjectEndPos + 1);
			
		case PREDICATE :
			return str.substring(predicateStartPos, predicateEndPos + 1);
			
		case OBJECT :
			return str.substring(objectStartPos, objectEndPos);

		default:
			return str;
		}
	}
	
	public static void showUsage() {
		System.out.println("\n\t USAGE : java files.ParseNT_ProduceSPO <nt-filepath>");
	}
	
	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			showUsage();
			return;
		}
		
		long startTime = System.currentTimeMillis();
		System.out.println("Started");
		
		String ntFilePath = args[0];
		ParseNT_ProduceSPO_ResultFile ntParseProduce = new ParseNT_ProduceSPO_ResultFile();
		ntParseProduce.parseNTproduce(ntFilePath);
		
		System.out.println("finished in " + (System.currentTimeMillis() - startTime) + " ms.");
	}
}
