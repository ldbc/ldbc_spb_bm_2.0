package eu.ldbc.semanticpublishing.templates.editorial;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import eu.ldbc.semanticpublishing.TestDriver;
import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection.QueryType;
import eu.ldbc.semanticpublishing.properties.Definitions;
import eu.ldbc.semanticpublishing.refdataset.DataManager;
import eu.ldbc.semanticpublishing.refdataset.model.Entity;
import eu.ldbc.semanticpublishing.substitutionparameters.SubstitutionParametersGenerator;
import eu.ldbc.semanticpublishing.templates.MustacheTemplate;
import eu.ldbc.semanticpublishing.util.RandomUtil;
import eu.ldbc.semanticpublishing.util.RdfUtils;
import eu.ldbc.semanticpublishing.util.StringUtil;
import org.eclipse.rdf4j.rio.RDFFormat;

/**
 * A class extending the MustacheTemplateCompiler, used to generate a query string
 * corresponding to file Configuration.QUERIES_PATH/editorial/insert.txt
 */
public class InsertTemplate extends MustacheTemplate implements SubstitutionParametersGenerator {
	//must match with corresponding file name of the mustache template file
	private static final String templateFileName =
			TestDriver.generatedCreativeWorksFormat == RDFFormat.TRIGSTAR ? "insert_sparql_star.txt":"insert.txt";
	
	private CWType cwType = CWType.BLOG_POST;
	private String cwTypeString = "cwork:BlogPost";
	private String contextURI;
	private int aboutsCount = 0;
	private int mentionsCount = 0;	
	private int seedYear = 2000;
	private Entity cwEntity;
	private boolean initialAboutUriUsed = false;
	private boolean geoLocationUsed = false;
	
	private final RandomUtil ru;
	
	private static enum CWType {
		BLOG_POST, NEWS_ITEM, PROGRAMME
	}	
	
	public InsertTemplate(String contextURI, RandomUtil ru, HashMap<String, String> queryTemplates, Definitions definitions) {
		this(contextURI, ru, queryTemplates, definitions, true, null);
	}
	
	public InsertTemplate(String contextURI, RandomUtil ru, HashMap<String, String> queryTemplates, Definitions definitions, boolean initializeCWEntity, String[] substitutionParameters) {
		super(queryTemplates, substitutionParameters);
		this.contextURI = contextURI;
		this.ru = ru;
		this.seedYear = definitions.getInt(Definitions.YEAR_SEED);
		preInitialize();
		if (initializeCWEntity) {
			initializeCreativeWorkEntity(contextURI);
		}
	}
	
	private void preInitialize() {
		this.aboutsCount = Definitions.aboutsAllocations.getAllocation();
		this.mentionsCount = Definitions.mentionsAllocations.getAllocation();
	}
	
	/**
	 * Creates an Entity with existing/new Creative Work URI and the label of a random (popular or not) existing entity.
	 * Label will be used in the title of that Creative Work
	 * @param updateCwUri - if empty, a new URI is generated
	 * @return the CreativeWork entity
	 */
	private void initializeCreativeWorkEntity(String updateCwUri) {
		Entity e;
		String cwURInew;
		try {			
			if (!updateCwUri.isEmpty()) {
				cwURInew = updateCwUri;
			} else {
				cwURInew = ru.numberURI(RandomUtil.THINGS_STRING, DataManager.creativeWorksNextId.incrementAndGet(), true, true);
			}
			
			this.contextURI = cwURInew.replace("/things/", "/context/");
			
			switch (Definitions.creativeWorkTypesAllocation.getAllocation()) {
				case 0 :
					this.cwType = CWType.BLOG_POST;
					this.cwTypeString = "cwork:BlogPost";
					break;
				case 1 :
					this.cwType = CWType.NEWS_ITEM;
					this.cwTypeString = "cwork:NewsItem";
					break;
				case 2 :
					this.cwType = CWType.PROGRAMME;
					this.cwTypeString = "cwork:Programme";
					break;					
			}
			
			boolean usePopularEntity = Definitions.usePopularEntities.getAllocation() == 0;
			
			if (usePopularEntity) {
				e = DataManager.popularEntitiesList.get(ru.nextInt(DataManager.popularEntitiesList.size()));
			} else {
				e = DataManager.regularEntitiesList.get(ru.nextInt(DataManager.regularEntitiesList.size()));
			}
			
			this.cwEntity = new Entity(cwURInew, e.getLabel(), e.getURI(), e.getCategory(), "0");
		} catch (IllegalArgumentException iae) {
			if (DataManager.popularEntitiesList.size() + DataManager.regularEntitiesList.size() == 0) {
				System.err.println("No reference data found in repository, initialize reposotory with ontologies and reference data first!");
			}
			throw new IllegalArgumentException(iae);
		}
	}
	
