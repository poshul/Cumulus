package com.btechconsulting.wein.cumulus.model;

import java.io.FileReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

public class NewMolculeObjectsTest {

	/**
	 * @param args
	 * TODO turn this into actual unit tests
	 */
	private static final String TEST_XML= "./text.xml";
	
	public static void main(String[] args) {
		NewCompound testNewCompound =new NewCompound();
		testNewCompound.setOwnerID("-1");
		testNewCompound.setCompoundID("foo");
		testNewCompound.setCompound("bar");
		testNewCompound.setMwt((float) 200);
		testNewCompound.setLogp((float) 1);
		testNewCompound.setDesolvApolar((float) 3);
		testNewCompound.setDesolvPolar((float) 2);
		testNewCompound.setHbd(1);
		testNewCompound.setHba(2);
		testNewCompound.setTpsa(3);
		testNewCompound.setCharge(4);
		testNewCompound.setNrb(5);
		testNewCompound.setSmiles("xyzzy");
		
		
		// Create jaxb context and instantiate marshaller
		try {
			JAXBContext context = JAXBContext.newInstance(NewCompound.class);
			Marshaller m = context.createMarshaller();
			FilterParams filters=new FilterParams();
			System.out.println("Testing Marshaller\n");
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			m.marshal(testNewCompound, System.out);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
