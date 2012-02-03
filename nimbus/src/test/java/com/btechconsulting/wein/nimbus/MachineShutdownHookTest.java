package com.btechconsulting.wein.nimbus;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class MachineShutdownHookTest {


	public void TestRun() {
		Thread t = new Thread(new MachineShutdownHook());
}
	public static void main(String[] args) {
		MachineShutdownHookTest h= new MachineShutdownHookTest();
		h.TestRun();
	}

}