	/**
	 * A method for replacing mustache template : {{{cwGraphUri}}}
	 */		
	public String cwGraphUri() {
		if (substitutionParameters != null) {
			return substitutionParameters[parameterIndex++];
		}
		
		return this.contextURI;
	}
	
	/**
	 * A method for replacing mustache template : {{{cwUri}}}
	 */
	public String cwUri() {
		return cwUri(false);
	}

	public String cwUri(boolean isGenerateParams) {
		if (substitutionParameters != null) {
			//assuming that cwUri is positioned in the first two items of the parameters list
			if (parameterIndex > 2) {
				//we are keeping the context uri for current Insert template
				return this.contextURI.replace("/context/", "/things/");
			} else {
				//update contextURI value
				this.contextURI = substitutionParameters[parameterIndex].replace("/things/", "/context/");
				return substitutionParameters[parameterIndex++];
			}
		}

		if (isGenerateParams) {
			return this.contextURI.replace("/context/", "/things/");
		}

		return StringUtil.generateEmbeddedTripleFromURI(this.contextURI.replace("/context/", "/things/"));
	}

	/**
	 * A method for replacing mustache template : {{{cwType}}}
	 */		
	public String cwType() {
		if (substitutionParameters != null) {
			return substitutionParameters[parameterIndex++];
		}
		
		return this.cwTypeString;
	}
	
	/**
	 * A method for replacing mustache template : {{{cwTitle}}}
	 */		
	public String cwTitle() {
		if (substitutionParameters != null) {
			return substitutionParameters[parameterIndex++];
		}
		
		return ru.sentenceFromDictionaryWords(this.cwEntity.getLabel(), 10, true, true, 2, true);
	}
	
	/**
	 * A method for replacing mustache template : {{{cwShortTitle}}}
	 */		
	public String cwShortTitle() {
		if (substitutionParameters != null) {
			return substitutionParameters[parameterIndex++];
		}
		
		return ru.sentenceFromDictionaryWords("", 10, true, true, 2, true);
	}
	
	/**
	 * A method for replacing mustache template : {{{cwCategory}}}
	 */		
	public String cwCategory() {
		if (substitutionParameters != null) {
			return substitutionParameters[parameterIndex++];
		}
		
		return ru.stringURI("category", cwEntity.getCategory(), true, false);
	}
	
	/**
	 * A method for replacing mustache template : {{{cwDescription}}}
	 */		
	public String cwDescription() {
		if (substitutionParameters != null) {
			return substitutionParameters[parameterIndex++];
		}
		
		return ru.sentenceFromDictionaryWords("", ru.nextInt(8, 26 + 1), true, true, 2, true);
	}
	
	/**
	 * A method for replacing mustache template list : {{{#cwAboutsList}}} {{{/cwAboutsList}}}
	 */	
	public List<Object> cwAboutsList() {		
		  List<Object> abouts = new ArrayList<Object>();
		  
		  if (substitutionParameters != null) {
			  abouts.add(new AboutUri(substitutionParameters[parameterIndex++]));
			  return abouts;
		  }

		  //using aboutsCount + 1, because Definitions.aboutsAllocations.getAllocation() returning 0 is still a valid allocation
		  for (int i = 0; i < aboutsCount * 3 + 1; i++) {
			  if (!initialAboutUriUsed) {
				  initialAboutUriUsed = true;
				  abouts.add(new AboutUri(this.cwEntity.getObjectFromTriple(Entity.ENTITY_ABOUT)));
			  } else {
				  abouts.add(new AboutUri(DataManager.regularEntitiesList.get(ru.nextInt(DataManager.regularEntitiesList.size())).getURI()));
			  }
		  }
		  
		  return abouts;
	}
	
