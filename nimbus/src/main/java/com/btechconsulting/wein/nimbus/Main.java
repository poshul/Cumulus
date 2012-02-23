/**
 * 
 */
package com.btechconsulting.wein.nimbus;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.tomcat.dbcp.pool.impl.GenericKeyedObjectPool.Config;

import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.btechconsulting.wein.nimbus.model.ReturnUnit;
import com.btechconsulting.wein.nimbus.model.WorkUnit;


/**
 * @author Samuel Wein
 *
 */
public class Main {

	/**
	 * This is the class that runs on startup of the Nimbus
	 * @param args
	 */
	public static void main(String[] args) {
		//TODO write shutdown hook here.

		//Get the queue names
		PropertiesCredentials credentials = null;
		try {
			credentials=new PropertiesCredentials(
					Main.class.getResourceAsStream(Constants.CREDENTIALSFILE));
			//new FileInputStream(Constants.CREDENTIALSFILE));
		} catch (FileNotFoundException e1) {
			System.err.println("Error reading credentials file");
			e1.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			System.err.println("Error reading credentials file");
			e.printStackTrace();
			System.exit(1);
		} if (credentials==null){
			System.err.println("Error reading credentials file, not caught.");
			//we should never reach here if we do it indicates a flow problem
			System.exit(2);
		}

		//get Properties file
		Boolean mvn= false; //this tracks if we are being run from the maven build, and whether to take properties from constants for nimbus.properties
		java.util.Properties props= new java.util.Properties();
		try {
			props.load( Main.class.getResourceAsStream(Constants.NIMBUSPROPSFILE));
			mvn=true;
		} catch (IOException e4) {
			//this means we couldn't load the properties file, so we default to the settings in constants.
			e4.printStackTrace();
			mvn=false;
		}
		
		String dispatchQueue=null;
		String returnQueue=null;
		if ((mvn==true && Boolean.getBoolean(props.getProperty("onCloud")))|| (mvn==false && Constants.ONCLOUD)){ //if we are in maven and on the cloud or if we aren't on maven and are on the cloud
			Map<String,String> queues= new HashMap<String, String>(); 

			dispatchQueue= queues.get("dispatch");
			returnQueue= queues.get("return");
			if (dispatchQueue==null|| returnQueue==null){
				System.err.println("Couldn't get queue names");
				System.exit(1);
			}
			try {
				queues = GetQueueName.GetQueues(GetQueueName.GetInstanceID(), null);
			} catch (IOException e) {
				System.err.println("Error reading credentials file");
				e.printStackTrace();
				System.exit(1);
			} catch (IllegalStateException e) {
				System.err.println("Internal Error");
				e.printStackTrace();
				System.exit(1);
			}
		} else{ //If we are not running from an ec2 instance take static queue names
			dispatchQueue= "https://queue.amazonaws.com/157399895577/dispatchQueue";
			returnQueue= "https://queue.amazonaws.com/157399895577/returnQueue";
		}
		//Get SQS client.
		AmazonSQSClient client = new AmazonSQSClient(credentials);

		//Get SQL connection
		Connection conn=null;
		Statement stmt=null;
		try {
			conn = PooledConnectionFactory.INSTANCE.getCumulusConnection();
			stmt = conn.createStatement();
		} catch (SQLException e2) {
			System.err.println("Couldn't connect to SQL server");
			e2.printStackTrace();
			System.exit(1);
		}


		//Start loop here
		while (true){
			//Check dispatch queue for workunit
			try {
				Thread.sleep(1000); // This guarantees that we will not query the SQS more than once a second
			} catch (InterruptedException e1) {
				e1.printStackTrace();
				System.err.println("Waiting between requests to sqs was interrupted");
				System.exit(2);
			} 
			List<Message> messageList = GetMessageBundle(dispatchQueue, client);
			if (messageList.size()==0){//if the queue didn't have any messages
				try {
					Thread.sleep(Long.valueOf(props.getProperty("waitTime")));
				} catch (InterruptedException e) {
					System.err.println("Waiting interrupted");
					e.printStackTrace();
					System.exit(2);
				}
				messageList = GetMessageBundle(dispatchQueue, client);
				if (messageList.size()==0){//if the queue still didn't have any messages we die of boredom
					System.err.println("We ran out of work");
					System.exit(0);
				}
			}if (messageList.size()>1){// if we somehow get too many messages
				System.err.println("we got too many messages");
				System.exit(2);
			}
			//Retrieve workunit
			System.out.println("Got workunit");
			String receiptHandle= messageList.get(0).getReceiptHandle();
			String marshalledWorkUnit= messageList.get(0).getBody();
			//unmarshall workunit
			WorkUnit unMarshalledUnit= null;
			try{
				unMarshalledUnit= UnmarshallWorkUnit(marshalledWorkUnit);
			}

			catch (JAXBException e) {
				System.err.println("Couldn't unmarshall WorkUnit");
				// TODO: handle exception send error to queue
			}

			if (unMarshalledUnit==null){
				//TODO: handle malformed work unit
				System.err.println("Unmarshalled work unit is null");
			}

			//generate the skeleton of the returnUnit
			ReturnUnit returnU= new ReturnUnit();
			returnU.setJobID(unMarshalledUnit.getJobID());
			returnU.setOwnerID(unMarshalledUnit.getOwnerID());
			returnU.setWorkUnitID(unMarshalledUnit.getWorkUnitID());
			returnU.setStatus("ERROR");//I'd much rather have a return unit incorrectly marked as an error than have a null status


			//Check to make sure that we haven't already done this calculation;
			String queryStatement="SELECT count(*) FROM cumulus.results WHERE owner_id='"+unMarshalledUnit.getOwnerID()+"' and job_id='"+unMarshalledUnit.getJobID()+"' and workunit_id='"+unMarshalledUnit.getWorkUnitID()+"';";
			Integer numResults=null;

			ResultSet duplicateResults;
			try {
				duplicateResults = stmt.executeQuery(queryStatement);
				while (duplicateResults.next())
				{
					numResults=duplicateResults.getInt(1);
				}
			} catch (SQLException e3) {
				System.err.println("couldn't check on duplicate status");
				e3.printStackTrace();
				//if we can't get duplicate status die
				System.exit(1);
			}
			if (numResults==0){
				//get receptor and molecule from sql
				String receptorQuery= "SELECT pdbqtfile FROM cumulus.receptor WHERE sha256='"+unMarshalledUnit.getPointerToReceptor()+"' AND( owner_id='"+unMarshalledUnit.getOwnerID()+"' "+"OR owner_id='0');";
				String moleculeQuery= "SELECT pdbqt FROM cumulus.mol_properties WHERE compound_id='"+unMarshalledUnit.getPointerToMolecule()+"' AND( owner_id='"+unMarshalledUnit.getOwnerID()+"' "+"OR owner_id='0');";
				//System.out.println(moleculeQuery);
				String receptorString=null;
				String moleculeString=null;
				try {
					ResultSet receptorResults= stmt.executeQuery(receptorQuery);
					Integer numRResults=0;
					while (receptorResults.next())
					{
						receptorString=receptorResults.getString(1);
						numRResults++;
					}
					ResultSet moleculeResults= stmt.executeQuery(moleculeQuery);
					Integer numMResults=0;
					while (moleculeResults.next())//TODO deal with blank molecule results
					{
						moleculeString=moleculeResults.getString(1);
						numMResults++;
					}

					if (numMResults<1||numRResults<1){
						System.err.println("Couldn't find either molecule or receptor");
						//TODO send error here
					}

					if (numRResults>1){
						System.err.println("We had a collision in receptor results");
						//TODO send error here
					}

					if (numMResults>1){
						System.err.println("We had a collision in molecule results");
						//TODO send error here
					}

				} catch (SQLException e2) {
					System.err.println("Couldn't retrieve data from SQL");
					e2.printStackTrace();
					System.exit(1);
				}

				//store receptor and molecule on disk TODO
				String receptorFileName="/tmp/receptor.pdbqt";
				String moleculeFileName="/tmp/molecule.pdbqt";
				File receptorFile= new File(receptorFileName);
				File moleculeFile= new File(moleculeFileName);
				try {
					FileOutputStream receptorOutputStream= new FileOutputStream(receptorFile);
					FileOutputStream moleculeOutputStream= new FileOutputStream(moleculeFile);
					PrintWriter rOut= new PrintWriter(receptorOutputStream);
					PrintWriter mOut= new PrintWriter(moleculeOutputStream);
					rOut.print(receptorString);
					mOut.print(moleculeString);
					mOut.close();
					rOut.close();
				} catch (FileNotFoundException e2) {
					System.err.println("Error creating file");
					e2.printStackTrace();
					System.exit(1);
				}


				//call vina
				Callable<String> callable = new VinaCaller(moleculeFileName, receptorFileName, unMarshalledUnit.getVinaParams());
				ExecutorService executor = new ScheduledThreadPoolExecutor(1);
				Future<String> returnString = executor.submit(callable);
				Long now =System.currentTimeMillis();
				Boolean retried= false;
				//we loop while waiting, up to a limit of 15 minutes
				while (!returnString.isDone()&& System.currentTimeMillis()-now<=900000) {
					try {
						Thread.sleep(1000); //check whether we are finished each second.
					} catch (InterruptedException e) {
						e.printStackTrace();
						System.err.println("We were interrupted");
						System.exit(2);
					}
				}
				//check to see whether we finished.
				if (!returnString.isDone()){ //TODO refactor this into a loop, it is currently sloppy
					//if we haven't finished.
					if(retried==false){ //If this is our first try, retry
						now =System.currentTimeMillis();
						//we loop while waiting, up to a limit of 15 minutes
						while (!returnString.isDone()&& System.currentTimeMillis()-now<=900000) {
							try {
								Thread.sleep(1000); //check whether we are finished each second.
							} catch (InterruptedException e) {
								e.printStackTrace();
								System.err.println("We were interrupted");
								System.exit(2);
							}
						}
					}
					//wait for time to expire or vina to return
					if (!returnString.isDone()){
						executor.shutdownNow(); // kill ALL the things!
						returnU.setStatus("ERROR");
						try {
							SendStatusToReturnQueue(client, returnQueue, returnU);
						} catch (JAXBException e) {
							e.printStackTrace();
							System.err.println("Problem marshalling return unit");
							System.exit(1);
						}
						DeleteMessageFromDispatchQueue(client, dispatchQueue, receiptHandle);
					} 
				}else {

					try {
						System.out.println(returnString.get());
					} catch (InterruptedException e) {
						e.printStackTrace();
						System.err.println("Execution was interrupted.  This is bad");
						System.exit(2);
					} catch (ExecutionException e) {
						e.printStackTrace();
						System.err.println("Vina exited abnormally");
						//try to tell the return queue that we have an error
						returnU.setStatus("ERROR");
						try {
							SendStatusToReturnQueue(client, returnQueue, returnU);
							DeleteMessageFromDispatchQueue(client, dispatchQueue, receiptHandle);
						} catch (JAXBException e1) {
							e1.printStackTrace();
							System.err.println("Multiple Internal errors");
							System.exit(1);
						}
						System.exit(1);
					}


					//load results from disk
					String results= null;
					String resultsFileName= moleculeFileName+".out";//location of the outfile is hardcodes in VinaCaller, this is bad TODO fix it
					File resultsFile = new File(resultsFileName);

					try {
						FileReader resultsReader= new FileReader(resultsFile);
						BufferedReader in = new BufferedReader(resultsReader);
						String thisline= in.readLine();
						while(thisline!=null){ //read until we hit the end of the file
							results=results+thisline;
							thisline=in.readLine();
						}
					} catch (FileNotFoundException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (IOException ioe) {
						// TODO Auto-generated catch block
						ioe.printStackTrace();
					}
					if (results==null){
						results=unMarshalledUnit.getPointerToMolecule()+": No confirmations were found with submitted parameters";
					}

					//put results into sql
					String resultsStatement="INSERT INTO cumulus.results (owner_id, job_id, workunit_id, results) VALUE('"+unMarshalledUnit.getOwnerID()+"','"+unMarshalledUnit.getJobID()+"','"+unMarshalledUnit.getWorkUnitID()+"','"+results+"');";
					System.out.println(resultsStatement);
					try {

						stmt.executeUpdate(resultsStatement);

					} catch (SQLException e1) {
						System.err.println("Couldn't put results into SQL");//TODO deal with duplicate key problems
						e1.printStackTrace();
						System.exit(1);
					}

					//put results in return queue
					returnU.setStatus("DONE");
					try {
						SendStatusToReturnQueue(client, returnQueue, returnU);
					} catch (JAXBException jbe) {
						jbe.printStackTrace();
						System.err.println("Problem marshalling return unit");
						System.exit(1);
					}
					//delete workunit from dispatch queue
					DeleteMessageFromDispatchQueue(client, dispatchQueue, receiptHandle);
				}
			}else{ //this can happen with SQS so we need to accept duplicate processing, in this case we trust the earlier computation
				System.err.println("Result is already in database");
				returnU.setStatus("DONE");
				try {
					SendStatusToReturnQueue(client, returnQueue, returnU);
				} catch (JAXBException jbe) {
					jbe.printStackTrace();
					System.err.println("Problem marshalling return unit");
					System.exit(1);
				}
				//delete workunit from dispatch queue
				DeleteMessageFromDispatchQueue(client, dispatchQueue, receiptHandle);
			}
		}
		//end loop

	}


	private static List<Message> GetMessageBundle(String dispatchQueue, AmazonSQSClient client){
		ReceiveMessageRequest request = new ReceiveMessageRequest(dispatchQueue).withMaxNumberOfMessages(1).withVisibilityTimeout(930);
		ReceiveMessageResult result= new ReceiveMessageResult();
		Integer retries=0;
		while (retries<=2){
			try{
				result= client.receiveMessage(request);
				break;
			}
			catch (Exception e){ //this lets us recover from transient "queue does not exist" faults
				System.err.println("Caught amazon exception");
				System.err.println(e);
				retries++;
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					//we should never reach here
					e1.printStackTrace();
					System.exit(2);
				}
				continue;
			}
		}
		if (retries>2){
			System.err.println("Couldn't connect to AWS");
			System.exit(1);
		}
		List<Message> messageList =result.getMessages();
		return messageList;
	}

