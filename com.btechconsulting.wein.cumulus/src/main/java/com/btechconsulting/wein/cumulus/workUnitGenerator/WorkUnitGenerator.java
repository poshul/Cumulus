/**
 * 
 */
package com.btechconsulting.wein.cumulus.workUnitGenerator;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.log4j.Logger;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.sqs.model.SendMessageBatchRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import com.btechconsulting.wein.cumulus.initialization.Initializer;
import com.btechconsulting.wein.cumulus.initialization.Initializer.wUStatus;
import com.btechconsulting.wein.cumulus.model.FilterParams;
import com.btechconsulting.wein.cumulus.model.VinaParams;
import com.btechconsulting.wein.cumulus.model.WorkUnit;

/**
 * @author samuel
 *
 */
public class WorkUnitGenerator {

	private final Logger logger= Logger.getLogger(WorkUnitGenerator.class);


	/**
	 * This function builds a job based from a receptor, ownerID, set of vinaParams, and set of filterParams.
	 * The job is submitted to the server and the jobID is returned
	 * @param receptor
	 * @param ownerID
	 * @param vinaParams
	 * @param filterParams
	 * @return jobID
	 * @throws SQLException
	 * @throws AmazonServiceException
	 * @throws JAXBException
	 * @throws AmazonClientException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static Integer BuildJob(String receptor, String ownerID, VinaParams vinaParams, FilterParams filterParams, String dispatchQueueLoc) throws SQLException, AmazonServiceException, JAXBException, AmazonClientException, FileNotFoundException, IOException{
		//find the new JobID
		Integer jobID= Initializer.getInstance(null).getMaxJobID(ownerID)+1;//our jobID is the NEXT integer
		//put an empty job on the server
		//Initializer.getInstance(null).putJobOnServer(ownerID, jobID, new HashMap<Integer,wUStatus>());//TODO REMOVE ME
		DetermineWorkToDo jobWork= new DetermineWorkToDo(receptor, ownerID, filterParams);
		String receptorID=jobWork.PutReceptorInDatabase();
		List<String> compoundIDs=jobWork.FilterCompoundsInDatabase();
		Integer workUnitId=0;
		List<SendMessageBatchRequestEntry> batch=new ArrayList<SendMessageBatchRequestEntry>();
		Integer iter=0;
		//List<Future<SendMessageBatchResult>> futures= new ArrayList<Future<SendMessageBatchResult>>();
		for(String i:compoundIDs){
			SendMessageBatchRequestEntry entry=putWorkUnitInSQSBatch(BuildWorkUnit(receptorID, i, ownerID, jobID, workUnitId, vinaParams));
			batch.add(entry);
			Initializer.getInstance().putWorkUnit(ownerID, jobID, workUnitId, wUStatus.INFLIGHT);
			workUnitId++;
			iter++;
			if (iter>=10){
				SendMessageBatchRequest request= new SendMessageBatchRequest(dispatchQueueLoc, batch);
				//futures.add(Initializer.getInstance().getSqsClient().sendMessageBatchAsync(request));
				Initializer.getInstance().getSqsClient().sendMessageBatch(request);
				//System.out.println("batch sent");
				iter=0;
				batch.removeAll(batch);
			}

		}
		//this catches the last <10
		if (batch.size()>0){
			SendMessageBatchRequest request= new SendMessageBatchRequest(dispatchQueueLoc, batch);
			//futures.add(Initializer.getInstance().getSqsClient().sendMessageBatchAsync(request));
			Initializer.getInstance().getSqsClient().sendMessageBatch(request);
			System.out.println("batch sent");
		}
		/*		while(futures.isEmpty()==false){
			List<Future<SendMessageBatchResult>> toRemove= new ArrayList<Future<SendMessageBatchResult>>();
			for (Future<SendMessageBatchResult>i:futures){
				if(i.isDone()==true){
					toRemove.add(i);
				}
			futures.removeAll(toRemove);
			System.out.println("Futures left:"+futures.size());
			}
		}*/
		//logger.debug("created "+workUnitId+" workunits");
		return jobID;
	}
	
	/**
	 *  This is the old method for building a job, it assumes the default queue location.
	 * @param receptor
	 * @param ownerID
	 * @param vinaParams
	 * @param filterParams
	 * @return
	 * @throws SQLException
	 * @throws AmazonServiceException
	 * @throws JAXBException
	 * @throws AmazonClientException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static Integer BuildJob(String receptor, String ownerID, VinaParams vinaParams, FilterParams filterParams) throws SQLException, AmazonServiceException, JAXBException, AmazonClientException, FileNotFoundException, IOException{
		return BuildJob(receptor, ownerID, vinaParams, filterParams, Initializer.getInstance(null).getDispatchQueue());
	}


	/**
	 * 
	 * @param receptor a string representing the receptor_id in the database
	 * @param molecule a string representing the molecule_id in the database
	 * @param ownerId a string representing the owner of the receptor, the molecule, and the workUnit
	 * @param jobId the ID of the job that this workUnit is part of
	 * @param workUnitId the ID of this unit
	 * @param vinaParams a set of params passed to Autodock Vina 
	 * @return the constructed workUnit object
	 */
	public static WorkUnit BuildWorkUnit(String receptor, String molecule, String ownerId, Integer jobId, Integer workUnitId, VinaParams vinaParams){
		WorkUnit localUnit = new WorkUnit();
		localUnit.setPointerToReceptor(receptor);
		localUnit.setPointerToMolecule(molecule);
		localUnit.setOwnerID(ownerId);
		localUnit.setJobID(jobId);
		localUnit.setWorkUnitID(workUnitId);
		localUnit.setVinaParams(vinaParams);
		return localUnit;
	}

	public static SendMessageBatchRequestEntry putWorkUnitInSQSBatch(WorkUnit workunit) throws InternalError, AmazonServiceException, JAXBException, AmazonClientException, FileNotFoundException, IOException{
		//do the marshalling
		java.io.StringWriter sw = new StringWriter();
		Marshaller m = Initializer.getInstance(null).getWorkUnitMarshaller();
		m.marshal(workunit, sw);
		String marshalledUnit= sw.toString();
		SendMessageBatchRequestEntry entry = new SendMessageBatchRequestEntry(workunit.getWorkUnitID().toString(),marshalledUnit);
		//put in the entry
		/*AmazonSQSAsync sqsClient = Initializer.getInstance().getSqsClient();
		sqsClient.sendMessageAsync(new SendMessageRequest(Initializer.getInstance().getDispatchQueue(),marshalledUnit));*/
		return entry;
	}

	//this is largely for testing, it allows the sending of an individual workUnit to the server
	public static void PutWorkUnitOnServer(WorkUnit workUnit, String dispatchQueueLoc) throws AmazonServiceException, InternalError, AmazonClientException, FileNotFoundException, JAXBException, IOException{
		List<SendMessageBatchRequestEntry> batch=new ArrayList<SendMessageBatchRequestEntry>();
		SendMessageBatchRequestEntry entry= putWorkUnitInSQSBatch(workUnit);
		batch.add(entry);
		SendMessageBatchRequest batchRequest= new SendMessageBatchRequest(dispatchQueueLoc, batch);
		Initializer.getInstance().getSqsClient().sendMessageBatch(batchRequest);
	}


}
