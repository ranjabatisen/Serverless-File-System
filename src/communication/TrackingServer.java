package communication;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import model.Action;
import model.Message;
import model.MessageType;
import utilities.UtilityMethods;
import model.FileObject;

public class TrackingServer {

	static ServerSocket trackSvrSocket;
	public static String trackSvrIp = "localhost";
	static int trackSvrPort = 4000;

	static ServerSocket heartbeatSvrSocket;
	static int heartbeatSvrPort = 6000;

	private static final String DIRECTORY_PATH = "/src/dirs/localhost_5051/";

	private static volatile ConcurrentHashMap<String, Map<String, String>> nodeFilesListMap = new ConcurrentHashMap<String, Map<String, String>>();
	

	public static void heartbeat(Socket heartbeatSocket) {
		
		Thread heart = new Thread() {
			public void run() {
				System.out.println("Starting thread to send heartbeats to every node");
				while (true) {
					try {
						DataOutputStream dos = new DataOutputStream(heartbeatSocket.getOutputStream());
						dos.writeUTF("Heartbeat");

						DataInputStream dis = new DataInputStream(heartbeatSocket.getInputStream());
						while (dis.available() < 1) {

						}
						String received = dis.readUTF();
						//System.out.println("Message received from node:" + received);
						Thread.sleep(7000);
					} catch (Exception e) {
						System.out.println("Exception in hreat thread");
					}
				}
			}
		};
		heart.start();
	}

