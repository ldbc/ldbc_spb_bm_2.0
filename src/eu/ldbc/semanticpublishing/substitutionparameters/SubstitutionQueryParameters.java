package eu.ldbc.semanticpublishing.substitutionparameters;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SubstitutionQueryParameters {
	private String queryName;
	private List<String> substParametersList;
	
	public SubstitutionQueryParameters(String queryName) {
		this.queryName = queryName;
		this.substParametersList = new ArrayList<String>();
	}
	
	@SuppressWarnings("resource")
	public void initFromFile(String fullPath, boolean suppressErrorMessages, boolean stopOnEmptyLine) throws IOException, InterruptedException {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(fullPath));
			String line = br.readLine();
			while (line != null) {
				if (line.startsWith("[") || line.startsWith("#")) {
					line = br.readLine();
					continue;
				}
				if (stopOnEmptyLine && line.isEmpty()) {
					return;
				}
				substParametersList.add(line);
				line = br.readLine();
			}
		} catch (IOException ioe) {
			if (!suppressErrorMessages) {
				System.out.println("\tFailed to initialize query substitution parameters from : " + fullPath + ", will use random values for query parameters. Set generateQuerySubstitutionParameters=true to enable generation or ignore.");
			}
		} finally {
			try { br.close();} catch(Exception e) {}
		}
	}
	
	public String getQueryName() {
		return this.queryName;
	}

	public String[] get(long ind) {
		if (substParametersList.size() > 0) {
			if (ind >= substParametersList.size()) {
				return substParametersList.get((int)ind % substParametersList.size()).split(SubstitutionParametersGenerator.PARAMS_DELIMITER);
			} else {
				return substParametersList.get((int)ind).split(SubstitutionParametersGenerator.PARAMS_DELIMITER);
			}
		}
		return null;		
	}
	
	public int getParametersCount() {
		return substParametersList.size();
	}
}
