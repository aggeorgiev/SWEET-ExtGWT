package uk.ac.kmi.microwsmo.server;

import java.rmi.RemoteException;

import javax.xml.rpc.ServiceException;

import uk.ac.open.kmi.watson.clientapi.SemanticContentSearch;

public class test {
	private static SemanticContentSearch ontoEngine;

	/**
	 * @param args
	 * @throws ServiceException 
	 */
	public static void main(String[] args) throws ServiceException {
		SemanticDocumentsCrawler s = new SemanticDocumentsCrawler();
		try {
			s.searchByKeywordPaginated("cat");
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