	/**
	 * A class for replacing mustache template : {{{CwAboutUri}}}, part of the cwAboutsList
	 */		
	static class AboutUri {
		String cwAboutUri;
		public AboutUri(String aboutUri) {
			this.cwAboutUri = aboutUri;
		}
	}
	
	/**
	 * A method for replacing mustache template list : {{{#cwAboutsList}}} {{{/cwAboutsList}}}
	 */		
	public List<Object> cwMentionsList() {
		List<Object> mentions = new ArrayList<Object>();
		
		if (substitutionParameters != null) {
			mentions.add(new MentionsUri(substitutionParameters[parameterIndex++]));
			return mentions;
		}
		
		 //using mentionsCount + 1, because Definitions.mentionsAllocations.getAllocation() returning 0 is still a valid allocation		
		for (int i = 0; i < mentionsCount * 3 + 1; i++) {
			if (!initialAboutUriUsed) {
				initialAboutUriUsed = true;
				mentions.add(new MentionsUri(this.cwEntity.getObjectFromTriple(Entity.ENTITY_ABOUT)));
			} else {
				if (!geoLocationUsed) {
					geoLocationUsed = true;
					mentions.add(new MentionsUri(DataManager.geonamesIdsList.get(ru.nextInt(DataManager.geonamesIdsList.size()))));
				} else {
					mentions.add(new MentionsUri(DataManager.regularEntitiesList.get(ru.nextInt(DataManager.regularEntitiesList.size())).getURI()));
				}
			}
		}
		
		return mentions;
	}
	
	/**
	 * A class for replacing mustache template : {{{CwMentionsUri}}}, part of the cwMentionsList
	 */
	static class MentionsUri {
		String cwMentionsUri;
		public MentionsUri(String mentionsUri) {
			this.cwMentionsUri = mentionsUri;
		}
	}
	
	/**
	 * A method for replacing mustache template : {{{cwAudienceType}}}
	 */		
	public String cwAudienceType() {
		if (substitutionParameters != null) {
			return substitutionParameters[parameterIndex++];
		}
		
		switch (cwType) {		
		case BLOG_POST :
			return "cwork:InternationalAudience";
		case NEWS_ITEM :
			return "cwork:NationalAudience";
		case PROGRAMME :
			return "cwork:InternationalAudience";
		default :
			return "cwork:InternationalAudience";
		}
	}
	
	/**
	 * A method for replacing mustache template : {{{cwLiveCoverage}}}
	 */		
	public String cwLiveCoverage() {
		if (substitutionParameters != null) {
			return substitutionParameters[parameterIndex++];
		}
		
		switch (cwType) {		
		case BLOG_POST :
			return ru.createBoolean(false);
		case NEWS_ITEM :
			return ru.createBoolean(false);
		case PROGRAMME :
			return ru.createBoolean(true);
		default :
			return ru.createBoolean(true);
		}
	}
	
	/**
	 * A method for replacing mustache template list : {{{#cwPrimaryFormatList}}} {{{/cwPrimaryFormatList}}}
	 */		
	public List<Object> cwPrimaryFormatList() {
		List<Object> format = new ArrayList<Object>();
		
		if (substitutionParameters != null) {
			format.add(new PrimaryFormat(substitutionParameters[parameterIndex++]));
			return format;
		}		
		
		switch (cwType) {		
		case BLOG_POST :
			format.add(new PrimaryFormat("cwork:TextualFormat"));
			if (ru.nextBoolean()) {
				format.add(new PrimaryFormat("cwork:InteractiveFormat"));
			}
		case NEWS_ITEM :
			format.add(new PrimaryFormat("cwork:TextualFormat"));
			format.add(new PrimaryFormat("cwork:InteractiveFormat"));
		case PROGRAMME :
			if (ru.nextBoolean()) {
				format.add(new PrimaryFormat("cwork:VideoFormat"));
			} else {
				format.add(new PrimaryFormat("cwork:AudioFormat"));
			}
		}
		return format;
	}
	
