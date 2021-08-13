package eu.ldbc.semanticpublishing.generators.data.sesamemodelbuilders;

import java.util.Calendar;
import java.util.Date;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.URI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleTriple;

import eu.ldbc.semanticpublishing.properties.Definitions;
import eu.ldbc.semanticpublishing.refdataset.DataManager;
import eu.ldbc.semanticpublishing.refdataset.model.Entity;
import eu.ldbc.semanticpublishing.util.RandomUtil;
import org.eclipse.rdf4j.rio.RDFFormat;

public class CreativeWorkBuilder implements SesameBuilder {

	private Date presetDate;
	private CWType cwType = CWType.BLOG_POST;
	private String cwTypeString = "cwork:BlogPost";
	private String contextURI = "";
	private String aboutPresetUri = "";
	private String optionalAboutPresetUri = "";
	private String mentionsPresetUri = "";
	private String optionalMentionsPresetUri = "";
	private int aboutsCount = 0;
	private int mentionsCount = 0;	
	private Entity cwEntity;
	private boolean usePresetData = false;
	
	private final RandomUtil ru;
	
	private static final String rdfTypeNamespace = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
	private static final String cworkNamespace = "http://www.bbc.co.uk/ontologies/creativework/";
	private static final String bbcNamespace = "http://www.bbc.co.uk/ontologies/bbc/";
	
	private static enum CWType {
		BLOG_POST, NEWS_ITEM, PROGRAMME
	}
	
	public CreativeWorkBuilder(String contextURI, RandomUtil ru) {
		this.contextURI = contextURI;
		this.ru = ru;
		Definitions.reconfigureAllocations(ru.getRandom());
		this.aboutsCount = Definitions.aboutsAllocations.getAllocation();
		this.mentionsCount = Definitions.mentionsAllocations.getAllocation();
		initializeCreativeWorkEntity(contextURI.replace("/context/", "/things/"));
	}
	
