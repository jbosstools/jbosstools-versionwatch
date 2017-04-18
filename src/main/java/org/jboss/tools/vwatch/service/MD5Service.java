package org.jboss.tools.vwatch.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Service {
	private MessageDigest complete = null;
	private static MD5Service instance = null;

	private MD5Service() throws NoSuchAlgorithmException {
		complete = MessageDigest.getInstance("MD5");
	}

	public static MD5Service getInstance() {
		if (instance == null) {
			try {
				instance = new MD5Service();
			} catch (NoSuchAlgorithmException e) {
				System.out.println("error");
				throw new RuntimeException(e.getMessage());
			}
		}
		return instance;
	}


	private  byte[] createChecksum(File filename) throws Exception {
		InputStream fis =  new FileInputStream(filename);

		byte[] buffer = new byte[1024];
		int numRead;

		do {
			numRead = fis.read(buffer);
			if (numRead > 0) {
				complete.update(buffer, 0, numRead);
			}
		} while (numRead != -1);

		fis.close();
		return complete.digest();
	}

	// see this How-to for a faster way to convert
	// a byte array to a HEX string
	public  String getMD5(File filename)  {
		byte[] b = new byte[0];
		try {
			b = createChecksum(filename);
		} catch (Exception e) {
			e.printStackTrace();
		}
		String result = "";

		for (int i=0; i < b.length; i++) {
			result += Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
		}
		return result;
	}
}
