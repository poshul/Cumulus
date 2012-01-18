/**
 * 
 */
package com.btechconsulting.wein.nimbus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.xml.bind.JAXBException;

import com.btechconsulting.wein.nimbus.model.VinaParams;
import com.btechconsulting.wein.nimbus.model.WorkUnit;

/**
 * @author samuel
 *
 */
public class VinaCaller implements Callable<String> {
	private String moleculeFile;
	private String receptorFile;
	private String ownerID;
	private Integer jobID;
	private Integer workUnitID;
	private VinaParams vinaParams;

	public VinaCaller(String moleculeFile, String receptorFile, String ownerID,
			Integer jobID, Integer workUnitID, VinaParams vinaParams) {
		this.moleculeFile = moleculeFile;
		this.receptorFile = receptorFile;
		this.ownerID = ownerID;
		this.jobID = jobID;
		this.workUnitID = workUnitID;
		this.vinaParams = vinaParams;
	}
	
	/**
	 * Unpacks the VinaParams object into a string of command line arguments.
	 * @return a string containing the command line form of the arguments to vina
	 * @throws JAXBException
	 */
	
	List<String> DecodeVinaParams() throws JAXBException{ //TODO write tests for this.
		List<String> decoded=new ArrayList<String>();
		decoded.add("--center_x");
		decoded.add(Float.toString(this.vinaParams.getCenterX()));
		decoded.add("--center_y");
		decoded.add(Float.toString(this.vinaParams.getCenterY()));
		decoded.add("--center_z");
		decoded.add(Float.toString(this.vinaParams.getCenterZ()));
		decoded.add("--size_x");
		decoded.add(Integer.toString(this.vinaParams.getSizeX()));
		decoded.add("--size_y");
		decoded.add(Integer.toString(this.vinaParams.getSizeY()));
		decoded.add("--size_z");
		decoded.add(Integer.toString(this.vinaParams.getSizeZ()));
		if (this.vinaParams.getExhaustiveness()!=null){
			decoded.add("--exhaustiveness");
			decoded.add(Integer.toString(this.vinaParams.getExhaustiveness()));
		}
		if (this.vinaParams.getSeed()!=null){
			decoded.add("--seed");
			decoded.add(Integer.toString(this.vinaParams.getSeed()));
		}
		if (this.vinaParams.getNumModes()!=null){
			decoded.add("--num_modes");
			decoded.add(Integer.toString(this.vinaParams.getNumModes()));
		}
		if (this.vinaParams.getEnergyRange()!=null){
			decoded.add("--energy_range");
			decoded.add(Integer.toString(this.vinaParams.getEnergyRange()));
		}
		return decoded;
	}
	
	
	/* (non-Javadoc)
	 * @see java.util.concurrent.Callable#call()
	 */
	@Override
	public String call() throws Exception {
		List<String> command= new ArrayList<String>();
		String outLocation=this.moleculeFile+".out";
		command.add(Constants.vinaLoc);
		command.add("--receptor");
		command.add(this.receptorFile);
		command.add("--ligand");
		command.add(this.moleculeFile);
		command.add("--out");
		command.add(outLocation);
		command.addAll(this.DecodeVinaParams());
		System.out.println(command);
		//run vina
		Process process = new ProcessBuilder(command).start();
		//wait for the process to finish
		process.waitFor();
		return outLocation;
	}

}
