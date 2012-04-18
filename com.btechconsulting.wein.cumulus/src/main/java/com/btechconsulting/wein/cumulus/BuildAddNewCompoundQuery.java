package com.btechconsulting.wein.cumulus;

import com.btechconsulting.wein.cumulus.model.NewCompound;

public class BuildAddNewCompoundQuery {
	public static String BuildQuery(NewCompound compound) {
		
		//The simplest way to do this is to split the query into 2 strings, and cat them at the end
		//String molQuery="INSERT INTO cumulus.mol_properties(compound_id, owner_id, pdbqt) VALUES (\""+compoundId+"\",\""+ownerId+"\",\""+compound+"\");";
		String columns="INSERT INTO cumulus.mol_properties(compound_id, owner_id, pdbqt";
		String values="VALUES(\""+compound.getCompoundID()+"\",\""+compound.getOwnerID()+"\",\""+compound.getCompound()+"\"";
		
		//TODO this is sloppy
		if (compound.getMwt()!=null){
			columns=columns.concat(", mwt");
			values=values.concat(",\""+compound.getMwt()+"\"");
		}
		if (compound.getLogp()!=null){
			columns=columns.concat(", logp");
			values=values.concat(",\""+compound.getLogp()+"\"");
		}
		if (compound.getDesolvApolar()!=null){
			columns=columns.concat(", desolv_apolar");
			values=values.concat(",\""+compound.getDesolvApolar()+"\"");
		}
		if (compound.getDesolvPolar()!=null){
			columns=columns.concat(", desolv_polar");
			values=values.concat(",\""+compound.getDesolvPolar()+"\"");
		}
		if (compound.getHbd()!=null){
			columns=columns.concat(", hbd");
			values=values.concat(",\""+compound.getHbd()+"\"");
		}
		if (compound.getHba()!=null){
			columns=columns.concat(", hba");
			values=values.concat(",\""+compound.getHba()+"\"");
		}
		if (compound.getTpsa()!=null){
			columns=columns.concat(", tpsa");
			values=values.concat(",\""+compound.getTpsa()+"\"");
		}
		if (compound.getCharge()!=null){
			columns=columns.concat(", charge");
			values=values.concat(",\""+compound.getCharge()+"\"");
		}
		if (compound.getNrb()!=null){
			columns=columns.concat(", nrb");
			values=values.concat(",\""+compound.getNrb()+"\"");
		}
		if (compound.getSmiles()!=null){
			columns=columns.concat(", smiles");
			values=values.concat(",\""+compound.getSmiles()+"\"");
		}
		columns=columns.concat(") ");
		values=values.concat(");");
		
		return columns+values;
	}

}
