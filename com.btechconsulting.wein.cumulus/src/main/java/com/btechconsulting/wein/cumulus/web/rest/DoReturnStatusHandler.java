/**
 * 
 */
package com.btechconsulting.wein.cumulus.web.rest;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.log4j.Logger;
import org.apache.tomcat.dbcp.dbcp.PoolableConnectionFactory;

import com.btechconsulting.wein.cumulus.initialization.Initializer;
import com.btechconsulting.wein.cumulus.initialization.PooledConnectionFactory;
import com.btechconsulting.wein.cumulus.model.Results;
import com.btechconsulting.wein.cumulus.web.rest.RestHandler;

/**
 * @author samuel
 *
 */
public class DoReturnStatusHandler implements RestHandler {

	public static final String OWNERID="ownerId";
	public static final String JOBID="jobId";
	private final Logger logger= Logger.getLogger(DoReturnStatusHandler.class);




	/* (non-Javadoc)
	 * @see com.btechconsulting.wein.cumulus.web.rest.RestHandler#executeSearch(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	public void executeSearch(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		//Parse out arguments
		String[] ownerIds= request.getParameterValues(OWNERID);
		if (ownerIds==null|| ownerIds.length!=1){
			logger.debug("User didn't supply an ownerID");
			throw new ServletException("You must supply an ownerID");
		}
		String ownerId=ownerIds[0];

		String[] jobIds= request.getParameterValues(JOBID);
		if (jobIds==null|| jobIds.length!=1){
			logger.debug("User didn't supply an jobID");
			throw new ServletException("You must supply a jobID");
		}
		Integer jobId=Integer.valueOf(jobIds[0]);//TODO handle parse exception here
		Boolean isDone=false;
		try{
			Integer numLeft=Initializer.getInstance().getNumberOfWorkUnitsInFlight(ownerId, jobId); 
			//set up the response
			response.setContentType("text/html");
			response.setStatus(200);
			response.addIntHeader(JOBID, jobId);
			//get the output stream
			Writer out = response.getWriter();
			//only proceed if we are done
			if (numLeft==0){
				out.write("done");
			}else{
				out.write(numLeft.toString());
			}
		}
		catch(IllegalStateException ise){
			throw (new ServletException(ise.getMessage()));
			//TODO deal with invalid owner and job ID's 
		}
		
	}

}