	public CreativeWorkBuilder(long cwID, RandomUtil ru) {
		this.contextURI = ru.numberURI(RandomUtil.THINGS_STRING, cwID, true, true).replace("/things/", "/context/");
		this.ru = ru;
		Definitions.reconfigureAllocations(ru.getRandom());
		this.aboutsCount = Definitions.aboutsAllocations.getAllocation();
		this.mentionsCount = Definitions.mentionsAllocations.getAllocation();
		initializeCreativeWorkEntity(contextURI.replace("/context/", "/things/"));
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
			
			this.cwEntity = new Entity(cwURInew, e.getLabel(), e.getURI(), e.getCategory(), e.getRank());
		} catch (IllegalArgumentException iae) {
			if (DataManager.popularEntitiesList.size() + DataManager.regularEntitiesList.size() == 0) {
				System.err.println("No reference data found in repository, initialize repository with ontologies and reference data first!");
			}
			throw new IllegalArgumentException(iae);
		}
	}
	
	public void setDateIncrement(Date startDate, int daySteps) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(startDate);
		calendar.add(Calendar.DATE, daySteps);
		calendar.add(Calendar.HOUR, ru.nextInt(23 + 1));
		calendar.add(Calendar.MINUTE, ru.nextInt(59 + 1));
		calendar.add(Calendar.SECOND, ru.nextInt(59 + 1));
		//milliseconds are fixed, some strange problem when re-running the generator - produces different values
		calendar.set(Calendar.MILLISECOND, ru.nextInt(999 + 1));
		
		this.presetDate = calendar.getTime();
	}
	
	public void setUsePresetData(boolean usePresetData) {
		this.usePresetData = true;
	}
	
	public void setAboutPresetUri(String aboutUri) {
		this.aboutPresetUri = aboutUri;
	}
	
	public void setOptionalAboutPresetUri(String optionalAboutUri) {
		this.optionalAboutPresetUri = optionalAboutUri;
	}
	
	public void setMentionsPresetUri(String mentionsUri) {
		this.mentionsPresetUri = mentionsUri;
	}
	
	public void setOptionalMentionsPresetUri(String optionalMentionsUri) {
		this.optionalMentionsPresetUri = optionalMentionsUri;
	}
		
	/**
	 * Builds a Sesame Model of the Insert query template using values from templateParameterValues array.
	 * Which gets initialized with values during construction of the object.
	 */
	@Override
	public synchronized Model buildSesameModel(RDFFormat rdfFormat) {
		Model model = new LinkedHashModel();
		String adaptedContextUri = contextURI.replace("<", "").replace(">", "");
		URI context = sesameValueFactory.createURI(adaptedContextUri);
		
		//Set Creative Work Type
		URI subject = sesameValueFactory.createURI(adaptedContextUri.replace("/context/", "/things/"));
		URI predicate = sesameValueFactory.createURI(rdfTypeNamespace);
		String s = cwTypeString;
		s = s.replace("cwork:", cworkNamespace);
		Value object = sesameValueFactory.createURI(s);

		SimpleTriple embeddedTriple = (SimpleTriple) sesameValueFactory.createTriple(subject, (IRI) predicate, sesameValueFactory.createURI("http://www.bbc.co.uk/ontologies/creativework/CreativeWork"));
		
		model.add(rdfFormat == RDFFormat.TRIGSTAR ? embeddedTriple : subject, predicate, object, context);
		
		//Set Title
		predicate = sesameValueFactory.createURI(cworkNamespace + "title");
		object = sesameValueFactory.createLiteral(ru.sentenceFromDictionaryWords(this.cwEntity.getLabel(), 10, false, true, 1, false));		
		
		model.add(rdfFormat == RDFFormat.TRIGSTAR ? embeddedTriple : subject, predicate, object, context);

		//Set Short Title
		predicate = sesameValueFactory.createURI(cworkNamespace + "shortTitle");
		object = sesameValueFactory.createLiteral(ru.sentenceFromDictionaryWords("", 10, false, true, 1, false));		
		
		model.add(rdfFormat == RDFFormat.TRIGSTAR ? embeddedTriple : subject, predicate, object, context);

		//Set Category
		predicate = sesameValueFactory.createURI(cworkNamespace + "category");
		object = sesameValueFactory.createURI(ru.stringURI("category", cwEntity.getCategory(), false, false));

		model.add(rdfFormat == RDFFormat.TRIGSTAR ? embeddedTriple : subject, predicate, object, context);
		
		//Set Description
		predicate = sesameValueFactory.createURI(cworkNamespace + "description");
		object = sesameValueFactory.createLiteral(ru.sentenceFromDictionaryWords("", ru.nextInt(8, 26 + 1), false, true, 1, false));
		
		model.add(rdfFormat == RDFFormat.TRIGSTAR ? embeddedTriple : subject, predicate, object, context);
		
		boolean initialAboutUriUsed = false;
		String initialUri = this.cwEntity.getObjectFromTriple(Entity.ENTITY_ABOUT);
		
		if (usePresetData) {
			initialUri = aboutPresetUri;
		}
		
		//Set About(s)
		//using aboutsCount + 1, because Definitions.aboutsAllocations.getAllocation() returning 0 is still a valid allocation
		for (int i = 0; i < aboutsCount * 3 + 1; i++) {
			predicate = sesameValueFactory.createURI(cworkNamespace + "about");
			
			if (!initialAboutUriUsed) {
				initialAboutUriUsed = true;
			} else {
				initialUri = DataManager.regularEntitiesList.get(ru.nextInt(DataManager.regularEntitiesList.size())).getURI();
			}
			
			object = sesameValueFactory.createURI(initialUri.replace("<", "").replace(">", ""));
			
			model.add(rdfFormat == RDFFormat.TRIGSTAR ? embeddedTriple : subject, predicate, object, context);
		}
		
		//Add optional About URI - in case of modeling correlations - disregard the about distributions
		if (usePresetData && !optionalAboutPresetUri.isEmpty()) {
			predicate = sesameValueFactory.createURI(cworkNamespace + "about");
			object = sesameValueFactory.createURI(optionalAboutPresetUri.replace("<", "").replace(">", ""));
			model.add(rdfFormat == RDFFormat.TRIGSTAR ? embeddedTriple : subject, predicate, object, context);
		}
		
		//Set Mention(s)
		//using mentionsCount + 1, because Definitions.mentionsAllocations.getAllocation() returning 0 is still a valid allocation
		boolean geoLocationsUsedLocal = false;			
		for (int i = 0; i < mentionsCount * 3 + 1; i++) {
			predicate = sesameValueFactory.createURI(cworkNamespace + "mentions");
			
			if (!initialAboutUriUsed) {
				initialAboutUriUsed = true;
			} else {
				if (!geoLocationsUsedLocal) {
					geoLocationsUsedLocal = true;
					initialUri = DataManager.geonamesIdsList.get(ru.nextInt(DataManager.geonamesIdsList.size()));
				} else {
					initialUri = DataManager.regularEntitiesList.get(ru.nextInt(DataManager.regularEntitiesList.size())).getURI();
				}
			}
			
			object = sesameValueFactory.createURI(initialUri.replace("<", "").replace(">", ""));			
			
			model.add(rdfFormat == RDFFormat.TRIGSTAR ? embeddedTriple : subject, predicate, object, context);
		}

		//Add Mentions URI - in case of modeling correlations - disregard the mentions distributions
		if (usePresetData && !mentionsPresetUri.isEmpty()) {
			predicate = sesameValueFactory.createURI(cworkNamespace + "mentions");
			object = sesameValueFactory.createURI(mentionsPresetUri.replace("<", "").replace(">", ""));
			model.add(rdfFormat == RDFFormat.TRIGSTAR ? embeddedTriple : subject, predicate, object, context);
		}		

		//Add optional Mentions URI - in case of modeling correlations - disregard the mentions distributions
		if (usePresetData && !optionalMentionsPresetUri.isEmpty()) {
			predicate = sesameValueFactory.createURI(cworkNamespace + "mentions");
			object = sesameValueFactory.createURI(optionalMentionsPresetUri.replace("<", "").replace(">", ""));
			model.add(rdfFormat == RDFFormat.TRIGSTAR ? embeddedTriple : subject, predicate, object, context);
		}		

		switch (cwType) {
		case BLOG_POST :
			//Set Audience
			predicate = sesameValueFactory.createURI(cworkNamespace + "audience");
			object = sesameValueFactory.createURI(cworkNamespace + "InternationalAudience");
			
			model.add(rdfFormat == RDFFormat.TRIGSTAR ? embeddedTriple : subject, predicate, object, context);
			
			//Set LiveCoverage
			predicate = sesameValueFactory.createURI(cworkNamespace + "liveCoverage");
			object = sesameValueFactory.createLiteral(false);
			
			model.add(rdfFormat == RDFFormat.TRIGSTAR ? embeddedTriple : subject, predicate, object, context);
			
			//Set PrimaryFormat
			predicate = sesameValueFactory.createURI(cworkNamespace + "primaryFormat");
			object = sesameValueFactory.createURI(cworkNamespace + "TextualFormat");
			
			model.add(rdfFormat == RDFFormat.TRIGSTAR ? embeddedTriple : subject, predicate, object, context);
			
			if (ru.nextBoolean()) {
				//Set additional primary format randomly
				predicate = sesameValueFactory.createURI(cworkNamespace + "primaryFormat");
				object = sesameValueFactory.createURI(cworkNamespace + "InteractiveFormat");
				
				model.add(rdfFormat == RDFFormat.TRIGSTAR ? embeddedTriple : subject, predicate, object, context);
			}
			
			break;
		case NEWS_ITEM :
			//Set Audience
			predicate = sesameValueFactory.createURI(cworkNamespace + "audience");
			object = sesameValueFactory.createURI(cworkNamespace + "NationalAudience");
			
			model.add(rdfFormat == RDFFormat.TRIGSTAR ? embeddedTriple : subject, predicate, object, context);
			
			//Set LiveCoverage
			predicate = sesameValueFactory.createURI(cworkNamespace + "liveCoverage");
			object = sesameValueFactory.createLiteral(false);
			
			model.add(rdfFormat == RDFFormat.TRIGSTAR ? embeddedTriple : subject, predicate, object, context);
			
			//Set PrimaryFormat
			predicate = sesameValueFactory.createURI(cworkNamespace + "primaryFormat");
			object = sesameValueFactory.createURI(cworkNamespace + "TextualFormat");
			
			model.add(rdfFormat == RDFFormat.TRIGSTAR ? embeddedTriple : subject, predicate, object, context);
			
			//Set additional primary format
			predicate = sesameValueFactory.createURI(cworkNamespace + "primaryFormat");
			object = sesameValueFactory.createURI(cworkNamespace + "InteractiveFormat");
			
			model.add(rdfFormat == RDFFormat.TRIGSTAR ? embeddedTriple : subject, predicate, object, context);
			
			break;
		case PROGRAMME : 
			//Set Audience
			predicate = sesameValueFactory.createURI(cworkNamespace + "audience");
			object = sesameValueFactory.createURI(cworkNamespace + "InternationalAudience");
			
			model.add(rdfFormat == RDFFormat.TRIGSTAR ? embeddedTriple : subject, predicate, object, context);
			
			//Set LiveCoverage
			predicate = sesameValueFactory.createURI(cworkNamespace + "liveCoverage");
			object = sesameValueFactory.createLiteral(true);
			
			model.add(rdfFormat == RDFFormat.TRIGSTAR ? embeddedTriple : subject, predicate, object, context);
			
			//Set PrimaryFormat
			predicate = sesameValueFactory.createURI(cworkNamespace + "primaryFormat");
			if (ru.nextBoolean()) {
				object = sesameValueFactory.createURI(cworkNamespace + "VideoFormat");
			} else {
				object = sesameValueFactory.createURI(cworkNamespace + "AudioFormat");
			}
			
			model.add(rdfFormat == RDFFormat.TRIGSTAR ? embeddedTriple : subject, predicate, object, context);
						
			break;
		}
		
		//Creation and Modification date
		Calendar calendar = Calendar.getInstance();
		
		if (usePresetData) {
			//Set Creation Date
			predicate = sesameValueFactory.createURI(cworkNamespace + "dateCreated");
			object = sesameValueFactory.createLiteral(presetDate);
			model.add(rdfFormat == RDFFormat.TRIGSTAR ? embeddedTriple : subject, predicate, object, context);
			
			//Set Modification Date
			calendar.setTime(presetDate);
			calendar.add(Calendar.MONTH, 1 * ru.nextInt(12 + 1));
			calendar.add(Calendar.DATE, 1 * ru.nextInt(31 + 1));
			calendar.add(Calendar.HOUR, 1 * ru.nextInt(23 + 1));
			calendar.add(Calendar.MINUTE, 1 * ru.nextInt(59 + 1));
			calendar.add(Calendar.SECOND, 1 * ru.nextInt(59 + 1));
			//milliseconds are fixed, some strange problem when re-running the generator - produces different values
			calendar.set(Calendar.MILLISECOND, ru.nextInt(999 + 1));
			
			predicate = sesameValueFactory.createURI(cworkNamespace + "dateModified");
			object = sesameValueFactory.createLiteral(calendar.getTime());
		} else {
			Date creationDate = ru.randomDateTime();
			
			//Set Creation Date
			predicate = sesameValueFactory.createURI(cworkNamespace + "dateCreated");
			object = sesameValueFactory.createLiteral(creationDate);
			
			model.add(rdfFormat == RDFFormat.TRIGSTAR ? embeddedTriple : subject, predicate, object, context);
			
			//Set Modification Date
			calendar.setTime(creationDate);
			calendar.add(Calendar.MONTH, 1 * ru.nextInt(12 + 1));
			calendar.add(Calendar.DATE, 1 * ru.nextInt(31 + 1));
			calendar.add(Calendar.HOUR, 1 * ru.nextInt(23 + 1));
			calendar.add(Calendar.MINUTE, 1 * ru.nextInt(59 + 1));
			calendar.add(Calendar.SECOND, 1 * ru.nextInt(59 + 1));
			//milliseconds are fixed, some strange problem when re-running the generator - produces different values
			calendar.set(Calendar.MILLISECOND, ru.nextInt(999 + 1));
			
			predicate = sesameValueFactory.createURI(cworkNamespace + "dateModified");
			object = sesameValueFactory.createLiteral(calendar.getTime());
		}
		
		model.add(rdfFormat == RDFFormat.TRIGSTAR ? embeddedTriple : subject, predicate, object, context);
		
		//Set Thumbnail
		predicate = sesameValueFactory.createURI(cworkNamespace + "thumbnail");
		object = sesameValueFactory.createURI(ru.randomURI("thumbnail", false, false));
		
		model.add(rdfFormat == RDFFormat.TRIGSTAR ? embeddedTriple : subject, predicate, object, context);
		
		//Set cwork:altText to thumbnail
		predicate = sesameValueFactory.createURI(cworkNamespace + "altText");
		object = sesameValueFactory.createLiteral("thumbnail atlText for CW " + adaptedContextUri);
		
		model.add(rdfFormat == RDFFormat.TRIGSTAR ? embeddedTriple : subject, predicate, object, context);
		
		//Set PrimaryContentOf
		int random = ru.nextInt(1, 4 + 1);
		for (int i = 0; i < random; i++) {
			predicate = sesameValueFactory.createURI(bbcNamespace + "primaryContentOf");
			String primaryContentUri = ru.numberURI(RandomUtil.DOCUMENT_STRING, DataManager.webDocumentNextId.incrementAndGet(), false, true);

			URI tripleSubject = sesameValueFactory.createURI(primaryContentUri);
			URI triplePredicate = sesameValueFactory.createURI(bbcNamespace + "webDocumentType");
			Value tripleObject;
			if (ru.nextBoolean()) {
				tripleObject = sesameValueFactory.createURI(bbcNamespace + "HighWeb");
			} else {
				tripleObject = sesameValueFactory.createURI(bbcNamespace + "Mobile");
			}

			model.add(tripleSubject, triplePredicate, tripleObject, context);

			object = sesameValueFactory.createTriple(tripleSubject, (IRI) triplePredicate, tripleObject);

			model.add(rdfFormat == RDFFormat.TRIGSTAR ? embeddedTriple : subject, predicate,
					rdfFormat == RDFFormat.TRIGSTAR ? object : tripleObject, context);
		}
		
		return model;
	}
}
