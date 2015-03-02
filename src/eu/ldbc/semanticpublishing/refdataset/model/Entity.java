package eu.ldbc.semanticpublishing.refdataset.model;

import java.util.ArrayList;

/**
 * A class for storing entities from different dataset's. Used to initialize the database and build insert queries.
 */
public class Entity {
	public static final String ENTITY_RDFS_LABEL = "rdfs:label";
	public static final String ENTITY_ABOUT = "entity:about";
	public static final String ENTITY_CATEGORY = "entity:category";
	public static final String ENTITY_RANK = "entity:rank";
	
	private String aboutURI;	
	private ArrayList<Triple> triplesList = new ArrayList<Triple>();
	
	public Entity() {
	}
	
	public Entity(String URI, 
				  String rdfsLabel, 
				  String mainAboutUri, 
				  String category,
				  String rank) {
		this.aboutURI = URI;
		addTriple(ENTITY_RDFS_LABEL, rdfsLabel);
		addTriple(ENTITY_ABOUT, mainAboutUri);
		addTriple(ENTITY_CATEGORY, category);
		addTriple(ENTITY_RANK, rank);
	}
	
	public String getURI() {
		return this.aboutURI;
	}
	
	public void setURI(String uri) {
		this.aboutURI = uri;
	}
	
	public String getLabel() {
		return getObjectFromTriple(ENTITY_RDFS_LABEL);
	}
	
	public void setLabel(String label) { 
		addTriple(ENTITY_RDFS_LABEL, label);
	}
	
	public String getCategory() {
		return getObjectFromTriple(ENTITY_CATEGORY);
	}
	
	public void setCategory(String category) {
		addTriple(ENTITY_CATEGORY, category);
	}	
	
	public String getRank() {
		return getObjectFromTriple(ENTITY_RANK);
	}
	
	public void setRank(String rank) {
		addTriple(ENTITY_RANK, rank);
	}
	
	public void addTriple(String predicate, String object) {
		Triple et = new Triple(getURI(), predicate, object);
		triplesList.add(et);
	}
	
	public void addTriple(String subject, String predicate, String object) {
		Triple et = new Triple(subject, predicate, object);
		triplesList.add(et);		
	}
	
	public String getSubjectFromTriple(String predicate) {
		for (Triple et : triplesList) {
			if (et.getPredicate().equals(predicate)) {
				return et.getSubject();
			}
		}
		return "";
	}
	
	public String getObjectFromTriple(String predicate) {
		for (Triple et : triplesList) {
			if (et.getPredicate().equals(predicate)) {
				return et.getObject();
			}
		}
		return "";
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Entity)) {
			return false;
		}
		return (((Entity)obj).getURI().equals(getURI()));
	}

	@Override
	public int hashCode() {
		return getURI().hashCode();
	}
}
