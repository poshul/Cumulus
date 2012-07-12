package com.btechconsulting.wein.cumulus;

import javax.servlet.ServletException;

import org.junit.Test;

public class CreateShortReturnTest {

	@Test
	public void testCreateShortResponse() throws ServletException {
		String testString =CreateShortReturn.createShortResponse("this",false);
		assert(testString.equals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><shortResponse xmlns=\"http://cumuluschemistry.com/WorkUnit\"><response>this</response><isError>false</isError></shortResponse>"));
	}

}
