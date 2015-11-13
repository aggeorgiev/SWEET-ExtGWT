package uk.ac.kmi.microwsmo.server.servlet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;

import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpsURL;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.http.HttpRequest;
import org.restlet.Client;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Protocol;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.DomRepresentation;
import org.restlet.resource.StringRepresentation;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.tidy.Tidy;

import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.HTTPHeader;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ResponseTextHandler;
//import com.google.gwt.user.client.impl.HTTPRequestImpl;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.appengine.api.urlfetch.URLFetchServiceFactory;

import org.apache.commons.httpclient.HttpClient;


import java.util.Properties;
import java.util.UUID;

/**
 * This servlet is basically a proxy. It retrieve the web page and send it back
 * to a page inside the server domain. An iframe is pointed to this proxy page
 * and every operation client side is allowed. This because the javascript function
 * works on a page that is in the same domain and so, they don't violate the
 * sand box's restrictions.
 * 
 * @author KMi, The Open University 
 */
public class Proxy extends HttpServlet {
	
	private static final long serialVersionUID = -7809356775586825102L;
	private URL url;
	private URLConnection connection;
	private Tidy parser;
	
	/**
	 * Redirects the user to an error page. It means the user is not allowed to
	 * calls directly the page by a GET method.
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.sendRedirect("405-method-not-allowed.html");
	}

	/**
	 * Is the method which manages the client request.
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// retrieve the url from the query string
		String address = request.getParameter("url");
		// decodes the url
		address = URLDecoder.decode(address, "UTF-8");
		
		//Save the address in the session
		HttpSession session = request.getSession(true);
	    session.setAttribute("apiURL", address);
	    
	    //Create a new process and put it in the session
	    String Id = UUID.randomUUID().toString();
	    String processID = "http://www.soa4all.eu/process/"+ Id;
	    session.setAttribute("processId", processID);
	    
		// instantiates a new URL object
		url = new URL(address);
		// create a DOM from the web page
		Document document = getDOM();

		// manipulate it, changing the relative paths, in absolute
		document = manipulateDOM(document);
		
		// serialize the DOM and send the serialized version back to the client
		serializeDOM(document, response);
	}
	
	/**
	 * Makes up the DOM from a web page.
	 * 
	 * @return an object which represent the DOM
	 * @throws IOException
	 */
	private Document getDOM() throws IOException {
		// open the connection toward the server which own the web page
		connection = url.openConnection();
		
		int lenght = connection.getContentLength();
		
		/*Properties prop = System.getProperties();
		prop.put("http.proxyHost", "wwwcache.open.ac.uk");
		prop.put("http.proxyPort", "80");*/
		
		if(lenght == -1){

			HttpClient client = new HttpClient();
			//client.getHostConfiguration().setProxy("wwwcache.open.ac.uk", 80);
			HttpMethodBase httpMethod = null;
			httpMethod = new GetMethod(url.toString());
			client.executeMethod(httpMethod);
			
			String responseString = httpMethod.getResponseBodyAsString();

			ByteArrayInputStream responseStream = new ByteArrayInputStream(responseString.getBytes("UTF-8")); 
			//
			/*
			 * Retrieve the DOM from the page by the Tidy parser.
			 * For further information: <http://tidy.sourceforge.net>,
			 * <http://www.w3.org/People/Raggett/tidy>
			 */
			
			parser = new Tidy();
			parser.setShowWarnings(false);
			parser.setXmlTags(false);
			parser.setInputEncoding("UTF-8");
			parser.setOutputEncoding("UTF-8");
			parser.setXHTML(true);// 
			parser.setMakeClean(true);


			Document document = parser.parseDOM(responseStream, null);
			//parser.pprint(document, System.out);
			return document;
			
		} else {
			connection.connect();
			
			InputStream inputStream = connection.getInputStream();
			
			//
			/*
			 * Retrieve the DOM from the page by the Tidy parser.
			 * For further information: <http://tidy.sourceforge.net>,
			 * <http://www.w3.org/People/Raggett/tidy>
			 */
			parser = new Tidy();
			Document document = parser.parseDOM(inputStream, null);
			return document;
		}
	}
	
	/**
	 * Change every link of the page, from relative to absolute.
	 * Basically the algorithm change the value of the attributes:
	 * "src", "href" and "action".
	 * 
	 * @param document the DOM to change
	 */
	private Document manipulateDOM(Document document) {
		NodeList elements = document.getElementsByTagName("*");
		Node href = null;
		Node src = null;
		for( int i = 0; i < elements.getLength(); i++ ) {
			Node element = elements.item(i);
			String elementName = element.getNodeName();
			NamedNodeMap attributes = element.getAttributes();
			// if the tag element <script> contains inline code
			if( elementName.equals("script") && attributes.getNamedItem("src") == null ) {
				urlInspector(element);
			} else {
				// rewrite the url value of the "href" attribute
				href = attributes.getNamedItem("href");
				if( href != null ) {
					// if the tag is an <a> element
					if( elementName.equals("a") ) {
						href.setNodeValue("javascript:callProxy('" + rewriteURL(href.getNodeValue()) + "')");
					// otherwise set only the absolute path
					} else {
						href.setNodeValue(rewriteURL(href.getNodeValue()));
					}
				}
				// rewrite the url value of the "src" attribute 
				src = attributes.getNamedItem("src");
				if( src != null ) {
					src.setNodeValue(rewriteURL(src.getNodeValue()));
				}
			}
		}
		return document;
	}
	
	/**
	 * Serialize the DOM and send it back to the client.
	 * 
	 * @param document the DOM
	 * @param response the object which represent the comunication from the servlet to the client.
	 * @throws IOException
	 */
	private void serializeDOM(Document document, HttpServletResponse response) throws IOException {
		response.setContentType("text/html");
		OutputStream out = response.getOutputStream();
		parser.pprint(document, out);
		out.close();
	}
	
	/**
	 * 
	 * @param element
	 */
	private void urlInspector(Node element) {
		/*
		 * TODO: this method is used for the tag such as
		 * <script> or <style> which contain inline code.
		 */
	}
	
	/**
	 * This method rewrite the URL from relative to absolute.
	 * 
	 * @param urlString is the URL string that has to be rewrited.
	 * @return the rewrited URL in absolute way.
	 */
	private String rewriteURL(String urlString) {
		if( urlString.startsWith("http") || urlString.startsWith("ftp") || urlString.startsWith("file") ) {
			return urlString;
		} else if (urlString.startsWith("/")) {
			return getDomain() + urlString;
		} else {
			return getCurrentPath() + urlString;
		}
	}
	
	/**
	 * Returns the name server domain.
	 * @return a string which represent the URL of the server.
	 */
	private String getDomain() {
		String protocol = url.getProtocol();
		String host = url.getHost();
		return protocol + "://" + host;
	}
	
	/**
	 * Returns the name of the URL, where the resource is stored inside
	 * the server.
	 * @return a string which represent the URL of the resource 
	 */
	private String getCurrentPath() {
		String path = url.getPath();
		String[] split = path.split("/");
		path = "/";
		for( int i = 0; i < split.length - 1; i++) {
			path += split[i];
		}
		return getDomain() + "/" + path + "/";
	}

}
