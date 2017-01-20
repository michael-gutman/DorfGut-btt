package com.codenvy.example.swing;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class InfoHashGen {
    public static String generate(String filePath, boolean format) {
		File file = new File(filePath);
		MessageDigest sha1 = null;;
		try {
			sha1 = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		InputStream input = null;

		try {
		    input = new FileInputStream(file);
		    StringBuilder builder = new StringBuilder();
		    while (!builder.toString().endsWith("4:info")) {
		        builder.append((char) input.read());
		    }
		    ByteArrayOutputStream output = new ByteArrayOutputStream();
		    for (int data; (data = input.read()) > -1; output.write(data));
		    sha1.update(output.toByteArray(), 0, output.size() - 1);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
		    if (input != null) try { input.close(); } catch (IOException ignore) {}
		}

		byte[] hash = sha1.digest();
		
		if (format) return byteArrayToURLString(hash);
		else return hash.toString();
	}
	
	public static String byteArrayToURLString(byte in[]) {
		byte ch = 0x00;
		int i = 0;
		if (in == null || in.length <= 0)
			return null;

		String pseudo[] = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
				"A", "B", "C", "D", "E", "F" };
		StringBuffer out = new StringBuffer(in.length * 2);

		while (i < in.length) {
			// First check to see if we need ASCII or HEX
			if ((in[i] >= '0' && in[i] <= '9')
					|| (in[i] >= 'a' && in[i] <= 'z')
					|| (in[i] >= 'A' && in[i] <= 'Z')
					|| in[i] == '-' || in[i] == '_' || in[i] == '.'
					|| in[i] == '~') {
				out.append((char) in[i]);
				i++;
			} else {
				out.append('%');
				ch = (byte) (in[i] & 0xF0); // Strip off high nibble
				ch = (byte) (ch >>> 4); // shift the bits down
				ch = (byte) (ch & 0x0F); // must do this is high order bit is
				// on!
				out.append(pseudo[(int) ch]); // convert the nibble to a
				// String Character
				ch = (byte) (in[i] & 0x0F); // Strip off low nibble
				out.append(pseudo[(int) ch]); // convert the nibble to a
				// String Character
				i++;
			}
		}

		String rslt = new String(out);
		return rslt;
	}
}
