package com.btechconsulting.wein.cumulus;

import java.io.StringWriter;

import javax.servlet.ServletException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import com.btechconsulting.wein.cumulus.model.ShortResponse;

//Builds the xml to return a ShortResponse

public class CreateShortReturn {
	public static String createShortResponse(String response, Boolean isError) throws ServletException{
		try {
			JAXBContext context = JAXBContext.newInstance(ShortResponse.class);
			Marshaller m = context.createMarshaller();
			ShortResponse responseXml= new ShortResponse();
			StringWriter writer = new StringWriter();
			responseXml.setIsError(isError);
			responseXml.setResponse(response);
			m.marshal(responseXml, writer);
			String written=writer.toString();
			return written;
		}
		catch (JAXBException jbe){
			jbe.printStackTrace();
			throw new ServletException("Couldn't marshall result");
		}

	}
}