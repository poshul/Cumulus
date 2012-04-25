package com.btechconsulting.wein.cumulus.initialization;


import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.eclipse.jdt.internal.core.index.impl.Int;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.DeleteQueueRequest;
import com.btechconsulting.wein.cumulus.model.WorkUnit;

/**
 * @author Samuel Wein
 * @date 12/1/2011
 *
 */
public class Initializer {


	public enum wUStatus{
		INFLIGHT,DONE,ERROR
	}
	private static Initializer instance = null;
	private String dispatchQueue;
	private String returnQueue;
	private AmazonSQS sqsClient;
	private Marshaller workUnitMarshaller;
	private PropertiesCredentials credentials;
	private Thread sqsListener;
	private Thread gridManager;
	private Boolean shuttingDown=false;
	// units on server is a map of OwnerID to a map of JobID to a map of WorkUnitID to work Unit status
	// TODO figure out how to get unit testing to work when this is private
	Map<String, Map<Integer, Map<Integer,wUStatus>>> unitsOnServer;


	public static Initializer getInstance(ServletContext servletContext){
		//System.out.println(instance);
		if (instance==null){
			instance = new Initializer(servletContext);
		}
		return instance;
	}

	//this is a compatability method
	public static Initializer getInstance(){
		if (instance==null){
			instance= new Initializer(null);
			System.err.println("warning, using deprecated version of Initializer");
		}
		return instance;
	}



	private Initializer(ServletContext servletContext){
		try{
			//we read credentials once here.  Minimizing reads to the disk
			FileInputStream creds= null;
			System.out.println(servletContext);
			if (servletContext==null){// if we are not being run off a servlet
				creds= new FileInputStream(Constants.credentialsFile);
				credentials = new PropertiesCredentials(creds);
			}else{// if we are running as a servlet get the credentials location from the servletConfig
				try{
					String credsName= (String) servletContext.getAttribute("creds");
					URL url= servletContext.getResource(credsName);//get the creds file location from the servlet
					if (url== null){
						url = getClass().getResource(credsName);
					}

					if (url == null) {
						System.err.println("No configuration found for RestServlet: " + credsName);
						throw new ServletException("No configuration found for RestSearchAction: " + credsName);
					}
					credentials=new PropertiesCredentials(url.openStream());

				}
				catch (IOException ioe) {
					System.err.println(ioe.getMessage());
					throw new ServletException("Error during Cumulus initialization: " + ioe.getMessage(), ioe);
				}
			}
			//credentials=new PropertiesCredentials(creds);
			JAXBContext context = JAXBContext.newInstance(WorkUnit.class);
			workUnitMarshaller = context.createMarshaller();
			workUnitMarshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
			sqsClient = new AmazonSQSClient(this.credentials);
			dispatchQueue = createQueue(sqsClient, Constants.dispatchQueueName);
			returnQueue = createQueue(sqsClient, Constants.returnQueueName);
			unitsOnServer= createUnitsOnServer();
			//createInstances(new AmazonEC2Client(this.credentials), Constants.initialInstances);
		}
		catch (AmazonServiceException ase) {
			System.err.println("Caught an AmazonServiceException, which means your request made it " +
					"to Amazon AWS, but was rejected with an error response for some reason.");
			System.err.println("Error Message:    " + ase.getMessage());
			System.err.println("HTTP Status Code: " + ase.getStatusCode());
			System.err.println("AWS Error Code:   " + ase.getErrorCode());
			System.err.println("Error Type:       " + ase.getErrorType());
			System.err.println("Request ID:       " + ase.getRequestId());
			//System.exit(1);
		}
		catch (AmazonClientException ace) {
			System.err.println("Caught an AmazonClientException, which means the client encountered " +
					"a serious internal problem while trying to communicate with AWS, such as not " +
					"being able to access the network.");
			System.err.println("Error Message: " + ace.getMessage());
			//System.exit(1);

		}
		catch (NullPointerException npee){
			System.err.println("Couldn't find credentials file\n");
			//System.exit(1);

		}
		catch (Exception e){
			System.err.println(e);
			//System.exit(1);

		}

		//start the SQSListener
		sqsListener = new Thread(new SqsListener(this));
		sqsListener.setName("sqsListener");
		sqsListener.start();		
		//start the gridManager
		gridManager = new Thread(new GridManager(this));
		gridManager.setName("gridManager");
		gridManager.start();

	}
	
