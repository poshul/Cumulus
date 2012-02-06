/**
 * 
 */
package com.btechconsulting.wein.cumulus.web.rest;

import java.io.IOException;
import java.io.StringReader;
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
import com.btechconsulting.wein.cumulus.model.FilterParams;
import com.btechconsulting.wein.cumulus.model.VinaParams;
import com.btechconsulting.wein.cumulus.workUnitGenerator.WorkUnitGenerator;

/**
 * @author samuel
 *
 */
public class DoSearchHandler implements RestHandler {
	public static final String OWNERID="ownerId";
	public static final String VINAPARAMS="vinaParams";
	public static final String RECEPTOR="receptor";
	public static final String FILTERPARAMS="filterParams";
	public static final String JOBID="jobId";
	private final Logger logger= Logger.getLogger(DoSearchHandler.class);
	


	public void executeSearch(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		//parse input to make sure it is compliant with spec.
		String[] ownerIds= request.getParameterValues(OWNERID);
		if (ownerIds==null|| ownerIds.length!=1){
			logger.debug("User didn't supply an ownerID");
			throw new ServletException("You must supply an ownerID");
		}
		String ownerId=ownerIds[0];
		
		String[] vinaParamss= request.getParameterValues(VINAPARAMS);
		if (vinaParamss==null||vinaParamss.length!=1){
			logger.debug("User didn't supply any vinaParams");
			throw new ServletException("You must supply one set of vinaparams");
		}
		String vinaParams=vinaParamss[0];
		
		String[] receptors= request.getParameterValues(RECEPTOR);
		if (receptors==null||receptors.length!=1){
			logger.debug("User didn't supply a receptor");
			throw new ServletException("You must supply one receptor");
		}
		String receptor= receptors[0];
		
		String[] filterParamss= request.getParameterValues(FILTERPARAMS);
		if (filterParamss==null||filterParamss.length!=1){
			logger.debug("User didn't supply any filterParams");
			throw new ServletException("You must supply one set of filterParams");
		}
		String filterParams= filterParamss[0];
				
		
		//test that VinaParams is well formed
		VinaParams vinaParamsObj = new VinaParams();
		try{
			JAXBContext context = JAXBContext.newInstance(VinaParams.class);
			Unmarshaller um = context.createUnmarshaller();
			//we save this one so we don't have to re-unmarshal it later
			vinaParamsObj= (VinaParams)um.unmarshal(new StringReader(vinaParams));
		}
		catch (JAXBException jbe) {
			logger.warn("user supplied badly formed vinaParams");
			throw new ServletException("Please make sure the vinaParams is well formatted");
		}
		
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
			throw new ServletException("Please make sure the filterParams is well formatted");
		}
		
		try {
			Integer jobId= WorkUnitGenerator.BuildJob(receptor, ownerId, vinaParamsObj, filterParamsObj);
			response.addIntHeader(JOBID, jobId);
			//TODO return number of units created to 
		} catch (AmazonServiceException ase) {
			logger.error(ase);
			throw new ServletException("Error in AWS please try again later.");
		} catch (AmazonClientException ace) {
			logger.error(ace);
			throw new ServletException("Internal error please try again");
		} catch (SQLException sqle) {
			logger.error(sqle);
			throw new ServletException("Error connecting to SQL please try again later");
		} catch (JAXBException jaxbe) {
			logger.error(jaxbe);
			throw new ServletException("Internal JAXB error");
		}

		
		
	}  

}
