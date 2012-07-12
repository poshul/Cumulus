/**
 * 
 */
package com.btechconsulting.wein.cumulus.web.rest;

import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.log4j.Logger;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.btechconsulting.wein.cumulus.CreateShortReturn;
import com.btechconsulting.wein.cumulus.model.FilterParams;
import com.btechconsulting.wein.cumulus.workUnitGenerator.DetermineWorkToDo;

/**
 * @author samuel
 *
 */
public class DoFindJobSizeHandler implements RestHandler {
	public static final String OWNERID="ownerId";
//	public static final String VINAPARAMS="vinaParams";
//	public static final String RECEPTOR="receptor";
	public static final String FILTERPARAMS="filterParams";
	public static final String JOBSIZE="jobSize";
	private final Logger logger= Logger.getLogger(DoSearchHandler.class);
	public void executeSearch(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		
		response.setContentType("text/xml");
		Writer writer= response.getWriter();

		//parse input to make sure it is compliant with spec.
		String[] ownerIds= request.getParameterValues(OWNERID);
		if (ownerIds==null|| ownerIds.length!=1){
			logger.debug("User didn't supply an ownerId");
			System.err.println(ownerIds);
			String error="You must supply an ownerId";
			response.setStatus(400);
			writer.write(CreateShortReturn.createShortResponse(error, true));
			return;
		}
		String ownerId=ownerIds[0];

		String[] filterParamss= request.getParameterValues(FILTERPARAMS);
		if (filterParamss==null||filterParamss.length!=1){
			logger.debug("User didn't supply any filterParams");
			String error="You must supply one set of filterParams";
			response.setStatus(400);
			writer.write(CreateShortReturn.createShortResponse(error, true));
			return;
		}
		String filterParams= filterParamss[0];

		//test that FilterParams is well formed
		FilterParams filterParamsObj= new FilterParams();
		try{
			JAXBContext context = JAXBContext.newInstance(FilterParams.class);
			Unmarshaller um = context.createUnmarshaller();
			//we save this one so we don't have to re-unmarshal it later
			filterParamsObj=(FilterParams) um.unmarshal(new StringReader(filterParams));
		}
		catch (JAXBException jbe) {
			logger.warn("user supplied badly formed filterParams");
			String error="Please make sure the filterParams is well formatted";
			response.setStatus(400);
			writer.write(CreateShortReturn.createShortResponse(error, true));
			return;
		}

		try {
			DetermineWorkToDo work= new DetermineWorkToDo(null, ownerId, filterParamsObj);
			Integer jobSize= work.FilterCompoundsInDatabase().size();
			response.setStatus(200);
			//response.addIntHeader(JOBID, jobId);
			writer.write(CreateShortReturn.createShortResponse(jobSize.toString(), false));
		}
		//TODO return number of units created to 
		catch (AmazonServiceException ase) {
			logger.error(ase);
			String error="Error in AWS please try again later.";
			response.setStatus(500);
			writer.write(CreateShortReturn.createShortResponse(error, true));
			return;
		} catch (AmazonClientException ace) {
			logger.error(ace);
			String error="Internal error please try again";
			response.setStatus(500);
			writer.write(CreateShortReturn.createShortResponse(error, true));
			return;
		} catch (SQLException sqle) {
			logger.error(sqle);
			String error="Error connecting to SQL please try again later";
			response.setStatus(500);
			writer.write(CreateShortReturn.createShortResponse(error, true));
			return;
		}



	}

}
