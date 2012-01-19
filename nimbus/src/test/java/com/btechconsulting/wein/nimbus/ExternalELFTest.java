package com.btechconsulting.wein.nimbus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

public class ExternalELFTest {



	public static synchronized void main(String[] args) {
		try {

			if (args.length <= 0) {
				System.err.println("Need command to run");
				System.exit(-1);
			}
			Process process = new ProcessBuilder(args).start();
			Thread.sleep(10000);
			process.exitValue();
			InputStream is= process.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String line;
			System.out.printf("Output of running %s is:", Arrays.toString(args));
			while ((line = br.readLine()) != null){
				System.out.println(line);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
