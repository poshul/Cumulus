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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.log4j.Logger;
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


	final static Logger logger = Logger.getLogger(Main.class);
	/**
	 * This is the class that runs on startup of the Nimbus
	 * @param args
	 */

	public static void main(String[] args) {
		//the following code block is currently unused, since we are relying on the crontab to deal with machine shutdown
		/*Runtime.getRuntime().addShutdownHook(new Thread() {
			//TODO add call to amazon API to shutdown machine
			public void run() {
				try {
					List<String> command= new ArrayList<String>();
					command.add("rm");
					command.add("/var/lock/nimbus");
					Process process= new ProcessBuilder(command).start();
				} catch (IOException e) {
					// THIS IS VERY BAD
					e.printStackTrace();
				}
				logger.info("in : run () : shutdownHook");
				logger.info("Shutdown hook completed...");
			}
		});*/

		//get Properties file
		Boolean mvn= false; //this tracks if we are being run from the maven build, and whether to take properties from constants for nimbus.properties
		java.util.Properties props= new java.util.Properties();
		try {
			logger.debug("loading properties file");
			props.load( Main.class.getResourceAsStream(Constants.NIMBUSPROPSFILE));
			mvn=true;
		} catch (IOException e4) {
			//this means we couldn't load the properties file, so we default to the settings in constants.
			logger.warn("Couldn't load .properties file");
			e4.printStackTrace();
			mvn=false;
		}
		String VINALOC;
		String CREDENTIALSFILE;
		String INSTANCEIDLOC;
		Integer WAITTIME;
		Boolean ONCLOUD;
		if (mvn==true){
			logger.info("Using properties file");
			VINALOC=props.getProperty("vinaLoc");
			CREDENTIALSFILE=props.getProperty("credentialsFile");
			INSTANCEIDLOC=props.getProperty("instanceIdLoc");
			WAITTIME=Integer.parseInt(props.getProperty("waitTime"));
			ONCLOUD=Boolean.valueOf(props.getProperty("onCloud"));
		}else{
			logger.warn("Using static properties");
			VINALOC=Constants.VINALOC;
			CREDENTIALSFILE=Constants.CREDENTIALSFILE;
			INSTANCEIDLOC=Constants.INSTANCEIDLOC;
			WAITTIME=Constants.WAITTIME;
			ONCLOUD=Constants.ONCLOUD;
		}


		//Get SQL connection
		Connection conn=null;
		Statement stmt=null;
		try {
			conn = PooledConnectionFactory.INSTANCE.getCumulusConnection();
			stmt = conn.createStatement();
		} catch (SQLException e2) {
			logger.error("Couldn't connect to SQL server");
			e2.printStackTrace();
			System.exit(1);
		}

		//Get the queue names
		PropertiesCredentials credentials = null;
		try {
			credentials=new PropertiesCredentials(
					Main.class.getResourceAsStream(CREDENTIALSFILE));
			//new FileInputStream(Constants.CREDENTIALSFILE));
		} catch (FileNotFoundException e1) {
			logger.error("Error reading credentials file");
			e1.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			logger.error("Error reading credentials file");
			e.printStackTrace();
			System.exit(1);
		} if (credentials==null){
			logger.error("Error reading credentials file, not caught.");
			//we should never reach here if we do it indicates a flow problem
			System.exit(2);
		}

		String dispatchQueue=null;
		String returnQueue=null;
		if (ONCLOUD){ //if we are in maven and on the cloud or if we aren't on maven and are on the cloud
			logger.debug("On cloud");
			Map<String,String> queues= new HashMap<String, String>(); 
/*			try {
*/				//queues = GetQueueName.GetQueues(GetQueueName.GetInstanceID(INSTANCEIDLOC), credentials);
				queues = loadQueueName(stmt);
/*			} catch (IOException e) {
				logger.error("Error reading credentials file");
				e.printStackTrace();
				System.exit(1);
			} catch (IllegalStateException e) {
				logger.error("Internal Error");
				e.printStackTrace();
				System.exit(1);
			}*/
			dispatchQueue= queues.get("dispatch");
			returnQueue= queues.get("return");
			if (dispatchQueue==null|| returnQueue==null){
				logger.error("Couldn't get queue names");
				logger.warn("Falling back to hardcoded queuenames");
				dispatchQueue= "https://queue.amazonaws.com/157399895577/dispatchQueue";
				returnQueue= "https://queue.amazonaws.com/157399895577/returnQueue";
			}

		} else{ //If we are not running from an ec2 instance take static queue names
			logger.warn("Off cloud");
			dispatchQueue= "https://queue.amazonaws.com/157399895577/dispatchQueue";
			returnQueue= "https://queue.amazonaws.com/157399895577/returnQueue";
		}
		//Get SQS client.
		AmazonSQSClient client = new AmazonSQSClient(credentials);



		//Start loop here
		while (true){
			while (true){
				//Check dispatch queue for workunit
				try {
					Thread.sleep(1000); // This guarantees that we will not query the SQS more than once a second
				} catch (InterruptedException e1) {
					e1.printStackTrace();
					logger.error("Waiting between requests to sqs was interrupted");
					System.exit(2);
				} 
				List<Message> messageList = GetMessageBundle(dispatchQueue, client);
				Long timer = 0L;
				while (messageList.size()==0){//if the queue didn't have any messages
					if (timer==0L){//if we haven't started the timer
						logger.debug("No work now");
						timer=System.currentTimeMillis();
					}if (System.currentTimeMillis()-timer>WAITTIME){//if we have waited as long as we can
						logger.warn("We ran out of work");
						System.exit(0);
					}
					try {
						Thread.sleep(1000); //wait 1 second
					} catch (InterruptedException e) {
						logger.error("Waiting interrupted");
						e.printStackTrace();
						System.exit(2);
					}
					messageList = GetMessageBundle(dispatchQueue, client);
				}if (messageList.size()>1){// if we somehow get too many messages
					logger.error("we got too many messages");
					System.exit(2);
				}if (messageList.size()==1){ //If we have a message reset the times
					timer=0L;
				}
				//Retrieve workunit
				logger.debug("Got workunit");
				String receiptHandle= messageList.get(0).getReceiptHandle();
				String marshalledWorkUnit= messageList.get(0).getBody();
				//unmarshall workunit
				WorkUnit unMarshalledUnit= null;
				try{
					unMarshalledUnit= UnmarshallWorkUnit(marshalledWorkUnit);
				}

				catch (JAXBException e) {
					logger.error("Couldn't unmarshall WorkUnit");
				}

				if (unMarshalledUnit==null){
					logger.error("Unmarshalled work unit is null");
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
					logger.error("couldn't check on duplicate status");
					e3.printStackTrace();
					//if we can't get duplicate status die
					System.exit(1);
				}
				if (numResults==0){
					//get receptor and molecule from sql
					String receptorQuery= "SELECT pdbqtfile FROM cumulus.receptor WHERE sha256='"+unMarshalledUnit.getPointerToReceptor()+"' AND( owner_id='"+unMarshalledUnit.getOwnerID()+"');";//+"OR owner_id='0');";
					System.out.println(receptorQuery);
					String moleculeQuery= "SELECT pdbqt FROM cumulus.mol_properties WHERE compound_id='"+unMarshalledUnit.getPointerToMolecule()+"' AND( owner_id='"+unMarshalledUnit.getOwnerID()+"' "+"OR owner_id='0');";
					//logger.info(moleculeQuery);
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
						while (moleculeResults.next())
						{
							moleculeString=moleculeResults.getString(1);
							numMResults++;
						}

						if (moleculeString==null||moleculeString.equals("")){
							logger.warn("Molecule pdbqt was null");
							returnU.setStatus("ERROR");
							try {
								SendStatusToReturnQueue(client, returnQueue, returnU);
								DeleteMessageFromDispatchQueue(client, dispatchQueue, receiptHandle);
							} catch (JAXBException e1) {
								e1.printStackTrace();
								logger.error("Multiple Internal errors");
								System.exit(1);
							}
							break; //this breaks from the inner while true loop
						}

						//Decode base64 encoded receptor
						byte[] receptorByteArray = DatatypeConverter.parseBase64Binary(receptorString);
						receptorString=new String(receptorByteArray);

						if (numMResults<1||numRResults<1){
							logger.error("Couldn't find either molecule or receptor");
							returnU.setStatus("ERROR");
							try {
								SendStatusToReturnQueue(client, returnQueue, returnU);
								DeleteMessageFromDispatchQueue(client, dispatchQueue, receiptHandle);
							} catch (JAXBException e1) {
								e1.printStackTrace();
								logger.error("Multiple Internal errors");
								System.exit(1);
							}
							break; //this breaks from the inner while true loop
						}

						if (numRResults>1){
							logger.error("We had a collision in receptor results");
							returnU.setStatus("ERROR");
							try {
								SendStatusToReturnQueue(client, returnQueue, returnU);
								DeleteMessageFromDispatchQueue(client, dispatchQueue, receiptHandle);
							} catch (JAXBException e1) {
								e1.printStackTrace();
								logger.error("Multiple Internal errors");
								System.exit(1);
							}
							break; //this breaks from the inner while true loop
						}

						if (numMResults>1){
							logger.error("We had a collision in molecule results");
							returnU.setStatus("ERROR");
							try {
								SendStatusToReturnQueue(client, returnQueue, returnU);
								DeleteMessageFromDispatchQueue(client, dispatchQueue, receiptHandle);
							} catch (JAXBException e1) {
								e1.printStackTrace();
								logger.error("Multiple Internal errors");
								System.exit(1);
							}
							break;//this breaks from the inner while true loop
						}

					} catch (SQLException e2) {
						logger.error("Couldn't retrieve data from SQL");
						logger.error(e2);
						returnU.setStatus("ERROR");
						try {
							SendStatusToReturnQueue(client, returnQueue, returnU);
							DeleteMessageFromDispatchQueue(client, dispatchQueue, receiptHandle);
						} catch (JAXBException e1) {
							e1.printStackTrace();
							logger.error("Multiple Internal errors");
							System.exit(1);
						}
						break;//this breaks from the inner while true loop
					}

					//store receptor and molecule on disk
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
						logger.error("Error creating file");
						returnU.setStatus("ERROR");
						try {
							SendStatusToReturnQueue(client, returnQueue, returnU);
							DeleteMessageFromDispatchQueue(client, dispatchQueue, receiptHandle);
						} catch (JAXBException e1) {
							e1.printStackTrace();
							logger.error("Multiple Internal errors");
							System.exit(1);
						}
						e2.printStackTrace();
						System.exit(1);
					}


					//call vina
					Callable<String> callable = new VinaCaller(moleculeFileName, receptorFileName, unMarshalledUnit.getVinaParams(), VINALOC);
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
							logger.error("We were interrupted");
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
									logger.error("We were interrupted");
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
								logger.error("Problem marshalling return unit");
								System.exit(1);
							}
							DeleteMessageFromDispatchQueue(client, dispatchQueue, receiptHandle);
						} 
					}else {

						try {
							logger.debug(returnString.get());
						} catch (InterruptedException e) {
							e.printStackTrace();
							logger.error("Execution was interrupted.  This is bad");
							System.exit(2);
						} catch (ExecutionException e) {
							e.printStackTrace();
							logger.error("Vina exited abnormally");
							//try to tell the return queue that we have an error
							returnU.setStatus("ERROR");
							try {
								SendStatusToReturnQueue(client, returnQueue, returnU);
								DeleteMessageFromDispatchQueue(client, dispatchQueue, receiptHandle);
							} catch (JAXBException e1) {
								e1.printStackTrace();
								logger.error("Multiple Internal errors");
								System.exit(1);
							}
							System.exit(1);
						}


						//load results from disk
						String results= new String("REMARK Compound name:"+unMarshalledUnit.getPointerToMolecule()+"\n");
						String resultsFileName= moleculeFileName+".out";//location of the outfile is hardcoded in VinaCaller, this is bad TODO fix it
						File resultsFile = new File(resultsFileName);

						try {
							FileReader resultsReader= new FileReader(resultsFile);
							BufferedReader in = new BufferedReader(resultsReader);
							String thisline= in.readLine();
							while(thisline!=null){ //read until we hit the end of the file
								results=results+"\n"+thisline;
								thisline=in.readLine();
							}
						} catch (FileNotFoundException e1) {
							returnU.setStatus("ERROR");
							try {
								SendStatusToReturnQueue(client, returnQueue, returnU);
								DeleteMessageFromDispatchQueue(client, dispatchQueue, receiptHandle);
							} catch (JAXBException e2) {
								e2.printStackTrace();
								logger.error("Multiple Internal errors");
								System.exit(1);
							}
							e1.printStackTrace();
						} catch (IOException ioe) {
							returnU.setStatus("ERROR");
							try {
								SendStatusToReturnQueue(client, returnQueue, returnU);
								DeleteMessageFromDispatchQueue(client, dispatchQueue, receiptHandle);
							} catch (JAXBException e1) {
								e1.printStackTrace();
								logger.error("Multiple Internal errors");
								System.exit(1);
							}
							ioe.printStackTrace();
						}
						if (results=="REMARK Compound name:"+unMarshalledUnit.getPointerToMolecule()+"\n"){
							results=unMarshalledUnit.getPointerToMolecule()+": No confirmations were found with submitted parameters";
						}

						//Convert the results into base64
						results=DatatypeConverter.printBase64Binary(results.getBytes());

						//put results into sql
						String resultsStatement="INSERT INTO cumulus.results (owner_id, job_id, workunit_id, results) VALUE('"+unMarshalledUnit.getOwnerID()+"','"+unMarshalledUnit.getJobID()+"','"+unMarshalledUnit.getWorkUnitID()+"','"+results+"');";
						logger.debug(resultsStatement);
						try {
							stmt.executeUpdate(resultsStatement);

						} catch (SQLException e1) {
							logger.warn("Couldn't put results into SQL");//TODO deal with duplicate key problems
							try {
								Thread.sleep(10000);
								stmt.executeUpdate(resultsStatement);

							} catch (SQLException | InterruptedException e3) { //retry once in case the SQL error was intermittant
								if (e3.equals(SQLException.class)){
									logger.error("Couldn't put results into SQL on second try");//TODO deal with duplicate key problems
								}else{
									logger.error("Interrupted");//TODO deal with duplicate key problems
								}
								e1.printStackTrace();
								returnU.setStatus("ERROR");
								try {
									SendStatusToReturnQueue(client, returnQueue, returnU);
									DeleteMessageFromDispatchQueue(client, dispatchQueue, receiptHandle);
								} catch (JAXBException e2) {
									e2.printStackTrace();
									logger.error("Multiple Internal errors");
									System.exit(1);
								}
								System.exit(1);
							}
						}

						//put results in return queue
						returnU.setStatus("DONE");
						try {
							SendStatusToReturnQueue(client, returnQueue, returnU);
						} catch (JAXBException jbe) {
							jbe.printStackTrace();
							logger.error("Problem marshalling return unit");
							System.exit(1);
						}
						//delete workunit from dispatch queue
						DeleteMessageFromDispatchQueue(client, dispatchQueue, receiptHandle);
					}
				}else{ //this can happen with SQS so we need to accept duplicate processing, in this case we trust the earlier computation
					logger.warn("Result is already in database");
					returnU.setStatus("DONE");
					try {
						SendStatusToReturnQueue(client, returnQueue, returnU);
					} catch (JAXBException jbe) {
						jbe.printStackTrace();
						logger.error("Problem marshalling return unit");
						System.exit(1);
					}
					//delete workunit from dispatch queue
					DeleteMessageFromDispatchQueue(client, dispatchQueue, receiptHandle);
				}
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
				logger.error("Caught amazon exception");
				logger.error(e);
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
			logger.error("Couldn't connect to AWS");
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

	/**
	 * This function loads the queue names from the metadata table on the database
	 * @param stmt a statement opened to the database
	 * @return the dispatch and return queues
	 */
	private static Map<String,String> loadQueueName(Statement stmt){
		Map<String, String> returnMap= new HashMap<String, String>();
		String dispatchQuery= "SELECT mvalue FROM cumulus.metadata WHERE mkey='dispatch';";
		String returnQuery="SELECT mvalue FROM cumulus.metadata WHERE mkey='return';";
		try{
			ResultSet results= stmt.executeQuery(dispatchQuery);
			returnMap.put("dispatch", results.getString(1)); //Put the result for the queue name in the map
			results = stmt.executeQuery(returnQuery);
			returnMap.put("return", results.getString(1)); //Put the result for the queue name in the map
		}
		catch (SQLException e) {
			logger.error("Couldn't connect to database");
		}
		return returnMap;
	}

}
