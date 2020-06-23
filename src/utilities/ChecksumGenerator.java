/**
 * 
 */
package utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author nenem Class to generate and verify checksum of the file
 */
public class ChecksumGenerator {

	static MessageDigest digest;
	public static final String SHA_1 = "SHA-1";

	public ChecksumGenerator() {

		try {
			this.digest = MessageDigest.getInstance(SHA_1);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

	public static String generateChecksum(File file) {

		FileInputStream fis;
		// Create byte array to read data in chunks
		byte[] byteArray = new byte[1024];
		int bytesCount = 0;
		try {
			fis = new FileInputStream(file);

			// Read file data and update in message digest
			while ((bytesCount = fis.read(byteArray)) != -1) {
				digest.update(byteArray, 0, bytesCount);
			}
			;

			// close the stream; We don't need it now.
			fis.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Get the hash's bytes
		byte[] bytes = digest.digest();

		StringBuilder checksum = new StringBuilder();
		for (int i = 0; i < bytes.length; i++) {
			checksum.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
		}

		// return complete hash
		return checksum.toString();

	}

	public static boolean isCheckSumCorrect(File file, String originalChecksum) {

		boolean result = false;
		String newChecksum = generateChecksum(file);
		if (newChecksum.equals(originalChecksum)) {
			result = true;
		}
		return result;

	}

}