	public static synchronized void find(Message message, Socket nodeSocket) {
		System.out.println("Checking list of nodes containing file");
		DataOutputStream dos;
		ObjectMapper mapper = new ObjectMapper();
		String filename = message.getFileName();
		System.out.println("Name of file to be found:" + filename);
		Map<String, String> nodes_checksum = new HashMap<String, String>();

		for (Map.Entry mapElement : nodeFilesListMap.entrySet()) {
			String key = (String) mapElement.getKey();
			Map<String, String> file_checksum = nodeFilesListMap.get(key);
			if (file_checksum.containsKey(filename)) {
				// map of node and given file's check sum
				nodes_checksum.put(key, file_checksum.get(filename));
			}
		}
		System.out.println("# of nodes containing the file: " + nodes_checksum.size());
		try {
			dos = new DataOutputStream(nodeSocket.getOutputStream());
			Message msg = new Message();
			msg.setAction(Action.FIND);
			msg.setMessageType(MessageType.RESPONSE);
			msg.setReceiverIp(message.getSenderIp());
			msg.setReceiverPort(message.getSenderPort());
			msg.setSenderIp(trackSvrIp);
			msg.setSenderPort(trackSvrPort);
			msg.setFileName(message.getFileName());
			msg.setFileContainingNodeswithChecksum(nodes_checksum);
			msg.setStartTime(message.getStartTime());
			msg.setOutputMsg(message.getOutputMsg() + "| Send List of Peers with file to the recipient node: "+ msg.getReceiverIp() + "_" + msg.getReceiverPort());
			dos.writeUTF(mapper.writeValueAsString(msg));
			System.out
					.println("Sent file containing nodes to peer:" + msg.getReceiverIp() + ":" + msg.getReceiverPort());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static synchronized void updatelist(Message message) {
		System.out.println("Storing updated list of files in a node");
		List<FileObject> files = message.getFileObjList();
		Map<String, String> file_checksum = new HashMap<String, String>();

		for (int i = 0; i < files.size(); i++) {
			FileObject obj = files.get(i);
			System.out.println("Files names are " + obj.getFilename());
			file_checksum.put(obj.getFilename(), obj.getChecksum());
		}
		System.out.println("Number of files in new node file list:" + file_checksum.size());
		String nodeIdentifier = message.getSenderIp() + ":" + message.getSenderPort();

		// nodeFilesListMap.replace(nodeIdentifier, file_checksum);
		nodeFilesListMap.put(nodeIdentifier, file_checksum);
		// System.out.println("nodeFilesListMap size after updating:
		// "+nodeFilesListMap.size());
	}

	public static synchronized void filelist(Message message) {
		System.out.println("Storing list of files in a node");
		List<FileObject> files = message.getFileObjList();
		Map<String, String> file_checksum = new HashMap<String, String>();

		for (int i = 0; i < files.size(); i++) {
			FileObject obj = files.get(i);
			file_checksum.put(obj.getFilename(), obj.getChecksum());
		}
		System.out.println("Number of files in node:" + file_checksum.size());

		String nodeIdentifier = message.getSenderIp() + ":" + message.getSenderPort();
		nodeFilesListMap.put(nodeIdentifier, file_checksum);
	}

	public static void peer(Socket nodeSocket) {
		Thread peerthread = new Thread() {
			public void run() {
				System.out.println("Starting thread for node");
				DataInputStream dis;
				ObjectMapper mapper = new ObjectMapper();
				while (true) {
					try {
						dis = new DataInputStream(nodeSocket.getInputStream());
						while (dis.available() < 1) {
							Thread.sleep(400);
						}
						Message message = mapper.readValue(dis.readUTF(), Message.class);
						System.out.println("Received message at tracking server:" + message.toString());
						if (message.getMessageType() == MessageType.REQUEST && message.getAction() == Action.FILELIST) {
							filelist(message);
						} else if (message.getMessageType() == MessageType.REQUEST
								&& message.getAction() == Action.UPDATELIST) {
							updatelist(message);
						} else if (message.getMessageType() == MessageType.REQUEST
								&& message.getAction() == Action.FIND) {
							find(message, nodeSocket);
						} else if (message.getMessageType() == MessageType.REQUEST
								&& message.getAction() == Action.DOWNLOAD_FROM_TRACK_SERVER) {
							sendFileContentToNode(message, nodeSocket);
						}

					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		};
		peerthread.start();
	}

	protected static void sendFileContentToNode(Message message, Socket nodeSocket) {

		File folder = new File(System.getProperty("user.dir") + DIRECTORY_PATH);
		folder.mkdir();
		System.out.println("folder: " + folder);
		File[] listOfFiles = folder.listFiles();
		System.out.println("No of files in the folder " + listOfFiles.length);

		for (File f : listOfFiles) {
			if (f.getName().equals(message.getFileName())) {
				sendFileContentToNode(message, nodeSocket, f);

				break;
			}
		}

	}

	private static void sendFileContentToNode(Message message, Socket nodeSocket, File f) {

		try {
			DataOutputStream dos = new DataOutputStream(nodeSocket.getOutputStream());
			Message msg = new Message();
			msg.setAction(Action.DOWNLOAD_FROM_TRACK_SERVER);
			msg.setMessageType(MessageType.RESPONSE);
			msg.setReceiverIp(message.getSenderIp());
			msg.setReceiverPort(message.getSenderPort());
			msg.setSenderIp(trackSvrIp);
			msg.setSenderPort(trackSvrPort);
			byte[] fileBytesArray = UtilityMethods.convertLocalFileToByteArray(f.getAbsoluteFile());
			msg.setFile(fileBytesArray);
			msg.setFileName(message.getFileName());
			ObjectMapper mapper = new ObjectMapper();
			System.out.println("File sent to the client");

			dos.writeUTF(mapper.writeValueAsString(msg));
			System.out.println("File sent to the client");

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String args[]) {
	
			try {
				trackSvrSocket = new ServerSocket(trackSvrPort);
				System.out.println("Started Tracking server on port:" + trackSvrPort);
				heartbeatSvrSocket = new ServerSocket(heartbeatSvrPort);
				System.out.println("Started hearbeat in tracking server on port:" + heartbeatSvrPort);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			Thread acceptPeerConnections = new Thread() {
				public void run() {
					while (true) {
						Socket nodeSocket;
						try {
							nodeSocket = trackSvrSocket.accept();
							peer(nodeSocket);

							Socket heartbeatSocket = heartbeatSvrSocket.accept();
							heartbeat(heartbeatSocket);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			};
			acceptPeerConnections.start();
		}

	

	
}