	private static WorkUnit UnmarshallWorkUnit(String workUnit) throws JAXBException{
		JAXBContext context = JAXBContext.newInstance(WorkUnit.class);
		Unmarshaller um = context.createUnmarshaller();
		WorkUnit returnUnit =(WorkUnit) um.unmarshal(new StringReader(workUnit));
		return returnUnit;
	}

	private static String MarshallReturnUnit(ReturnUnit returnUnit) throws JAXBException{
		JAXBContext context = JAXBContext.newInstance(ReturnUnit.class);
		Marshaller m = context.createMarshaller();
		StringWriter writer = new StringWriter();
		m.marshal(returnUnit, writer);
		return writer.toString();
	}

	private static void SendStatusToReturnQueue(AmazonSQSClient client,String returnQueue, ReturnUnit returnUnit) throws JAXBException{
		String marshalledReturnUnit = MarshallReturnUnit(returnUnit);
		SendMessageRequest request = new SendMessageRequest(returnQueue, marshalledReturnUnit);
		client.sendMessage(request);
	}

	private static void DeleteMessageFromDispatchQueue(AmazonSQSClient client, String dispatchQueue, String receiptHandle){
		DeleteMessageRequest request = new DeleteMessageRequest(dispatchQueue, receiptHandle);
		client.deleteMessage(request);
	}

}
