/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package communication;

import java.net.DatagramPacket;
import java.net.InetAddress;
import javafx.util.Pair;

/**
 *
 * @author ranjabati sen
 */
public class PeerUICommunication extends PeerNode {
	int servletPort;
	int servletSenderPort;

	public PeerUICommunication(String nodeIp, int nodePort, String fileDir, String configFilePath, int servletPort,
			int servletSenderPort) {
		super(nodeIp, nodePort, fileDir, configFilePath, servletPort);
		this.servletPort = servletPort;
		this.servletSenderPort = servletSenderPort;
	}

	public Pair<Integer, String> receiveFileName() {
		int port = this.servletPort;
		String filename = "";
		try {
			byte[] receive = new byte[65535];
			DatagramPacket DpReceive = null;
			DpReceive = new DatagramPacket(receive, receive.length);
			ds.receive(DpReceive);
			port = DpReceive.getPort();
			servletSenderPort = port;
			System.out.println("Servlet sender port in receive file name is :" + servletSenderPort);
			filename = data(receive).toString();
			System.out.println("Message received from Java Servlet :-" + filename);
			// getFile(filename);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new Pair<Integer, String>(port, filename);
	}

	public void sendOutputtoUI(String output) {
		try {
			InetAddress ip = InetAddress.getLocalHost();
			byte buf[] = null;
			buf = output.getBytes();
			System.out.println("Sending message : " + output + " to UI on port: " + this.servletSenderPort);
			DatagramPacket DpSend = new DatagramPacket(buf, buf.length, ip, this.servletSenderPort);
			ds.send(DpSend);
			System.out.println("Sent output message to UI "+data(buf).toString());
		} catch (Exception e) {
			//Exception when trying to sned message to UI client which is not connected
			// Not required to be sent, hence catch and do nothing 
			System.out.println(e.getMessage());

		}
	}

	public static StringBuilder data(byte[] a) {
		if (a == null)
			return null;
		StringBuilder ret = new StringBuilder();
		int i = 0;
		while (a[i] != 0) {
			ret.append((char) a[i]);
			i++;
		}
		return ret;
	}

}