	/**
	 * A class for replacing mustache template : {{{cwPrimaryFormat}}}, part of the cwPrimaryFormatList
	 */	
	static class PrimaryFormat {
		String cwPrimaryFormat;
		public PrimaryFormat(String primaryFormat) {
			this.cwPrimaryFormat = primaryFormat;
		}
	}
	
	/**
	 * A method for replacing mustache template : {{{cwDateCreated}}}
	 */	
	public String cwDateCreated() {
		if (substitutionParameters != null) {
			return substitutionParameters[parameterIndex++];
		}
		
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.YEAR, seedYear);
		return ru.dateTimeString(calendar.getTime());
	}
	
	/**
	 * A method for replacing mustache template : {{{cwDateModified}}}
	 */		
	public String cwDateModified() {
		if (substitutionParameters != null) {
			return substitutionParameters[parameterIndex++];
		}
		
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.YEAR, seedYear);
		calendar.add(Calendar.MONTH, 1 * ru.nextInt(12 + 1));
		calendar.add(Calendar.DATE, 1 * ru.nextInt(31 + 1));
		calendar.add(Calendar.HOUR, 1 * ru.nextInt(23 + 1));
		return ru.dateTimeString(calendar.getTime());
	}
	
	/**
	 * A method for replacing mustache template : {{{cwThumbnailUri}}}
	 */	
	public String cwThumbnailUri() {
		if (substitutionParameters != null) {
			return substitutionParameters[parameterIndex++];
		}
		
		return ru.randomURI("thumbnail", true, false);
	}
	
	/**
	 * A method for replacing mustache template list : {{{#cwPrimaryContentList}}} {{{/cwPrimaryContentList}}}
	 */		
	public List<Object> cwPrimaryContentList() {
		List<Object> primaryContent = new ArrayList<Object>();
		
		if (substitutionParameters != null) {
			primaryContent.add(new PrimaryContentUri(substitutionParameters[parameterIndex++], substitutionParameters[parameterIndex++]));
			return primaryContent;
		}		
		
		for (int i = 0; i < ru.nextInt(1, 4 + 1); i++) {
			primaryContent.add(new PrimaryContentUri(ru.numberURI(RandomUtil.DOCUMENT_STRING, (long)(1E14 + DataManager.webDocumentNextId.incrementAndGet()), true, true), ru.nextBoolean() ? "bbc:HighWeb" : "bbc:Mobile"));
		}
		
		return primaryContent;
	}
	
	/**
	 * A class for replacing mustache template : {{{cwPrimaryContentUri}}} and {{{cwWebDocumentType}}}, part of the cwPrimaryFormatList
	 */	
	static class PrimaryContentUri {
		String cwPrimaryContentUri;
		String cwWebDocumentType;
		String cwPrimaryContentUriAsTriple;
		public PrimaryContentUri(String primaryContentUri, String webDocumentType) {
			this.cwPrimaryContentUri = primaryContentUri;
			this.cwWebDocumentType = webDocumentType;
			this.cwPrimaryContentUriAsTriple = "<<" + cwPrimaryContentUri + " " + RdfUtils.expandNamepsacePrefix("bbc:webDocumentType") + " " + RdfUtils.expandNamepsacePrefix(cwWebDocumentType) + ">>";
		}
	}
	
	@Override
	public String generateSubstitutionParameters(BufferedWriter bw, int amount) throws IOException {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < amount; i++) {
			preInitialize();
			initializeCreativeWorkEntity("");
			sb.setLength(0);
			sb.append(cwGraphUri());
			sb.append(SubstitutionParametersGenerator.PARAMS_DELIMITER);
			sb.append(cwUri(true));
			sb.append(SubstitutionParametersGenerator.PARAMS_DELIMITER);
			sb.append(RdfUtils.expandNamepsacePrefix(cwType()));
			sb.append(SubstitutionParametersGenerator.PARAMS_DELIMITER);
			sb.append(cwTitle());
			sb.append(SubstitutionParametersGenerator.PARAMS_DELIMITER);
			sb.append(cwShortTitle());
			sb.append(SubstitutionParametersGenerator.PARAMS_DELIMITER);
			sb.append(cwCategory());
			sb.append(SubstitutionParametersGenerator.PARAMS_DELIMITER);
			sb.append(cwDescription());
			sb.append(SubstitutionParametersGenerator.PARAMS_DELIMITER);
			//abouts list
			List<Object> aboutsList = cwAboutsList();
			if (aboutsList.size() > 0) {
				//executing one iteration on purpose, future TODO
				for (int j = 0; j < 1; j++) {
					sb.append(((AboutUri)aboutsList.get(j)).cwAboutUri);
				}
				sb.append(SubstitutionParametersGenerator.PARAMS_DELIMITER);
			} else {
				sb.append(" ");
				sb.append(SubstitutionParametersGenerator.PARAMS_DELIMITER);
			}
			//mentions list
			List<Object> mentionsList = cwMentionsList();
			if (mentionsList.size() > 0) {
				//executing one iteration on purpose, future TODO
				for (int j = 0; j < 1; j++) {
					sb.append(((MentionsUri)mentionsList.get(j)).cwMentionsUri);
				}
				sb.append(SubstitutionParametersGenerator.PARAMS_DELIMITER);
			} else {
				sb.append(" ");
				sb.append(SubstitutionParametersGenerator.PARAMS_DELIMITER);
			}
			sb.append(RdfUtils.expandNamepsacePrefix(cwAudienceType()));
			sb.append(SubstitutionParametersGenerator.PARAMS_DELIMITER);
			sb.append(cwLiveCoverage());
			sb.append(SubstitutionParametersGenerator.PARAMS_DELIMITER);
			//primary format list
			List<Object> primaryFormatList = cwPrimaryFormatList();
			if (primaryFormatList.size() > 0) {
				//executing one iteration on purpose, future TODO
				for (int j = 0; j < 1; j++) {
					sb.append(RdfUtils.expandNamepsacePrefix(((PrimaryFormat)primaryFormatList.get(j)).cwPrimaryFormat));
				}				
				sb.append(SubstitutionParametersGenerator.PARAMS_DELIMITER);
			} else {
				sb.append(" ");
				sb.append(SubstitutionParametersGenerator.PARAMS_DELIMITER);
			}
			sb.append(cwDateCreated());
			sb.append(SubstitutionParametersGenerator.PARAMS_DELIMITER);
			sb.append(cwDateModified());
			sb.append(SubstitutionParametersGenerator.PARAMS_DELIMITER);
			sb.append(cwThumbnailUri());
			sb.append(SubstitutionParametersGenerator.PARAMS_DELIMITER);
			//primary content list
			List<Object> primaryContentList = cwPrimaryContentList();
			if (primaryContentList.size() > 0) {
				//executing one iteration on purpose, future TODO
				for (int j = 0; j < 1; j++) {
					sb.append(ru.numberURI(RandomUtil.DOCUMENT_STRING, (long)(1E14 + DataManager.webDocumentNextId.incrementAndGet()), true, true));
					sb.append(SubstitutionParametersGenerator.PARAMS_DELIMITER);
					sb.append(ru.nextBoolean() ? RdfUtils.expandNamepsacePrefix("bbc:HighWeb") : RdfUtils.expandNamepsacePrefix("bbc:Mobile"));
				}
				sb.append(SubstitutionParametersGenerator.PARAMS_DELIMITER);
			} else {
				sb.append(" ");
				sb.append(SubstitutionParametersGenerator.PARAMS_DELIMITER);
			}			
			sb.append("\n");
			if (bw != null) {
				bw.write(sb.toString());
			}
		}
		return sb.toString();
	}
	
	@Override
	public String getTemplateFileName() {
		return templateFileName;
	}
	
	@Override
	public QueryType getTemplateQueryType() {
		return QueryType.INSERT;
	}
}
