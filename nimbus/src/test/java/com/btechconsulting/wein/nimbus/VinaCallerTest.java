package com.btechconsulting.wein.nimbus;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import javax.xml.bind.JAXBException;

import org.junit.Before;
import org.junit.Test;

import com.btechconsulting.wein.nimbus.model.VinaParams;

public class VinaCallerTest {

	@Test
	public void testVinaCaller() throws JAXBException {
		VinaParams vinaParams= new VinaParams();
		vinaParams.setCenterX(1);
		vinaParams.setCenterY(2);
		vinaParams.setCenterZ(3);
		vinaParams.setSizeX(5);
		vinaParams.setSizeY(6);
		vinaParams.setSizeZ(7);
		vinaParams.setExhaustiveness(3);
		VinaCaller caller = new VinaCaller("/home/samuel/foo", "/home/samuel/bar", vinaParams);
		List<String> decodedParams;
		decodedParams = caller.DecodeVinaParams();
		List<String> expectedParams= new ArrayList<>();
		expectedParams.add("--center_x");
		expectedParams.add("1");
		expectedParams.add("--center_y");
		expectedParams.add("2");
		expectedParams.add("--center_z");
		expectedParams.add("3");
		expectedParams.add("--size_x");
		expectedParams.add("5");
		expectedParams.add("--size_y");
		expectedParams.add("6");
		expectedParams.add("--size_z");
		expectedParams.add("7");
		expectedParams.add("--exhaustiveness");
		expectedParams.add("3");
		assert(decodedParams.equals(expectedParams));
	}

	@Test
	public void testCall() throws Exception {
		VinaParams vinaParams= new VinaParams();
		vinaParams.setCenterX((float)-2.2);
		vinaParams.setCenterY(-9);
		vinaParams.setCenterZ((float)-9.8);
		vinaParams.setSizeX(22);
		vinaParams.setSizeY(22);
		vinaParams.setSizeZ(22);
		vinaParams.setExhaustiveness(15);
		VinaCaller caller = new VinaCaller("src/main/resources/APC.pdbqt", "src/main/resources/1HWKnohet.pdbqt", vinaParams);
		String returnString=caller.call();
		System.out.println(returnString);
	}
	
	@Test
	public void testFuture() throws Exception {
		VinaParams vinaParams= new VinaParams();
		vinaParams.setCenterX((float)-2.2);
		vinaParams.setCenterY(-9);
		vinaParams.setCenterZ((float)-9.8);
		vinaParams.setSizeX(22);
		vinaParams.setSizeY(22);
		vinaParams.setSizeZ(22);
		vinaParams.setExhaustiveness(15);
		Callable<String> callable = new VinaCaller("src/main/resources/APC.pdbqt", "src/main/resources/1HWKnohet.pdbqt", vinaParams);
		ExecutorService executor = new ScheduledThreadPoolExecutor(1);
		Future<String> returnString = executor.submit(callable);
		while (!returnString.isDone()) {
		Thread.sleep(1000);
		}
		System.out.println(returnString.get());
	}

}
