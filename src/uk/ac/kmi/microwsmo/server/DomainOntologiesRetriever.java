package uk.ac.kmi.microwsmo.server;

import java.io.IOException;
import java.rmi.RemoteException;

import javax.xml.rpc.ServiceException;

import uk.ac.open.kmi.watson.clientapi.EntitySearch;
import uk.ac.open.kmi.watson.clientapi.EntitySearchServiceLocator;
import uk.ac.open.kmi.watson.clientapi.EntityResult;
//import uk.ac.open.kmi.watson.clientapi.OntologySearch;
//import uk.ac.open.kmi.watson.clientapi.OntologySearchServiceLocator;
import uk.ac.open.kmi.watson.clientapi.SearchConf;
import uk.ac.open.kmi.watson.clientapi.SemanticContentSearch;
import uk.ac.open.kmi.watson.clientapi.SemanticContentSearchServiceLocator;

public class DomainOntologiesRetriever {

	private EntitySearch entityEngine;
	//private OntologySearch ontoEngine;
	private SemanticContentSearch ontoEngine;
	private SemanticDocumentsCrawler sdCrawler;
	
	public DomainOntologiesRetriever() throws ServiceException {
		entityEngine = new EntitySearchServiceLocator().getUrnEntitySearch();
		
		
		//ontoEngine = new OntologySearchServiceLocator().getUrnOntologySearch();
		SemanticContentSearchServiceLocator locator = new SemanticContentSearchServiceLocator();
		ontoEngine = locator.getUrnSemanticContentSearch();			
		
		sdCrawler = new SemanticDocumentsCrawler();
	}
	
	/**
	 * 
	 * @return
	 * @throws IOException
	 */
	@SuppressWarnings("static-access")
	public String getDomainOntologies(String keyword) throws IOException {
		String[] semanticDocuments = sdCrawler.searchByKeywordPaginated(keyword);
		String result = semanticDocuments[semanticDocuments.length - 1] + "<!>";
        SearchConf conf = new SearchConf();
        conf.setScope(SearchConf.LABEL+conf.LOCAL_NAME+conf.LITERAL);
        conf.setEntities(SearchConf.CLASS+SearchConf.INDIVIDUAL+conf.PROPERTY);
        conf.setMatch(SearchConf.TOKEN_MATCH);
        conf.setEntitiesInfo(SearchConf.ENT_TYPE_INFO+SearchConf.ENT_ANYRELATIONFROM_INFO+SearchConf.ENT_ANYRELATIONTO_INFO);			
		for( int i = 0; i < semanticDocuments.length - 1; i++ ) {
			String ontoURI = semanticDocuments[i];
			result += ontoURI;
			//String[] entities = entityEngine.getEntitiesByKeyword(ontoURI, keyword);		
	        EntityResult[] entities = entityEngine.getEntitiesByKeyword(ontoURI, keyword, conf);
	        if (entities!=null) for (EntityResult entity : entities) {
			//for( String entityURI : entities ) {
				//String type = entityEngine.getType(ontoURI, entityURI);
	        	String type = entity.getType();
				result += "<:>" + type + "<:>" + entity.getURI();
			}
			result += "<&>";
		}
		// drops off the last "<&>" or "<!>" from the string 
		result = result.substring(0, result.length() - 3);
		// returns the result
		return result;
	}
	
	public String getConceptsOf(String url) throws RemoteException {
		String[] concepts = ontoEngine.listClasses(url);
		String result = "";
		for( String concept : concepts ) {
			result += concept.split("#")[1] + "<:>";
		}
		if( !result.equals("") ) {
			result = result.substring(0, result.length() - 3);
		}
		return result;
	}
	
	/**
	 * 
	 * @param result
	 */
	public void showDomainOntologies(String result) {
		if( result.startsWith("0") ) {
			System.out.println("no results");
			return;
		}
		// if is not empty then, show the result
		String[] split = result.split("<!>")[1].split("<&>");
		for( String s : split ) {
			String[] chunk = s.split("<:>");
			System.out.println("Ontology: " + chunk[0]);
			for( int i = 1; i < chunk.length; i++ ) {
				if( (i % 2) != 0 ) {
					System.out.print("    " + chunk[i]  + " - ");
				} else {
					System.out.println(chunk[i]);
				}
			}
			System.out.println();
		}
	}
	
}