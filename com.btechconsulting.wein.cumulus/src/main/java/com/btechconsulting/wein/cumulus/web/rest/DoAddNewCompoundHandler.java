package com.btechconsulting.wein.cumulus.web.rest;

import java.io.IOException;
import java.io.Writer;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.btechconsulting.wein.cumulus.CreateShortReturn;
import com.btechconsulting.wein.cumulus.initialization.PooledConnectionFactory;

/*
 * @author Samuel Wein
 * A handler to allow the user to add a new compound to the database.  Input is expected to comply with the NewCompound xml spec
 */

public class DoAddNewCompoundHandler implements RestHandler {

	public static final String OWNERID="ownerId";
	public static final String COMPOUNDID="compoundId";
	public static final String COMPOUND="compound";
	public static final String OVERWRITE="overwrite";
	private final Logger logger=Logger.getLogger(DoSearchHandler.class);

	public void executeSearch(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		
		Writer writer=response.getWriter();
		response.setContentType("text/xml");

		String[] ownerIds= request.getParameterValues(OWNERID);
		if (ownerIds==null|| ownerIds.length!=1){
			logger.debug("User didn't supply an ownerId");
			String error= "You must supply an ownerId";
			response.setStatus(400);
			writer.write(CreateShortReturn.createShortResponse(error, true));
			//throw new ServletException();
			return;
		}
		String ownerId=ownerIds[0];

		String[] compoundIds= request.getParameterValues(COMPOUNDID);
		if (compoundIds==null|| compoundIds.length!=1){
			logger.debug("User didn't supply an compoundId");
			String error="You must supply an compoundId";
			response.setStatus(400);
			writer.write(CreateShortReturn.createShortResponse(error, true));
			return;
		}
		String compoundId=compoundIds[0];

		String[] compounds= request.getParameterValues(COMPOUND);
		if (compounds==null|| compounds.length!=1){
			logger.debug("User didn't supply an compound");
			String error="You must supply an compound";
			response.setStatus(400);
			writer.write(CreateShortReturn.createShortResponse(error, true));
			return;
		}
		String compound=compounds[0];

		String[] overwrites= request.getParameterValues(OVERWRITE);
		if (overwrites==null){  //if the user doesn't specify whether to overwrite existing value or not we default to not
			overwrites=new String[1];
			overwrites[0]="false";
		}
		if (overwrites.length!=1){
			logger.debug("User supplied more than one value for overwrites");
		}
		Boolean overwrite=Boolean.valueOf(overwrites[0]);

		//TODO check validity of pdbqt file here.
		String molQuery="INSERT INTO cumulus.mol_properties(compound_id, owner_id, pdbqt) VALUES (\""+compoundId+"\",\""+ownerId+"\",\""+compound+"\");";
		String provQuery="INSERT INTO cumulus.supplier_properties (owner_id, compound_id) VALUES (\""+ownerId+"\",\""+compoundId+"\");";
		Statement stmt= null;
		Connection con;
		//check for an existing entry with the same ownerId and compoundId
		try {
			con = PooledConnectionFactory.INSTANCE.getCumulusConnection();
			try {
				con.setAutoCommit(false);
				stmt= con.createStatement();
				ResultSet results= stmt.executeQuery("SELECT COUNT(*) FROM cumulus.mol_properties WHERE compound_id=\""+compoundId+"\" and owner_id=\""+ownerId+"\";");
				results.next();
				if (results.getInt(1)!=0)
				{
					if (overwrite){//if the request wants to overwrite the existing data we delete the existing entries
						stmt.executeUpdate("DELETE FROM cumulus.mol_properties where compound_id=\""+compoundId+"\" and owner_id=\""+ownerId+"\";");
						stmt.executeUpdate("DELETE FROM cumulus.supplier_properties where compound_id=\""+compoundId+"\" and owner_id=\""+ownerId+"\";");
						con.commit();
					}else{
						String error="A compound with this name and user already exists in the database, please repeat the query with \"overwrite=true\" to overwrite";
						response.setStatus(400);
						writer.write(CreateShortReturn.createShortResponse(error, true));
						
						return;
					}
				}				
				
				stmt.executeUpdate(molQuery);
				stmt.executeUpdate(provQuery);
				con.commit();
				con.setAutoCommit(true);
				con.close();
			} catch (SQLException e) {
				e.printStackTrace(System.err);
				try {
					con.rollback(); //this stupidly complex series of try catch blocks prevents leaving the database in an inconsistant state
					con.setAutoCommit(true);
				} catch (SQLException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace(System.err);
					String error="Couldn't add compound to database.  Please contact an administrator before trying again.";
					response.setStatus(500);
					writer.write(CreateShortReturn.createShortResponse(error, true));
					return;
				}
				String error="Couldn't connect to database.  Please try again later.";
				response.setStatus(500);
				writer.write(CreateShortReturn.createShortResponse(error, true));
				return;

			}
		} catch (SQLException e2) {
			System.err.println("Couldn't get SQL connection");
			e2.printStackTrace(System.err);
			String error="Couldn't connect to database.  Please try again later.";
			response.setStatus(500);
			writer.write(CreateShortReturn.createShortResponse(error, true));
			return;
		}
		response.setStatus(200);
		//Build the response xml
		writer.write(CreateShortReturn.createShortResponse("Successfully added "+compoundId+" to database.", false));
	}

}
