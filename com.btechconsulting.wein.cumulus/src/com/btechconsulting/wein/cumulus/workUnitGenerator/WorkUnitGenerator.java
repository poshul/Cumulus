/**
 * 
 */
package com.btechconsulting.wein.cumulus.workUnitGenerator;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.btechconsulting.wein.cumulus.initialization.Constants;
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
	
	public void BuildJob(String receptor, String ownerID, VinaParams vinaParams, FilterParams filterParams) throws SQLException, AmazonServiceException, JAXBException, AmazonClientException, FileNotFoundException, IOException{
		//find the new JobID
		Integer jobID= Initializer.INSTANCE.getMaxJobID(ownerID);
		//put an empty job on the server
		Initializer.INSTANCE.putJobOnServer(ownerID, jobID,new HashMap<Integer,wUStatus>());
		DetermineWorkToDo jobWork= new DetermineWorkToDo(receptor, ownerID, filterParams);
		String receptorID=jobWork.PutReceptorInDatabase();
		List<String> compoundIDs=jobWork.FilterCompoundsInDatabase();
		Integer workUnitId=0;
		for(String i:compoundIDs){
			Initializer.INSTANCE.putWorkUnit(ownerID, jobID, PutWorkUnitInSQS(BuildWorkUnit(receptorID, i, ownerID, jobID, workUnitId, vinaParams)), wUStatus.INFLIGHT); 
			workUnitId++;
		}
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
	
	public static Integer PutWorkUnitInSQS(WorkUnit workunit) throws InternalError, AmazonServiceException, JAXBException, AmazonClientException, FileNotFoundException, IOException{
		//do the marshalling
        java.io.StringWriter sw = new StringWriter();
		JAXBContext context = JAXBContext.newInstance(WorkUnit.class);
		Marshaller m = context.createMarshaller();
		m.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
		m.marshal(workunit, sw);
		String marshalledUnit= sw.toString();
		//put on the queue
		AmazonSQS sqsClient = new AmazonSQSClient(new PropertiesCredentials(
				new FileInputStream(Constants.credentialsFile)));
		sqsClient.sendMessage(new SendMessageRequest(Initializer.INSTANCE.getDispatchQueue(),marshalledUnit));
		
		return workunit.getWorkUnitID();
	}
	
	
}
