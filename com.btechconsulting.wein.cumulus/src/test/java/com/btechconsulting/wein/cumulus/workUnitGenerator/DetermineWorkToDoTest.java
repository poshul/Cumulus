package com.btechconsulting.wein.cumulus.workUnitGenerator;

import static org.junit.Assert.assertFalse;

import java.sql.SQLException;
import java.util.List;

import org.junit.After;
import org.junit.Test;

import com.btechconsulting.wein.cumulus.model.FilterParams;

public class DetermineWorkToDoTest {

	@After
	public void tearDown() throws Exception {
		//Initializer.INSTANCE.teardownAll();
	}
	
	@Test
	public void testWithMinSuppliers(){
		try{
			FilterParams filter= new FilterParams();
			filter.setMaxNrb(0);
			filter.setMinSuppliers("1");
			DetermineWorkToDo newwork = new DetermineWorkToDo("blah", "0",filter);
			newwork.PutReceptorInDatabase();
			List<String> ids = newwork.FilterCompoundsInDatabase();
			System.out.println(ids.size());
			assertFalse(ids.size()==1564040);//this is a dirty way of telling that we have actually filtered
		}
		catch(SQLException e){
			e.printStackTrace();
		}
	}

	@Test
	public void test() {
		try{
			FilterParams filter= new FilterParams();
			filter.setMaxNrb(0);
			DetermineWorkToDo newwork = new DetermineWorkToDo("blah", "0",filter);
			newwork.PutReceptorInDatabase();
			List<String> ids = newwork.FilterCompoundsInDatabase();
			System.out.println(ids.size());
			assertFalse(ids.size()==1564040);//this is a dirty way of telling that we have actually filtered
		}
		catch(SQLException e){
			e.printStackTrace();
		}
	}

}
