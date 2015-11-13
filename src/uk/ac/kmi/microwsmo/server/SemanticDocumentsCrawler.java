package uk.ac.kmi.microwsmo.server;

import java.rmi.RemoteException;
import java.security.CodeSource;
import java.util.ArrayList;

import javax.xml.rpc.ServiceException;

//import uk.ac.open.kmi.watson.clientapi.OntologySearch;
//import uk.ac.open.kmi.watson.clientapi.OntologySearchServiceLocator;
//import uk.ac.open.kmi.watson.clientapi.WatsonService;
import uk.ac.open.kmi.watson.clientapi.SearchConf;
import uk.ac.open.kmi.watson.clientapi.SemanticContentResult;
import uk.ac.open.kmi.watson.clientapi.SemanticContentSearch;
import uk.ac.open.kmi.watson.clientapi.SemanticContentSearchServiceLocator;

public class SemanticDocumentsCrawler {

	/** the engine for the watson queries  */
	//private OntologySearch ontoEngine;
	private SemanticContentSearch ontoEngine;
	private SubsetStore subsetStore;
	
	private static final int DELTA = 10;
	
	public SemanticDocumentsCrawler() throws ServiceException {
		SemanticContentSearchServiceLocator locator = new SemanticContentSearchServiceLocator();
		try{
			ontoEngine = locator.getUrnSemanticContentSearch();
		}
		catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.getCause());
		}		
		//ontoEngine = new OntologySearchServiceLocator().getUrnOntologySearch();
		subsetStore = new SubsetStore();
	}
	
	/**
	 * Retrieve the semantic documents related to the keyword.
	 * 
	 * @param keyword the keyword which is contained into the retrieved ontologies.
	 * @throws RemoteException
	 */
	@SuppressWarnings("static-access")
	public String[] searchByKeywordPaginated(String keyword) throws RemoteException {
		Subset subset = subsetStore.getSubset(keyword);
		String[] keywords = {keyword};
		SearchConf conf = new SearchConf();
		conf.setScope(SearchConf.LABEL+conf.LOCAL_NAME+conf.LITERAL);
		conf.setEntities(SearchConf.CLASS+SearchConf.INDIVIDUAL+conf.PROPERTY);
		conf.setMatch(SearchConf.EXACT_MATCH);
//		String[][] filters = {{"language","OWL"}};
//		conf.setFilters(filters);		
		conf.setEntitiesInfo(SearchConf.ENT_TYPE_INFO+SearchConf.ENT_ANYRELATIONFROM_INFO+SearchConf.ENT_ANYRELATIONTO_INFO);
		conf.setInc(DELTA);
		
        // conf.setEntities(SearchConf.ENT_ANYRELATIONFROM_INFO+SearchConf.ENT_ANYRELATIONTO_INFO);
		
		//int scopeModifier = WatsonService.LOCAL_NAME + WatsonService.LABEL + WatsonService.LITERAL;
		//int entityTypeModifier = WatsonService.CLASS + WatsonService.PROPERTY + WatsonService.INDIVIDUAL;
		//int matchTechnique = WatsonService.EXACT_MATCH;
//		System.out.println("Predi Atanas2");
//		String[] params = {"cat"};
//		SearchConf conf = new SearchConf();
//		conf.setEntities(SearchConf.CLASS+SearchConf.INDIVIDUAL+SearchConf.PROPERTY);
//		conf.setScope(SearchConf.LOCAL_NAME+SearchConf.LITERAL);
//		conf.setMatch(SearchConf.TOKEN_MATCH);
//		conf.setSCInfo(SearchConf.SC_DOMAIN_INFO+SearchConf.SC_LANGUAGES_INFO+SearchConf.SC_ENTITIES_INFO);
//		String[][] filters = {{"language","OWL"}};
//		conf.setFilters(filters);
//		conf.setInc(10);		
		
		SemanticContentResult[] res = ontoEngine.getSemanticContentByKeywords(keywords, conf);
//		SemanticContentResult[] res =  ontoEngine.getSemanticContentByKeywords(params, conf);

		ArrayList<String> e = new ArrayList<String>();
		//String[] result = ontoEngine.getSemanticContentByKeywordsWithRestrictionsPaginated(keywords, scopeModifier, entityTypeModifier, matchTechnique, subset.getStartPosition(), DELTA);

		for (SemanticContentResult s : res) {
             e.add(s.getURI());
		 }		

		String[] result = e.toArray(new String[e.size()]);
		if( subset.isFirstSearch() ) {
			subset.setSize(res.length - 1);
			subset.setFirstSearch(false);
		}
		subset.incrementStartPositionOf(DELTA);
		//if( subset.getStartPosition() >= subset.getSize() ) {
		if( res.length < 1 ) {
			result[res.length - 1] = "0";
		} else {
			result[res.length - 1] = String.valueOf(subset.getSize() - subset.getStartPosition());
			//result[res.length - 1] = String.valueOf(res.length);
		}
		return result;
	}
	
}
