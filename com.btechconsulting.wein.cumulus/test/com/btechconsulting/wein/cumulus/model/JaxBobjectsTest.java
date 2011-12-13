package com.btechconsulting.wein.cumulus.model;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

public class JaxBobjectsTest {

	/**
	 * @param args
	 */
	private static final String TEST_XML= "./text.xml";
	
	public static void main(String[] args) {
		VinaParams params = new VinaParams();
		params.setCenterX(0);
		params.setCenterY(3);
		params.setCenterZ(-1);
		params.setExhaustiveness(9);
		params.setNumModes(7);
		
		// Create jaxb context and instantiate marshaller
		try {
			JAXBContext context = JAXBContext.newInstance(VinaParams.class);
			Marshaller m = context.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			m.marshal(params, System.out);
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
