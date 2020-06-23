package communication;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.ObjectMapper;

import model.Action;
import model.Message;
import model.MessageType;
import utilities.UtilityMethods;

/**
 * Handle communication between peer nodes
 * 
 * @author vcroopana
 *
 */
public class PeerCommunication extends PeerNode {

	String nodeIp;
	int nodePort;
	public String output;

	public PeerCommunication(String ip, int port, String fileDir, String configFilePath, int servletPort, int servletSenderPort) {
		super(ip, port, fileDir, configFilePath, servletPort);
		this.nodePort = port;
		this.nodeIp = ip;
	}

	public String establishConnWithAllPeers() {
		// Get list of nodes running from tracking server
		// ConcurrentHashMap<String, Integer> nodeIpPortMap =
		// TrackingServer.getNodeIpPortMap();
		Thread acceptPeerConnections = new Thread() {
			public void run() {
				try {
					ServerSocket currNodeSocket = new ServerSocket(nodePort);

					while (true) {
						Socket currNodePeerSocket = currNodeSocket.accept();
						System.out.println("Accepted connection from peer with Port:" + currNodePeerSocket.getPort());
						PeerCommunicationThread peercommunicationThread = new PeerCommunicationThread(
								currNodePeerSocket, nodeIp, nodePort, fileDir, configFilePath, servletPort, servletSenderPort);
						new Thread(peercommunicationThread).start();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
		acceptPeerConnections.start();
		return output;
	}
}

class PeerCommunicationThread extends PeerCommunication implements Runnable {
	Socket peerSocket = null;
	DataInputStream dis;
	DataOutputStream dos;
	PeerResponseHandler peerResponseHandler;

	PeerCommunicationThread(Socket peerSocket, String ip, int port, String fileDir, String configFilePath, int servletPort, int servletSenderPort) {

		super(ip, port, fileDir, configFilePath, servletPort, servletSenderPort);
		this.peerSocket = peerSocket;
		// this.peerResponseHandler = new PeerResponseHandler(configFilePath);
	}

	public void run() {
		try {
			this.dis = new DataInputStream(this.peerSocket.getInputStream());
			this.dos = new DataOutputStream(this.peerSocket.getOutputStream());
			while (true) { // keep running to receive inputs from peers

				while (this.dis.available() < 1) {
					Thread.sleep(500);
				}
				String input = this.dis.readUTF();
				System.out.println("Received input message at peer:" + input);
				ObjectMapper objectMapper = new ObjectMapper();
				Message inputMsg = objectMapper.readValue(input, Message.class);

				// RESPONSE DOWNLOAD REQUEST SEND FILE
				// RESPONSE GETCURRENTLOAD
				if (inputMsg.getMessageType() == MessageType.RESPONSE) {

					output = PeerResponseHandler.handle(inputMsg, fileDir, configFilePath, servletPort, servletSenderPort);
					if (inputMsg.getAction() == Action.DOWNLOAD) {
						decrementRequests();
					}
				}

				else if (inputMsg.getAction() == Action.DOWNLOAD) {

					incrementRequests(); // increment req count

					byte[] fileByteArr = getLocalFile(inputMsg.getFileName());
					Socket socket = new Socket(inputMsg.getSenderIp(), inputMsg.getSenderPort());

					inputMsg.setMessageType(MessageType.RESPONSE);
					inputMsg.setReceiverIp(inputMsg.getSenderIp());
					inputMsg.setReceiverPort(inputMsg.getSenderPort());
					inputMsg.setSenderIp(nodeIp);
					inputMsg.setSenderPort(nodePort);
					dos = new DataOutputStream(socket.getOutputStream());

					if (fileByteArr != null) {

						inputMsg.setFile(fileByteArr);
						int latency = UtilityMethods.getLatencyBnNodes(configFilePath, inputMsg.getReceiverPort(),
								inputMsg.getSenderPort());
						// emulating the latency as per config file
						// subtracting 30 ms to account for the operations so far
						if (latency - 30 > 0) {
							Thread.sleep(latency);
						}
						inputMsg.setOutputMsg(inputMsg.getOutputMsg()+"| Send file to Peer: "+ inputMsg.getReceiverIp()+"_"+inputMsg.getReceiverPort());

						dos.writeUTF(objectMapper.writeValueAsString(inputMsg));

					} else {
						System.out.println("File is not available. Sending empty response");
						inputMsg.setOutputMsg(inputMsg.getOutputMsg()+"| File is not available. Sending empty response");

						dos.writeUTF(objectMapper.writeValueAsString(inputMsg));
					}
					decrementRequests(); // decrement req count
					socket.close();
				} else if (inputMsg.getAction() == Action.GETLOAD) {

					int n = getnRequests();
					Socket socket = new Socket(inputMsg.getSenderIp(), inputMsg.getSenderPort());
					inputMsg.setLoad(n);
					inputMsg.setMessageType(MessageType.RESPONSE);
					inputMsg.setReceiverIp(inputMsg.getSenderIp());
					inputMsg.setReceiverPort(inputMsg.getSenderPort());
					inputMsg.setSenderIp(nodeIp);
					inputMsg.setSenderPort(nodePort);
					inputMsg.setOutputMsg(inputMsg.getOutputMsg()+"| Send GETLOAD response to Peer: "+ inputMsg.getReceiverIp()+"_"+inputMsg.getReceiverPort());

					dos = new DataOutputStream(socket.getOutputStream());
					dos.writeUTF(objectMapper.writeValueAsString(inputMsg));
					socket.close();
					System.out.println("Responded to Peer Request with Load = " + n + ", " + inputMsg);
				}
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}

}