package com.btechconsulting.wein.cumulus.initialization;

import org.junit.Test;

import com.amazonaws.services.ec2.AmazonEC2Client;

public class SpotTest {

	@Test
	public void testCreateSpotInstances() throws Exception {
		AmazonEC2Client ec2Client= new AmazonEC2Client(Initializer.getInstance().getCredentials());
		Initializer.getInstance().createSpotInstances(ec2Client, 1);
	}

}
