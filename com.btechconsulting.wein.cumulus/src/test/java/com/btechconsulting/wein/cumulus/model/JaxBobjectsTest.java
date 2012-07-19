package com.btechconsulting.wein.cumulus.model;

import java.io.FileReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.junit.Test;

public class JaxBobjectsTest {

	/**
	 * @param args
	 * TODO turn this into actual unit tests
	 */
	private static final String TEST_XML= "./text.xml";
	
	@Test
	public void JaxBobjectsTest1() {
		VinaParams params = new VinaParams();
		params.setCenterX(0);
		params.setCenterY(3);
		params.setCenterZ(-1);
		params.setExhaustiveness(9);
		params.setNumModes(7);
		params.setSizeX(3);
		//Build test workunit
		WorkUnit unit = new WorkUnit();
		unit.setJobID(1);
		unit.setVinaParams(params);
		unit.setPointerToMolecule("ZINC68740768");
		unit.setPointerToReceptor("2");
		unit.setOwnerID("0");
		unit.setWorkUnitID(010);
		
		// Create jaxb context and instantiate marshaller
		try {
			JAXBContext context = JAXBContext.newInstance(VinaParams.class);
			JAXBContext context2 = JAXBContext.newInstance(FilterParams.class);
			Marshaller m2= context2.createMarshaller();
			Marshaller m = context.createMarshaller();
			FilterParams filters=new FilterParams();
			System.out.println("Testing Marshaller\n");
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			m2.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			m.marshal(params, System.out);
			m2.marshal(filters, System.out);
			System.out.println("Testing UnMarshaller\n");
			Unmarshaller um = context.createUnmarshaller();
			VinaParams inParams = (VinaParams) um.unmarshal(new FileReader(TEST_XML));
			System.out.println(inParams.getNumModes());
			System.out.println("testing marshalling of WorkUnit");
			context= JAXBContext.newInstance(WorkUnit.class);
			m = context.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			m.marshal(unit, System.out);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
