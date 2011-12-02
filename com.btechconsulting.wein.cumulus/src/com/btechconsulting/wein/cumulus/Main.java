package com.btechconsulting.wein.cumulus;

import com.btechconsulting.wein.cumulus.initialization.Initializer;

public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//check units on server
		System.out.println(Initializer.INSTANCE.getUnitsOnServer());
		//check sqs dispatch queue
		System.out.println(Initializer.INSTANCE.getDispatchQueue());
		//check sqs return queue
		System.out.println(Initializer.INSTANCE.getReturnQueue());
	}

}
