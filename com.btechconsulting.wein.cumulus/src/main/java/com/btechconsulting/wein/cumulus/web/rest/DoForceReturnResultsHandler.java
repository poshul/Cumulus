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
public class DoForceReturnResultsHandler implements RestHandler {

	public static final String OWNERID="ownerId";
	public static final String JOBID="jobId";
	private final Logger logger= Logger.getLogger(DoForceReturnResultsHandler.class);




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
		/*Boolean isDone=false;
		try{
			if(Initializer.getInstance().getNumberOfWorkUnitsInFlight(ownerId, jobId)==0){
				isDone=true;
			}
		}
		catch(IllegalStateException ise){
			throw (new ServletException(ise.getMessage()));
			//TODO deal with invalid owner and job ID's 
		}*/
		//only proceed if we are done
/*		if (isDone){
*/			//TODO parse all results, return a list of ERROR, and DONE

			//get all of the results from the database
			String query="SELECT results FROM cumulus.results WHERE owner_id='"+ownerId+"' AND job_id='"+jobId+"';";
			List<String> resultpdbqts =new ArrayList<String>();
			Statement stmt= null;
			try {
				stmt= PooledConnectionFactory.INSTANCE.getCumulusConnection().createStatement();
				ResultSet results= stmt.executeQuery(query);
				while (results.next()){ //go through all of the results that we have
					resultpdbqts.add(results.getString(1));
				}
			} catch (SQLException e) {
				// TODO deal with ramifications of a sql exception
				e.printStackTrace();
			}
			//set up the response
			response.setContentType("text/xml");
			response.setStatus(200);
			response.addIntHeader(JOBID, jobId);
			//get the output stream
			Writer out = response.getWriter();
			//create a results object for the results.
			Results results= new Results();
			//put each result in the results object
			for(String i:resultpdbqts){
				results.getResult().add(i);
			}
			try {
				JAXBContext context = JAXBContext.newInstance(Results.class);
				Marshaller m = context.createMarshaller();
				m.marshal(results, out);
			} catch (JAXBException e1) {
				e1.printStackTrace();
				throw new ServletException(e1);
			}
			
			/*//delete results from sql
			query="DELETE FROM cumulus.results WHERE owner_id='"+ownerId+"' and job_id='"+jobId+"';";
			try {
				stmt.executeUpdate(query);
			} catch (SQLException e) {
				// TODO deal with ramifications of sql exception
				e.printStackTrace();
			}
			//delete job from local store
			Initializer.getInstance().removeJobFromServer(ownerId, jobId);*/

/*		}else{
			throw (new ServletException("Job is not finished yet"));
		}*/
	}

}
