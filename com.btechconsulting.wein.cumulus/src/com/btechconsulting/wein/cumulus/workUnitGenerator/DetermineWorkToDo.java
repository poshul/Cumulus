package com.btechconsulting.wein.cumulus.workUnitGenerator;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.btechconsulting.wein.cumulus.initialization.PooledConnectionFactory;
import com.btechconsulting.wein.cumulus.model.FilterParams;

public class DetermineWorkToDo {
	private Connection conn = null;
	private String receptor="";
	private String ownerId="";
	private FilterParams filterParams= new FilterParams();
	private final Logger logger= Logger.getLogger(DetermineWorkToDo.class);


	public DetermineWorkToDo(String receptor, String ownerId, FilterParams filterParams) throws SQLException {
		this.conn = PooledConnectionFactory.INSTANCE.getCumulusConnection();
		this.receptor=receptor;
		this.ownerId=ownerId;
		this.filterParams=filterParams;
	}

	/**
	 * Checks if the receptor/owner is already in the database, if not it puts the compound in the database	
	 * @return the sha-256 hash of the receptor.
	 * @throws SQLException
	 */
	public String PutReceptorInDatabase() throws SQLException{
		//take MD5 of receptor
		MessageDigest md;
		String query= "";
		String sha256="";
		try {
			md = MessageDigest.getInstance("SHA-256");
			byte[] bytes = md.digest(this.receptor.getBytes());
			sha256=new String(bytes);
			//query="SELECT count(*) FROM cumulus.mol_properties;";
			query="SELECT count(*) FROM cumulus.receptor WHERE sha256=\'"+sha256+"\' and owner_id=\'"+this.ownerId+"\';";
		} catch (NoSuchAlgorithmException e) {
			logger.error("Couldn't find SHA256");
			// Unless SHA256 goes somewhere any time soon, this block should not be reached
			e.printStackTrace();
			System.err.println("SHA256 has disappeared.");
		}
		Statement stmt = this.conn.createStatement();
		ResultSet results= stmt.executeQuery(query);
		if (results.next()){
			//if the receptor is not in the database (with the right ownership)
			if (results.getInt(1)==0)
			{
				logger.debug("Receptor/owner pair is not in the database.");
				query="INSERT INTO cumulus.receptor (owner_id, pdbqtfile, sha256) value(\""+this.ownerId+"\",\""+this.receptor+"\",\""+sha256+"\");";
				stmt.executeUpdate(query);
			}
			else{
				logger.debug("The receptor is already in the database");
				System.out.println("receptor is already in the database");
			}
		}
		return sha256;
	}
	
	/**
	 * Determine which compounds in the database match the properties in filterParams.
	 * @return a list of compounds_id's which match the requested parameters.
	 * @throws SQLException
	 */
	public List<String> FilterCompoundsInDatabase() throws SQLException{
		List<String> resultstring= new ArrayList<String>();
		String query="";
		// we use the non merging statement if we don't need to specify suppliers 
		if (this.filterParams.getSupplier()!=null && this.filterParams.getMinSuppliers()!=null){
			query="SELECT distinct(mol_properties.compound_id) FROM cumulus.mol_properties, cumulus.supplier_properties WHERE mol_properties.owner_id="+this.ownerId+" OR mol_properties.owner_id=0 AND supplier_properties.owner_id="+this.ownerId+" OR supplier_properties.owner_id=0 AND mol_properties.compound_id=supplier_properties.compound_id ";
		}
		else{
			query="SELECT distinct(mol_properties.compound_id) FROM cumulus.mol_properties WHERE mol_properties.owner_id="+this.ownerId+" OR mol_properties.owner_id=0";
		}
		// TODO This block is sloppy.
		if (this.filterParams.getMinMwt()!=null){
			query=query.concat(" and mol_properties.mwt>="+this.filterParams.getMinMwt());
		}
		if (this.filterParams.getMaxMwt()!=null){
			query=query.concat(" and mol_properties.mwt<="+this.filterParams.getMaxMwt());
		}
		if (this.filterParams.getMinLogp()!=null){
			query=query.concat(" and mol_properties.logp>="+this.filterParams.getMinLogp());
		}
		if (this.filterParams.getMaxLogp()!=null){
			query=query.concat(" and mol_properties.logp<="+this.filterParams.getMaxLogp());
		}
		if (this.filterParams.getMinDesolvApolar()!=null){
			query=query.concat(" and mol_properties.desolv_apolar>="+this.filterParams.getMinDesolvApolar());
		}
		if (this.filterParams.getMaxDesolvApolar()!=null){
			query=query.concat(" and mol_properties.desolv_apolar<="+this.filterParams.getMaxDesolvApolar());
		}
		if (this.filterParams.getMinDesolvPolar()!=null){
			query=query.concat(" and mol_properties.desolv_polar>="+this.filterParams.getMinDesolvPolar());
		}
		if (this.filterParams.getMaxDesolvPolar()!=null){
			query=query.concat(" and mol_properties.desolv_polar<="+this.filterParams.getMaxDesolvPolar());
		}
		if (this.filterParams.getMinHbd()!=null){
			query=query.concat(" and mol_properties.hbd>="+this.filterParams.getMinHbd());
		}
		if (this.filterParams.getMaxHbd()!=null){
			query=query.concat(" and mol_properties.hbd<="+this.filterParams.getMaxHbd());
		}
		if (this.filterParams.getMinHba()!=null){
			query=query.concat(" and mol_properties.hba>="+this.filterParams.getMinHba());
		}
		if (this.filterParams.getMaxHba()!=null){
			query=query.concat(" and mol_properties.hba<="+this.filterParams.getMaxHba());
		}
		if (this.filterParams.getMinTpsa()!=null){
			query=query.concat(" and mol_properties.tpsa>="+this.filterParams.getMinTpsa());
		}
		if (this.filterParams.getMaxTpsa()!=null){
			query=query.concat(" and mol_properties.tpsa<="+this.filterParams.getMaxTpsa());
		}
		if (this.filterParams.getMinCharge()!=null){
			query=query.concat(" and mol_properties.charge>="+this.filterParams.getMinCharge());
		}
		if (this.filterParams.getMaxCharge()!=null){
			query=query.concat(" and mol_properties.charge<="+this.filterParams.getMaxCharge());
		}
		if (this.filterParams.getMinNrb()!=null){
			query=query.concat(" and mol_properties.nrb>="+this.filterParams.getMinNrb());
		}
		if (this.filterParams.getMaxNrb()!=null){
			query=query.concat(" and mol_properties.nrb<="+this.filterParams.getMaxNrb());
		} else {
			query=query.concat(" and mol_properties.nrb<=10"); // this block prevents massive compounds from being loaded, regardless of what the user says
		}
		if (this.filterParams.getSupplier()!=null){
			query=query.concat(" and supplier_properties.supplier="+this.filterParams.getSupplier());  //TODO doublecheck this
		}
		Statement stmt = this.conn.createStatement();
		ResultSet results= stmt.executeQuery(query);
		while(results.next()){
			resultstring.add(results.getString(1));
		}
		return resultstring;
	}


	/**
	 * Test code
	 * TODO remove
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			DetermineWorkToDo newwork= new DetermineWorkToDo("blah","0",new FilterParams());
			newwork.PutReceptorInDatabase();
			List<String> ids= newwork.FilterCompoundsInDatabase();
			System.out.println(ids);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
