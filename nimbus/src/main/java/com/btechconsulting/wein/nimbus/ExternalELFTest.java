package com.btechconsulting.wein.nimbus;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class ExternalELFTest {
	public static void main(String[] args) {
		try {
			Process process = new ProcessBuilder(args).start();
			InputStream is= process.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
