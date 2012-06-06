/**
 * 
 */
package com.btechconsulting.wein.cumulus.web.rest;

import java.io.IOException;
import java.io.Writer;
import java.sql.Connection;
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

import com.btechconsulting.wein.cumulus.CreateShortReturn;
import com.btechconsulting.wein.cumulus.initialization.Initializer;
import com.btechconsulting.wein.cumulus.initialization.PooledConnectionFactory;
import com.btechconsulting.wein.cumulus.model.Results;

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
		Boolean isDone=false;
		try{
			if(Initializer.getInstance().getNumberOfWorkUnitsInFlight(ownerId, jobId)==0){
				isDone=true;
			}
		}
		catch(IllegalStateException ise){
			String error=ise.getMessage();
			response.setStatus(500);
			writer.write(CreateShortReturn.createShortResponse(error, true));
			return;
		}
		//only proceed if we are done
		if (isDone){

			//get all of the results from the database
			String query="SELECT results FROM cumulus.results WHERE owner_id='"+ownerId+"' AND job_id='"+jobId+"';";
			List<String> resultpdbqts =new ArrayList<String>();
			Statement stmt= null;
			try {
				//FIXME intermittant error here
				Connection conn = PooledConnectionFactory.INSTANCE.getCumulusConnection();
				stmt= conn.createStatement();
				ResultSet results= stmt.executeQuery(query);
				while (results.next()){ //go through all of the results that we have
					resultpdbqts.add(results.getString(1));
				}
				conn.close();
			} catch (SQLException e) {
				// TODO deal with ramifications of a sql exception
				e.printStackTrace();
			}
			//set up the response
			response.setStatus(200);
			response.addIntHeader(JOBID, jobId);
			//get the output stream
			//create a results object for the results.
			Results resultz= new Results();
			//put each result in the results object
			for(String i:resultpdbqts){
				resultz.getResult().add(i);
			}
			try {
				JAXBContext context = JAXBContext.newInstance(Results.class);
				Marshaller m = context.createMarshaller();
				m.marshal(resultz, writer);
			} catch (JAXBException e1) {
				e1.printStackTrace();
				String error="Couldn't marshall results";
				response.setStatus(500);
				writer.write(CreateShortReturn.createShortResponse(error, true));
				return;
			}

		}else{
			String error="Job is not finished yet";
			response.setStatus(400);
			writer.write(CreateShortReturn.createShortResponse(error, true));
			return;
		}
	}

}
