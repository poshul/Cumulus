package com.btechconsulting.wein.nimbus;

import java.io.IOException;

class MachineShutdownHook extends Thread {
	
	/**
	 * This shuts down the machine (not JVM) that java is running on.
	 */
	public void run(){
/*
		try {
			
			Process process = new ProcessBuilder(Constants.SHUTDOWNCOMMAND,Constants.SHUTDOWNMODIFIER,"now").start();
			process.waitFor();
		} catch (IOException | InterruptedException e) {
			System.err.println("couldn't shutdown, this is BAD"); //TODO email me here
			e.printStackTrace();
		}
*/
	}
}
