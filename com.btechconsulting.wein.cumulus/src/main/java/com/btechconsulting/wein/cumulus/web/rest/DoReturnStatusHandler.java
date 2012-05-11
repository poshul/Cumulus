/**
 * 
 */
package com.btechconsulting.wein.cumulus.web.rest;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.btechconsulting.wein.cumulus.CreateShortReturn;
import com.btechconsulting.wein.cumulus.initialization.Initializer;

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
		
		Writer writer = response.getWriter();
		response.setContentType("text/xml");

		//Parse out arguments
		String[] ownerIds= request.getParameterValues(OWNERID);
		if (ownerIds==null|| ownerIds.length!=1){
			logger.debug("User didn't supply an ownerID");
			String error="You must supply an ownerID";
			response.setStatus(400);
			writer.write(CreateShortReturn.createShortResponse(error, true));
			return;
		}
		String ownerId=ownerIds[0];

		String[] jobIds= request.getParameterValues(JOBID);
		if (jobIds==null|| jobIds.length!=1){
			logger.debug("User didn't supply an jobID");
			String error="You must supply a jobID";
			response.setStatus(400);
			writer.write(CreateShortReturn.createShortResponse(error, true));
			return;
		}
		Integer jobId=Integer.valueOf(jobIds[0]);//TODO handle parse exception here
		//Boolean isDone=false;
		try{
			Integer numLeft=Initializer.getInstance().getNumberOfWorkUnitsInFlight(ownerId, jobId); 
			//set up the response
			response.setStatus(200);
			response.addIntHeader(JOBID, jobId);
			//only proceed if we are done
			String toWrite;
			if (numLeft==0){
				toWrite="done";
			}else{
				toWrite=numLeft.toString();
			}
			response.setStatus(200);
			writer.write(CreateShortReturn.createShortResponse(toWrite, false));
		}
		catch(IllegalStateException ise){
			String error=ise.getMessage();
			response.setStatus(400);
			writer.write(CreateShortReturn.createShortResponse(error, true));
			return;
			//TODO deal with invalid owner and job ID's 
		}
		
	}

}