	/*@deprecated
	 * this is to be replaced with the more versatile createInstances
	 */
	@Deprecated
	private void createInitialInstances() throws Exception {
		AmazonEC2 ec2= new AmazonEC2Client(this.credentials);
		//set the zone
		ec2.setEndpoint(Constants.ec2Region);
		System.out.println("Intializing "+Constants.initialInstances+" instances\n");
		//create EC2 instances
		RunInstancesRequest runInstancesRequest = new RunInstancesRequest()
		.withInstanceType(Constants.instanceType)
		.withImageId(Constants.imageID)
		.withMinCount(Constants.initialInstances)
		.withMaxCount(Constants.initialInstances)
		.withSecurityGroupIds(Constants.securityGroupID)
		.withKeyName(Constants.keyName);

		RunInstancesResult runInstances = ec2.runInstances(runInstancesRequest);
		System.out.println(runInstances.toString());

		//tag the instances with the dispatch and return queues
		List<Instance> instances = runInstances.getReservation().getInstances();
		for (Instance instance : instances) {
			CreateTagsRequest createTagsRequest = new CreateTagsRequest();
			createTagsRequest.withResources(instance.getInstanceId()) //
			.withTags(new Tag("dispatch", this.dispatchQueue))
			.withTags(new Tag("return", this.returnQueue));
			ec2.createTags(createTagsRequest);
		}
	}
	
	/*
	 * @param ec2: an ec2 client
	 * @param instancesIn: the number of instances to create
	 * This creates a specified number of instances of nimbus
	 */
	void createInstances(AmazonEC2Client ec2, Integer instancesIn) throws Exception {

		//set the zone
		ec2.setEndpoint(Constants.ec2Region);
		System.out.println("Intializing "+instancesIn+" instances\n");
		//create EC2 instances
		RunInstancesRequest runInstancesRequest = new RunInstancesRequest()
		.withInstanceType(Constants.instanceType)
		.withImageId(Constants.imageID)
		.withMinCount(1)
		.withMaxCount(instancesIn)
		.withSecurityGroupIds(Constants.securityGroupID)
		.withKeyName(Constants.keyName);

		RunInstancesResult runInstances = ec2.runInstances(runInstancesRequest);
		System.out.println(runInstances.toString());

		//tag the instances with the dispatch and return queues
		List<Instance> instances = runInstances.getReservation().getInstances();
		for (Instance instance : instances) {
			CreateTagsRequest createTagsRequest = new CreateTagsRequest();
			createTagsRequest.withResources(instance.getInstanceId()) //
			.withTags(new Tag("dispatch", this.dispatchQueue))
			.withTags(new Tag("return", this.returnQueue));
			ec2.createTags(createTagsRequest);
		}
	}

	/* creatUnitsOnServer: creates the datastructure for storing workunits
	 */

	private Map<String, Map<Integer, Map<Integer, wUStatus>>> createUnitsOnServer() {
		return Collections.synchronizedMap(new HashMap<String, Map<Integer, Map<Integer,wUStatus>>>());
	}

	/* createQueue: creates the AWS SQS queue for cumulus.  Throws Exception if we can't create the queue
	 * @param credentialsFile the file containing credentials to access the AWS
	 * @return AmazonSQS the queue
	 */
	private String createQueue(AmazonSQS client, String queueName) throws Exception {
		// Create a queue
		System.out.println("Creating a new SQS queue "+queueName+".\n");
		CreateQueueRequest createQueueRequest = new CreateQueueRequest(queueName);
		String myQueueUrl = client.createQueue(createQueueRequest).getQueueUrl();
		System.out.println("Created queue at "+myQueueUrl+"\n");
		return myQueueUrl;
	}

	/**
	 * 
	 */
	public void teardownAll(){
		this.shuttingDown=true;
		this.sqsListener.interrupt();
		this.gridManager.interrupt();
		sqsClient.deleteQueue(new DeleteQueueRequest(dispatchQueue));
		sqsClient.deleteQueue(new DeleteQueueRequest(returnQueue));
		System.out.println("Deleted SQS queues");
		//We create a request to describe current instances with the imageID that Cumulus uses.
		DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest();
		describeInstancesRequest.withFilters(new Filter("image-id").withValues(Constants.imageID));
		System.out.println("Getting list of active cumulus drones");
		// Initialize variables.
		List<String> instanceIds = new ArrayList<String>();
		try{
			AmazonEC2 ec2= new AmazonEC2Client(this.credentials);
			// get all of the instanceID's.  add them to the list
			DescribeInstancesResult describeInstancesResult = ec2.describeInstances(describeInstancesRequest);
			List<Reservation> reservations = describeInstancesResult.getReservations();
			if (reservations!=null&& reservations.size()!=0){
				for (Reservation reservation: reservations){
					List<Instance> instances = reservation.getInstances();
					if (instances!=null){
						for (Instance instance: instances){
							instanceIds.add(instance.getInstanceId());
						}
					}
				}
				// Terminate instances.
				System.out.println("Terminate instances");
				TerminateInstancesRequest terminateRequest = new TerminateInstancesRequest(instanceIds);
				ec2.terminateInstances(terminateRequest);
			}
		} catch (AmazonServiceException e) {
			// Write out any exceptions that may have occurred.
			System.err.println("Error terminating instances");
			System.err.println("Caught Exception: " + e.getMessage());
			System.err.println("Reponse Status Code: " + e.getStatusCode());
			System.err.println("Error Code: " + e.getErrorCode());
			System.err.println("Request ID: " + e.getRequestId());
			//System.exit(1);
		} catch (Exception e) {
			//caught another exception
			System.err.println(e);
			System.err.println("Did not exit cleanly");
			//System.exit(1);
		}

		System.out.println("Killed all cumulus drones");
		//clearing results from database  TODO reevaluate the usefulness of this at release time
		
		String query="DELETE FROM cumulus.results;";
		Statement stmt = null;
		try {
			Connection conn= PooledConnectionFactory.INSTANCE.getCumulusConnection();
			stmt= conn.createStatement();
			stmt.executeUpdate(query);
			System.out.println("Deleted un-returned results");
			conn.close();
		} catch (SQLException e) {
			System.out.println("Couldn't delete un-returned results");
			e.printStackTrace();
		}
		
	}

