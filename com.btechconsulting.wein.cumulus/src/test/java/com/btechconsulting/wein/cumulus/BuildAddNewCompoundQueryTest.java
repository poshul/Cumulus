/**
 * 
 */
package com.btechconsulting.wein.cumulus;

import static org.junit.Assert.*;

import org.junit.Test;

import com.btechconsulting.wein.cumulus.model.NewCompound;

/**
 * @author samuel
 *
 */
public class BuildAddNewCompoundQueryTest {

	/**
	 * Test method for {@link com.btechconsulting.wein.cumulus.BuildAddNewCompoundQuery#BuildQuery(com.btechconsulting.wein.cumulus.model.NewCompound)}.
	 */
	@Test
	public void testBuildQuery() {
		NewCompound testNewCompound =new NewCompound();
		testNewCompound.setOwnerID("-1");
		testNewCompound.setCompoundID("foo");
		testNewCompound.setCompound("bar");
		testNewCompound.setMwt((float) 200);
		testNewCompound.setLogp((float) 1);
		testNewCompound.setDesolvApolar((float) 3);
		testNewCompound.setDesolvPolar((float) 2);
		testNewCompound.setHbd(1);
		testNewCompound.setHba(2);
		testNewCompound.setTpsa(3);
		testNewCompound.setCharge(4);
		testNewCompound.setNrb(5);
		testNewCompound.setSmiles("xyzzy");
		String results= BuildAddNewCompoundQuery.BuildQuery(testNewCompound);
		System.out.println(results);
		assert (results.equals("INSERT INTO cumulus.mol_properties(compound_id, owner_id, pdbqt, mwt, logp, desolv_apolar, desolv_polar, hbd, hba, tpsa, charge, nrb, smiles) VALUES(\"foo\",\"-1\",\"bar\",\"200.0\",\"1.0\",\"3.0\",\"2.0\",\"1\",\"2\",\"3\",\"4\",\"5\",\"xyzzy\");"));
	}
	
	/**
	 * Test method for {@link com.btechconsulting.wein.cumulus.BuildAddNewCompoundQuery#BuildQuery(com.btechconsulting.wein.cumulus.model.NewCompound)}.
	 */
	@Test
	public void testBuildQuerySparse() {
		NewCompound testNewCompound =new NewCompound();
		testNewCompound.setOwnerID("-1");
		testNewCompound.setCompoundID("foo");
		testNewCompound.setCompound("bar");
		testNewCompound.setLogp((float) 1);
		testNewCompound.setDesolvApolar((float) 3);
		testNewCompound.setDesolvPolar((float) 2);
		testNewCompound.setHbd(1);
		testNewCompound.setTpsa(3);
		testNewCompound.setCharge(4);
		testNewCompound.setNrb(5);
		testNewCompound.setSmiles("xyzzy");
		String results= BuildAddNewCompoundQuery.BuildQuery(testNewCompound);
		System.out.println(results);
		assert (results.equals("INSERT INTO cumulus.mol_properties(compound_id, owner_id, pdbqt, logp, desolv_apolar, desolv_polar, hbd, tpsa, charge, nrb, smiles) VALUES(\"foo\",\"-1\",\"bar\",\"1.0\",\"3.0\",\"2.0\",\"1\",\"3\",\"4\",\"5\",\"xyzzy\");"));
	}
	
	

}
