package com.btechconsulting.wein.cumulus.workUnitGenerator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;

import javax.xml.bind.JAXBException;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.btechconsulting.wein.cumulus.initialization.Initializer;
import com.btechconsulting.wein.cumulus.model.VinaParams;
import com.btechconsulting.wein.cumulus.model.WorkUnit;
import com.btechconsulting.wein.cumulus.model.FilterParams;

public class NimbusTestFramework {

	/**
	 * @param args
	 * This class sets up the queues and populates the dispatch queue with a workunit to allow
	 * testing of Nimbus
	 * @throws IOException 
	 * @throws JAXBException 
	 * @throws FileNotFoundException 
	 * @throws AmazonClientException 
	 * @throws InternalError 
	 * @throws AmazonServiceException 
	 * @throws SQLException 
	 */
	public static void main(String[] args) throws AmazonServiceException, InternalError, AmazonClientException, FileNotFoundException, JAXBException, IOException, SQLException {
		System.out.println(Initializer.getInstance().getDispatchQueue());
		System.out.println(Initializer.getInstance().getReturnQueue());
		FilterParams filter=new FilterParams();
		DetermineWorkToDo worktodo= new DetermineWorkToDo(getReceptor(), "0", filter);
		String pointerToReceptor = worktodo.PutReceptorInDatabase();
		WorkUnit myWorkunit= WorkUnitGenerator.BuildWorkUnit(pointerToReceptor, "ZINC00003575", "0", 0, 0, BuildVinaParams());
		WorkUnitGenerator.PutWorkUnitOnServer(myWorkunit);
		System.out.println("ready for nimbus");
		//read in the receptor

		
		
	}

	private static String getReceptor(){
		String receptor="";
		String receptorFileName="resources/1HWKnohet.pdbqt";
		File receptorFile = new File(receptorFileName);
		try {
			FileReader receptorReader= new FileReader(receptorFile);
			BufferedReader in = new BufferedReader(receptorReader);
			String thisline= in.readLine();
			while(thisline!=null){ //read until we hit the end of the file
				receptor=receptor+thisline;
				thisline=in.readLine();
			}
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return receptor;
	}
	
	private static VinaParams BuildVinaParams(){
		VinaParams params = new VinaParams();
		params.setCenterX(0);
		params.setCenterY(3);
		params.setCenterZ(-1);
		params.setSizeX(3);
		params.setSizeY(3);
		params.setSizeZ(2);
		params.setExhaustiveness(9);
		params.setNumModes(7);
		params.setSizeX(3);
		return params;
	}
}
