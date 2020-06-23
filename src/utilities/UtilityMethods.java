/**
 * 
 */
package utilities;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

/**
 * @author nenem
 *
 */
public class UtilityMethods {

	public static boolean convertByteArrayToFile(byte[] receivedFile, String path) {

		boolean result = true;

		try {
			File file = new File(path);
			System.out.println("path is " + path + " bytes length is " + receivedFile.length);
			FileOutputStream outputStream = new FileOutputStream(file);
			outputStream.write(receivedFile);
			outputStream.close();
		} catch (IOException e) {
			result = false;
		}

		return result;
	}

	public static byte[] convertLocalFileToByteArray(File file) {
		byte[] byteArr;
		File currFile = file.getAbsoluteFile();
		byteArr = new byte[(int) currFile.length()];
		FileInputStream fis;
		try {
			fis = new FileInputStream(currFile);
			new BufferedInputStream(fis).read(byteArr, 0, byteArr.length);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return byteArr;
	}

	public static int getLatencyBnNodes(String configFilePath, int nodeid1, int nodeid2) {
		System.out.println("Fetching latency for nodes, node1: " + nodeid1 + " node2:" + nodeid2 );
		Properties properties = new Properties();
		int latency = Integer.MAX_VALUE;
		try (InputStream input = new FileInputStream(configFilePath)) {

			properties.load(input);
			latency = nodeid1 < nodeid2
					? Integer.valueOf(properties.getProperty(String.valueOf(nodeid1) + "." + String.valueOf(nodeid2)))
					: Integer.valueOf(properties.getProperty(String.valueOf(nodeid2) + "." + String.valueOf(nodeid1)));

			System.out.println("node1: " + nodeid1 + " node2:" + nodeid2 + " latency between nodes: " + latency);
		} catch (Exception ex) {
			System.out.println("node1:" + nodeid1 + " node2:" + nodeid2 + ". Returning Integer.MAX_VALUE");
			return latency;
		}
		return latency;
	}

}
