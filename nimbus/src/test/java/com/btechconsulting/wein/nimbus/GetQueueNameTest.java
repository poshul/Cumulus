package com.btechconsulting.wein.nimbus;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("unused")
public class GetQueueNameTest {

	@Test
	public void testGetQueues() throws FileNotFoundException, IOException {
		System.out.println(GetQueueName.GetQueues("i-36316d54", null));//FIXME this is really brittle and should be fixed once we deploy
	}
	
	//FIXME we need to test getting the instance ID, that will be done after we get enough in place to deploy a jar

}
