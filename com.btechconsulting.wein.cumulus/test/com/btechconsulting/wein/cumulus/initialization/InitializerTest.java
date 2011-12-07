/**
 * 
 */
package com.btechconsulting.wein.cumulus.initialization;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.btechconsulting.wein.cumulus.initialization.Initializer.wUStatus;

/**
 * @author samuel
 *
 */
public class InitializerTest {

/*	*//**
	 * Test method for {@link com.btechconsulting.wein.cumulus.initialization.Initializer#teardownAll()}.
	 *//*
	@Test
	public void testTeardownAll() {
		fail("Not yet implemented");
	}*/

	/**
	 * Test method for {@link com.btechconsulting.wein.cumulus.initialization.Initializer#putJobOnServer(java.lang.String, java.lang.String, java.util.Map)}.
	 */
	@Test
	public void testPutJobOnServer() {
		Map<String, wUStatus> testWorkUnit=  new HashMap<String, wUStatus>();
		testWorkUnit.put("first", wUStatus.INFLIGHT);
		Initializer.INSTANCE.putJobOnServer("0", "0",testWorkUnit);
		assert(Initializer.INSTANCE.unitsOnServer.get("0").get("0").get("first")==wUStatus.INFLIGHT);
	}

	/**
	 * Test method for {@link com.btechconsulting.wein.cumulus.initialization.Initializer#removeJobFromServer(java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testRemoveJobFromServer() {
		Map<String, wUStatus> testJob=  new HashMap<String, wUStatus>();
		testJob.put("first", wUStatus.INFLIGHT);
		Map<String,Map<String,wUStatus>> testUID= new HashMap<String, Map<String,wUStatus>>();
		testUID.put("1",testJob);
		Initializer.INSTANCE.unitsOnServer.put("1", testUID );
		Initializer.INSTANCE.removeJobFromServer("1", "1");
		assertNull(Initializer.INSTANCE.unitsOnServer.get("1").get("1"));	
	}

	/**
	 * Test method for {@link com.btechconsulting.wein.cumulus.initialization.Initializer#putWorkUnit(java.lang.String, java.lang.String, java.lang.String, com.btechconsulting.wein.cumulus.initialization.Initializer.wUStatus)}.
	 */
	@Test
	public void testPutWorkUnit() {
		Map<String, wUStatus> testJob=  new HashMap<String, wUStatus>();
		Map<String,Map<String,wUStatus>> testUID= new HashMap<String, Map<String,wUStatus>>();
		testUID.put("2",testJob);
		Initializer.INSTANCE.unitsOnServer.put("2", testUID );
		Initializer.INSTANCE.putWorkUnit("2", "2", "two", wUStatus.INFLIGHT);
		//tests creating a new workunit
		assertEquals(Initializer.INSTANCE.unitsOnServer.get("2").get("2").get("two"), wUStatus.INFLIGHT);
		Initializer.INSTANCE.putWorkUnit("2", "2", "two", wUStatus.ERROR);
		//tests modifiying an existing work unit
		assertEquals(Initializer.INSTANCE.unitsOnServer.get("2").get("2").get("two"), wUStatus.ERROR);
	}

	/**
	 * Test method for {@link com.btechconsulting.wein.cumulus.initialization.Initializer#getStatusOfWorkUnit(java.lang.String, java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testGetStatusOfWorkUnit() {
		Map<String, wUStatus> testJob=  new HashMap<String, wUStatus>();
		testJob.put("third", wUStatus.INFLIGHT);
		Map<String,Map<String,wUStatus>> testUID= new HashMap<String, Map<String,wUStatus>>();
		testUID.put("3",testJob);
		Initializer.INSTANCE.unitsOnServer.put("3", testUID );
		assertEquals(Initializer.INSTANCE.getStatusOfWorkUnit("3", "3", "third"), wUStatus.INFLIGHT);
	}

	/**
	 * Test method for {@link com.btechconsulting.wein.cumulus.initialization.Initializer#getDispatchQueue()}.
	 */
	@Test
	public void testGetDispatchQueue() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.btechconsulting.wein.cumulus.initialization.Initializer#getReturnQueue()}.
	 */
	@Test
	public void testGetReturnQueue() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.btechconsulting.wein.cumulus.initialization.Initializer#getSqsClient()}.
	 */
	@Test
	public void testGetSqsClient() {
		fail("Not yet implemented");
	}

}