	/**
	 * putJobOnServer: add a complete job (jobID, Map<String, wUstatus>) to the server
	 * @param userID the user ID of the end user adding the job
	 * @param jobID the ID of the job to add to the server
	 * @param workUnits A map of workunits to add to the server
	 */
	public synchronized void putJobOnServer(
			String userID, Integer jobID, Map<Integer, wUStatus> workUnits ){
		//If this user doesn't have any existing jobs, create the user
		if (this.unitsOnServer.get(userID)==null){
			this.unitsOnServer.put(userID,new HashMap<Integer, Map<Integer, wUStatus>>());
		}
		this.unitsOnServer.get(userID).put(jobID, workUnits);
	}
	/**
	 * removeJobFromServer: remove a complete job from the server
	 * @param userID the user ID of the end user removing the job
	 * @param jobID the ID of the job to be removed
	 */
	public synchronized void removeJobFromServer(
			String userID, Integer jobID){
		this.unitsOnServer.get(userID).remove(jobID);
	}

	/**
	 * putWorkUnit: puts a workunit into an existing job.  NB: This either creates a new work unit, or overwrites an existing one
	 * @param userID the ID of the end user putting in the work unit
	 * @param jobID the ID of the job for which the work unit is being modified
	 * @param workUnitID the ID of the work unit to be modified
	 * @param newStatus the new status of the work unit
	 */

	public synchronized void putWorkUnit(
			String userID, Integer jobID, Integer workUnitID, wUStatus newStatus){
		this.unitsOnServer.get(userID).get(jobID).put(workUnitID, newStatus);
	}

	/**
	 * getStatusOfWorkUnit: returns the status of the selected work unit
	 * @param userID the ID of the end user querying the work unit
	 * @param jobID the ID of the job which is being queried
	 * @param workUnitID the ID of the work unit being queried 
	 * @return wUStatus the status of the work unit
	 */
	public synchronized wUStatus getStatusOfWorkUnit(
			String userID, Integer jobID, Integer workUnitID){
		return this.unitsOnServer.get(userID).get(jobID).get(workUnitID);
	}

	/**
	 * Returns the max numbered jobID for a userID
	 * @param userID
	 * @return max jobID
	 */
	public synchronized Integer getMaxJobID(String userID){
		if(this.unitsOnServer.get(userID)!=null&& this.unitsOnServer.get(userID).keySet().size()>0){
			return Collections.max(this.unitsOnServer.get(userID).keySet());
		}else{
			return 0;
		}
	}

	public synchronized Integer getNumberOfWorkUnitsInFlight(String userID, Integer jobID) throws IllegalStateException{
		try{
			Map<Integer,wUStatus> job= this.unitsOnServer.get(userID).get(jobID);
			Integer numInFlight=0;
			for (Integer i : job.keySet()) {
				if (job.get(i).equals(wUStatus.INFLIGHT)){
					numInFlight++;
				}
			}
			return numInFlight;
		}
		catch(NullPointerException npe){
			throw (new IllegalStateException("specified job and/or user does not exist"));
		}
	}

	/**
	 * @return the dispatchQueue
	 */
	public String getDispatchQueue() {
		return dispatchQueue;
	}

	/**
	 * @return the returnQueue
	 */
	public String getReturnQueue() {
		return returnQueue;
	}

	/**
	 * @return the sqsClient
	 */
	public AmazonSQS getSqsClient() {
		return sqsClient;
	}

	/**
	 * @return the workUnitMarshaller
	 */
	public Marshaller getWorkUnitMarshaller() {
		return workUnitMarshaller;
	}

	/**
	 * @param workUnitMarshaller the workUnitMarshaller to set
	 */
	public void setWorkUnitMarshaller(Marshaller workUnitMarshaller) {
		this.workUnitMarshaller = workUnitMarshaller;
	}

	/**
	 * @return the credentials
	 */
	public PropertiesCredentials getCredentials() {
		return credentials;
	}

	/**
	 * @param credentials the credentials to set
	 */
	public void setCredentials(PropertiesCredentials credentials) {
		this.credentials = credentials;
	}

	/**
	 * @return the shuttingDown
	 */
	public Boolean getShuttingDown() {
		return shuttingDown;
	}



}
