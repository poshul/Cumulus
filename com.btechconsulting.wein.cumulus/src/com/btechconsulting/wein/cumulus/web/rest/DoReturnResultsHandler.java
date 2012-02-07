/**
 * 
 */
package com.btechconsulting.wein.cumulus.web.rest;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.btechconsulting.wein.cumulus.initialization.Initializer;

/**
 * @author samuel
 *
 */
public class DoReturnResultsHandler implements RestHandler {
	
	public static final String OWNERID="ownerId";
	public static final String JOBID="jobId";
	private final Logger logger= Logger.getLogger(DoReturnResultsHandler.class);

	
	

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
			if(Initializer.INSTANCE.getNumberOfWorkUnitsInFlight(ownerId, jobId)==0){
				isDone=true;
			}
		}
		catch(IllegalStateException ise){
			throw (new ServletException(ise.getMessage()));
		}
		//only proceed if we are done
		if (isDone){
			response
		}else{
			throw (new ServletException("Job is not finished yet"));
		}
	}

}
