/**
 * 
 */
package com.btechconsulting.wein.cumulus.web.rest;

import java.io.IOException;
import java.io.StringReader;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import com.btechconsulting.wein.cumulus.model.FilterParams;
import com.btechconsulting.wein.cumulus.model.VinaParams;

/**
 * @author samuel
 *
 */
public class DoSearchHandler implements RestHandler {
	public static final String OWNERID="ownerId";
	public static final String VINAPARAMS="vinaParams";
	public static final String RECEPTOR="receptor";
	public static final String FILTERPARAMS="filterParams";
	


	public void executeSearch(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		//parse input to make sure it is compliant with spec.
		String[] ownerIds= request.getParameterValues(OWNERID);
		if (ownerIds==null|| ownerIds.length!=1){
			throw new ServletException("You must supply an ownerID");
		}
		String ownerId=ownerIds[0];
		
		String[] vinaParamss= request.getParameterValues(VINAPARAMS);
		if (vinaParamss==null||vinaParamss.length!=1){
			throw new ServletException("You must supply one set of vinaparams");
		}
		String vinaParams=vinaParamss[0];
		
		String[] receptors= request.getParameterValues(RECEPTOR);
		if (receptors==null||receptors.length!=1){
			throw new ServletException("You must supply one receptor");
		}
		String receptor= receptors[0];
		
		String[] filterParamss= request.getParameterValues(FILTERPARAMS);
		if (filterParamss==null||filterParamss.length!=1){
			throw new ServletException("You must supply one set of filterParams");
		}
		String filterParams= filterParamss[0];
				
		
		//test that VinaParams is well formed
		
		try{
			JAXBContext context = JAXBContext.newInstance(VinaParams.class);
			Unmarshaller um = context.createUnmarshaller();
			um.unmarshal(new StringReader(vinaParams));
		}
		catch (JAXBException jbe) {
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
			throw new ServletException("Please make sure the filterParams is well formatted");
		}

		
		
	}  

}
