/**
 * 
 */
package com.btechconsulting.wein.cumulus.web.rest;

import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;
import java.sql.Statement;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.log4j.Logger;

import com.btechconsulting.wein.cumulus.initialization.Initializer;
import com.btechconsulting.wein.cumulus.initialization.PooledConnectionFactory;
import com.btechconsulting.wein.cumulus.model.ShortResponse;

/**
 * @author samuel
 *
 */
public class DoDeleteResultsHandler implements RestHandler {


	public static final String OWNERID="ownerId";
	public static final String JOBID="jobId";
	private final Logger logger= Logger.getLogger(DoDeleteResultsHandler.class);

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
			if(Initializer.getInstance().getNumberOfWorkUnitsInFlight(ownerId, jobId)==0){
				isDone=true;
			}
		}
		catch(IllegalStateException ise){
			throw (new ServletException(ise.getMessage()));
			//TODO deal with invalid owner and job ID's 
		}
		if(isDone){
			//delete results from sql
			String query="DELETE FROM cumulus.results WHERE owner_id='"+ownerId+"' and job_id='"+jobId+"';";
			try {
				Statement stmt= PooledConnectionFactory.INSTANCE.getCumulusConnection().createStatement();
				stmt.executeUpdate(query);
				Writer writer = response.getWriter();
				response.setContentType("text/xml");
				response.setStatus(200);
				//Build the response xml
				JAXBContext context = JAXBContext.newInstance(ShortResponse.class);
				Marshaller m = context.createMarshaller();
				ShortResponse responseXml= new ShortResponse();
				responseXml.setIsError(false);
				responseXml.setResponse("Job "+jobId+" deleted.");
				m.marshal(responseXml, writer);

			} catch (JAXBException e3) {
				// TODO Auto-generated catch block
				e3.printStackTrace();
				throw new ServletException("Couldn't marshall result");
			} catch (SQLException e) {
				e.printStackTrace();
				throw new ServletException("Couldn't connect to SQL:"+e);
			}
			//delete job from local store
			Initializer.getInstance().removeJobFromServer(ownerId, jobId);
		}else{
			throw (new ServletException("Job is not finished yet"));
		}
	}

}
