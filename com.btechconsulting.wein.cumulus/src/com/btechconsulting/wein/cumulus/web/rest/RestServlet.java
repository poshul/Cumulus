package com.btechconsulting.wein.cumulus.web.rest;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import org.apache.bcel.generic.NEW;
import org.apache.log4j.Logger; 

import com.btechconsulting.wein.cumulus.initialization.Initializer;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 
 * @author samuel
 *
 */

public class RestServlet extends HttpServlet{

	private static Properties props = null;
	private static final String CONFIG_INIT_PARAM ="config";
	private static final String CREDENTIALS_PARAM = "credentials";
	private static Logger logger = Logger.getLogger(RestServlet.class);
	public static final int MAX_NUMBER_TO_SEARCH = 1;

	public void init(ServletConfig servletConfig) throws ServletException {
		super.init(servletConfig);

		if (props == null){

			String configFile = servletConfig.getInitParameter(CONFIG_INIT_PARAM);
			String credentialsFile= servletConfig.getInitParameter(CREDENTIALS_PARAM);
			if (configFile != null){
				try {
					URL url = getServletContext().getResource(configFile);

					//If the config isn't in the servlet context, try the class loader
					//which allows the config files to be stored in a jar
					if (url== null){
						url = getClass().getResource(configFile);
					}

					if (url == null) {
						logger.error("No configuration found for RestServlet: " + configFile);
						throw new ServletException("No configuration found for RestSearchAction: " + configFile);
					}

					props = new Properties();
					props.load(url.openStream());
					logger.warn("REST servlet configured");
					logger.debug(props.toString());
				}
				catch (IOException ioe) {
					props=null;
					logger.error(ioe.getMessage(), ioe);
					throw new ServletException("Error during RestServlet configuration: " + ioe.getMessage(), ioe);
				}
			} else {
				throw new ServletException("No configuration file specified for RestServlet");
			}
			
			Initializer.getInstance(servletConfig);//load the Initializer class into memory

		} 
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String requestType = request.getPathInfo();
		if (requestType != null) {
			if (props !=null) {
				String handlerClassName = props.getProperty(requestType);
				if (handlerClassName != null){

					try {
						Class handlerClass = Class.forName(handlerClassName);
						RestHandler processor = (RestHandler) handlerClass.newInstance();
						logger.debug("Got instance of processor.");
						processor.executeSearch(request, response);

					} catch (ClassNotFoundException cnfe){
						logger.error(cnfe.getMessage(),cnfe);
						throw new ServletException("No handler class available to process request: " + cnfe.getMessage(), cnfe);
					} catch (ClassCastException cce){
						logger.error(cce.getMessage(),cce);
						throw new ServletException("Class "+ handlerClassName+ " does not implement the required RestHandler interface");
					} catch (IllegalAccessException iae){
						logger.error(iae.getMessage(),iae);
						throw new ServletException("Class "+ handlerClassName + " does not allow such access");
					} catch (InstantiationException ie){
						logger.error(ie.getMessage(),ie);
						throw new ServletException("Class "+ handlerClassName+ " could not be instantiated");
					}

				} else {
					logger.error("No handler class defined for request type: "+ requestType);
					throw new ServletException("No handler class defined for request type: "+ requestType);
				}
			} else {
				logger.error("REST Servlet improperly configured!");
				throw new ServletException("REST Servlet improperly configured!");
			}
		} else {
			logger.error("No request type defined for REST processor");
			throw new ServletException("No request type defined for REST processor");
		}
	}
}