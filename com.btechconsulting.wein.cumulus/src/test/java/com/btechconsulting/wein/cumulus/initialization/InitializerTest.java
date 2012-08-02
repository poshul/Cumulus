/**
 * 
 */
package com.btechconsulting.wein.cumulus.initialization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.btechconsulting.wein.cumulus.initialization.Initializer.wUStatus;

/**
 * @author samuel
 *
 */
public class InitializerTest {

	/**
	 * Test method for {@link com.btechconsulting.wein.cumulus.initialization.Initializer#putJobOnServer(java.lang.String, Integer, Map)}.
	 */
/*	@Test
	public synchronized void testPutJobOnServer() {
		Map<Integer, wUStatus> testJob=  new HashMap<Integer, wUStatus>();
		testJob.put(1, wUStatus.INFLIGHT);
		Initializer.getInstance(null).putJobOnServer("0", 0,testJob);
		assert(Initializer.getInstance(null).unitsOnServer.get("0").get(0).get(1)==wUStatus.INFLIGHT);
	}*/

	/**
	 * Test method for {@link com.btechconsulting.wein.cumulus.initialization.Initializer#removeJobFromServer(java.lang.String, java.lang.String)}.
	 */
/*	@Test
	public synchronized void testRemoveJobFromServer() {
		Map<Integer, wUStatus> testJob=  new HashMap<Integer, wUStatus>();
		testJob.put(1, wUStatus.INFLIGHT);
		Map<Integer,Map<Integer,wUStatus>> testUID= new HashMap<Integer, Map<Integer,wUStatus>>();
		testUID.put(1,testJob);
		Initializer.getInstance(null).unitsOnServer.put("1", testUID );
		Initializer.getInstance(null).removeJobFromServer("1", 1);
		assertNull(Initializer.getInstance(null).unitsOnServer.get("1").get(1));	
	}*/

	/**
	 * Test method for {@link com.btechconsulting.wein.cumulus.initialization.Initializer#putWorkUnit(java.lang.String, Integer, Integer, com.btechconsulting.wein.cumulus.initialization.Initializer.wUStatus)}.
	 */
/*	@Test
	public synchronized void testPutWorkUnit() {
		Map<Integer, wUStatus> testJob=  new HashMap<Integer, wUStatus>();
		Map<Integer,Map<Integer,wUStatus>> testUID= new HashMap<Integer, Map<Integer,wUStatus>>();
		testUID.put(2,testJob);
		Initializer.getInstance(null).unitsOnServer.put("2", testUID );
		Initializer.getInstance(null).putWorkUnit("2", 2, 2, wUStatus.INFLIGHT);
		//tests creating a new workunit
		assertEquals(Initializer.getInstance(null).unitsOnServer.get("2").get(2).get(2), wUStatus.INFLIGHT);
		Initializer.getInstance(null).putWorkUnit("2", 2, 2, wUStatus.ERROR);
		//tests modifiying an existing work unit
		assertEquals(Initializer.getInstance(null).unitsOnServer.get("2").get(2).get(2), wUStatus.ERROR);
	}*/

	/**
	 * Test method for {@link com.btechconsulting.wein.cumulus.initialization.Initializer#getStatusOfWorkUnit(java.lang.String, java.lang.String, java.lang.String)}.
	 */
/*	@Test
	public synchronized void testGetStatusOfWorkUnit() {
		Map<Integer, wUStatus> testJob=  new HashMap<Integer, wUStatus>();
		testJob.put(3, wUStatus.INFLIGHT);
		Map<Integer,Map<Integer,wUStatus>> testUID= new HashMap<Integer, Map<Integer,wUStatus>>();
		testUID.put(3,testJob);
		Initializer.getInstance(null).unitsOnServer.put("3", testUID );
		assertEquals(Initializer.getInstance(null).getStatusOfWorkUnit("3", 3, 3), wUStatus.INFLIGHT);
	}*/
	
/*	@Test
	public synchronized void testGetMaxJobID(){
		//test that we behave when the user doesn't yet exist
		assertEquals(Initializer.getInstance(null).getMaxJobID("this"),(Integer) 0);
		//test that we behave when the user exists but doesn't have any jobs
		Initializer.getInstance(null).unitsOnServer.put("4", new HashMap<Integer, Map<Integer,wUStatus>>());
		assertEquals(Initializer.getInstance(null).getMaxJobID("4"),(Integer) 0);
		//test that we behave when there is a user and a job
		Map<Integer, wUStatus> testJob=  new HashMap<Integer, wUStatus>();
		testJob.put(5, wUStatus.INFLIGHT);
		Map<Integer,Map<Integer,wUStatus>> testUID= new HashMap<Integer, Map<Integer,wUStatus>>();
		testUID.put(5,testJob);
		Initializer.getInstance(null).unitsOnServer.put("5", testUID );
		assertEquals(Initializer.getInstance(null).getMaxJobID("5"),(Integer) 5);
		//test that we behave when there are multiple jobs for a user
		testUID.put(6,testJob);
		Initializer.getInstance(null).unitsOnServer.put("5", testUID );
		assertEquals(Initializer.getInstance(null).getMaxJobID("5"),(Integer) 6);
	}*/
/*	
	@Test(expected=IllegalStateException.class)
	public void testGetNumberOfWorkUnitsInFlight(){
		//test proper handling before we put anything on the server
		assertEquals((Integer) 0, Initializer.getInstance(null).getNumberOfWorkUnitsInFlight("a", 0));
	}
	
	@Test
	public void testGetNumberOfWorkUnitsInFlight2(){
		Map<Integer, wUStatus> testJob=  new HashMap<Integer, wUStatus>();
		Map<Integer,Map<Integer,wUStatus>> testUID= new HashMap<Integer, Map<Integer,wUStatus>>();
		testUID.put(2,testJob);
		Initializer.getInstance(null).unitsOnServer.put("10", testUID );
		Initializer.getInstance(null).putWorkUnit("10", 2, 2, wUStatus.INFLIGHT);
		//test whether we count right
		assertEquals((Integer) 1, Initializer.getInstance(null).getNumberOfWorkUnitsInFlight("10", 2));
	}*/

	/**
	 * Test method for {@link com.btechconsulting.wein.cumulus.initialization.Initializer#getDispatchQueue()}.
	 */
	@Test
	public void testGetDispatchQueue() {
		assertNotNull(Initializer.getInstance(null).getDispatchQueue());
	}

	/**
	 * Test method for {@link com.btechconsulting.wein.cumulus.initialization.Initializer#getReturnQueue()}.
	 */
	@Test
	public void testGetReturnQueue() {
		assertNotNull(Initializer.getInstance(null).getDispatchQueue());
	}

	/**
	 * Test method for {@link com.btechconsulting.wein.cumulus.initialization.Initializer#getSqsClient()}.
	 */
	@Test
	public void testGetSqsClient() {
		assertNotNull(Initializer.getInstance(null).getDispatchQueue());
	}

}
