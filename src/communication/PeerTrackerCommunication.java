package communication;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.ObjectMapper;

import model.Action;
import model.Message;
import model.MessageType;
import utilities.UtilityMethods;

/**
 * Handles communication between Node and Tracking Server
 * 
 * @author vcroopana
 *
 */
public class PeerTrackerCommunication extends PeerNode {

	String nodeIp;
	int nodePort;
	public static boolean isConnected = true;
	Thread heartbeatListerner;
	Thread uploader;
	Thread listener;

	public PeerTrackerCommunication(String ip, int port, String fileDir, String configFilePath, int servletPort,
			int servletSenderPort) {
		super(ip, port, fileDir, configFilePath, servletPort);
		nodeIp = ip;
		nodePort = port;
		// register();
		uploadList();
		listenTrackerResponse();
		listenToHeartbeat();
	}

	/**
	 * upload current file list to tracking server
	 * 
	 * @param fileNames
	 */
	public synchronized void uploadList() {

		uploader = new Thread() {
			public void run() {
				System.out.println("Starting thread to upload files to tracking server");
				while (!uploader.isInterrupted()) {
					// super class's method
					// System.out.println("Checking has update " + updated);
					if (updated) {
						System.out.println("Uploading new files list to the server");
						// code to upload list to tracking server
						try {
							DataOutputStream dos = new DataOutputStream(nodeToTrackerSocket.getOutputStream());
							Message msg = new Message();
							msg.setAction(Action.UPDATELIST);
							msg.setMessageType(MessageType.REQUEST);
							msg.setSenderIp(nodeIp);
							msg.setSenderPort(nodePort);
							msg.setReceiverIp(trackSvrIp);
							msg.setReceiverPort(trackSvrPort);
							msg.setmapofFilesinNodeWithChecksum(getFileNamesWithChecksum());
							msg.setFileObjList(getFileObjList());
							ObjectMapper objMapper = new ObjectMapper();

							dos.writeUTF(objMapper.writeValueAsString(msg));
							System.out.println("wrote update list message to the server");

							updated = false;
						} catch (SocketException e) {
							try {
								System.out.println("Sleeping because server went down");
								Thread.sleep(5000);
								isConnected = false;
								stopAllThread();

							} catch (InterruptedException e1) {
							}
						} catch (IOException e) {
							e.printStackTrace();
						}

					} else {
						try {
							Thread.sleep(3000);
						} catch (InterruptedException e) {
							System.out.println(" Thread to upload files to tracking server is interrupted");
						}
					}
				}

				System.out.println("Stopping thread to upload files to tracking server");
			}
		};
		uploader.start();

	}

	public void listenTrackerResponse() {
		listener = new Thread() {
			public void run() {
				System.out.println("Starting thread to listen to tracking server");
				DataInputStream dis;
				try {
					dis = new DataInputStream(nodeToTrackerSocket.getInputStream());

					ObjectMapper mapper = new ObjectMapper();
					while (!listener.isInterrupted()) {
						while (dis.available() < 1) {
							Thread.sleep(400);
						}
						Message message = mapper.readValue(dis.readUTF(), Message.class);
						System.out.println("Received Message from Tracking server:" + message.toString());

						Map<String, String> fileContainingNodesChecksum;

						if (message.getMessageType() == MessageType.RESPONSE && message.getAction() == Action.FIND) {

							fileContainingNodesChecksum = message.getFileContainingNodeswithChecksum();
							System.out.println("No of nodes containing file:" + fileContainingNodesChecksum.size());
							if (fileContainingNodesChecksum.size() == 0) {

								System.out.println("File is not available for download on any peer node");
								new PeerUICommunication(nodeIp, nodePort, fileDir, configFilePath, servletPort,
										servletSenderPort)
												.sendOutputtoUI("Chosen file name: " + message.getFileName() + "|File is not available for download on any Live peer node");

							} else {
								// send get load requests to peers
								broadcastReqToPeers(fileContainingNodesChecksum, message.getFileName(),
										message.getOutputMsg(), message.getStartTime());
								// This Request will be handled in PeerCommunication class
							}

						} else if (message.getMessageType() == MessageType.RESPONSE
								&& message.getAction() == Action.DOWNLOAD_FROM_TRACK_SERVER) {
							System.out.println("File recieved from tracking server");
							byte[] receivedFile = message.getFile();
							// TODO : how to save file with same name twice
							String path = System.getProperty("user.dir") + DEFAULT_DIRECTORY_PATH + "newdwnfile.txt";
							boolean response = UtilityMethods.convertByteArrayToFile(receivedFile, path);
							if (response) {
								System.out.println("File written at node ");
							}
						}
					}
				} catch (SocketException e) {
					try {
						System.out.println("Sleeping because server went down");
						Thread.sleep(5000);
						isConnected = false;
						stopAllThread();

					} catch (InterruptedException e1) {
					}
				} catch (IOException | InterruptedException e) {
					System.out.println(" Thread to  listen to tracking server is interrupted");

				}
				System.out.println("Stopping thread to  listen to tracking server");

			}
		};
		listener.start();
	}

	public void broadcastReqToPeers(Map<String, String> nodeCheckSumMap, String fileName, String outputMsg,
			long startTime) {

		Message loadReq = new Message();

		loadReq.setSenderIp(nodeIp);
		loadReq.setSenderPort(nodePort);
		loadReq.setMessageType(MessageType.REQUEST);
		loadReq.setAction(Action.GETLOAD);
		loadReq.setFileName(fileName);
		loadReq.setStartTime(startTime);

		nodeCheckSumMap = removeDeadNodes(nodeCheckSumMap);
		if (nodeCheckSumMap.size() == 0) {
			System.out.println("File is not available for download on any peer node");
			new PeerUICommunication(nodeIp, nodePort, fileDir, configFilePath, servletPort, servletSenderPort)
					.sendOutputtoUI("Chosen file name: " + fileName + "|File is not available for download on any Live peer node");

		} else {
			Map<String, String> nodeCheckSumMapRemoveFailures = new HashMap<String, String>();
			nodeCheckSumMapRemoveFailures.putAll(nodeCheckSumMap);

			for (Entry<String, String> nodeId : nodeCheckSumMap.entrySet()) {
				String peerIp = nodeId.getKey().split(":")[0];
				int peerPort = Integer.valueOf(nodeId.getKey().split(":")[1]);

				try {
					Socket peerToPeerSocket = new Socket(peerIp, peerPort);
					// send download file request
					DataOutputStream dosPeer = new DataOutputStream(peerToPeerSocket.getOutputStream());

					loadReq.setReceiverIp(peerIp);
					loadReq.setReceiverPort(peerPort);
					if (nodeCheckSumMapRemoveFailures.size() == 0) {
						System.out.println("File is not available for download on any Live peer node");
						new PeerUICommunication(nodeIp, nodePort, fileDir, configFilePath, servletPort,
								servletSenderPort)
										.sendOutputtoUI("Chosen file name: " + fileName + "|File is not available for download on any Live peer node");
					}
					loadReq.setFileContainingNodeswithChecksum(nodeCheckSumMapRemoveFailures);
					loadReq.setOutputMsg(outputMsg + "| Send GETLOAD Request to Peer: " + loadReq.getReceiverIp() + "_"
							+ loadReq.getReceiverPort());

					dosPeer.writeUTF(new ObjectMapper().writeValueAsString(loadReq));
					System.out.println("Sent get load Request to peer: " + loadReq.toString());
					peerToPeerSocket.close();
					// this message will be processed in PeerCommunication
				} catch (IOException e) {
					System.out.println(
							"Could not establish socket connection with Peer Ip: " + peerIp + " Port: " + peerPort);
					nodeCheckSumMapRemoveFailures.remove(nodeId.getKey());
				}
			}
		}

	}

	private Map<String, String> removeDeadNodes(Map<String, String> nodeCheckSumMap) {
		Map<String, String> result = new HashMap<String, String>();
		for (Entry<String, String> nodeId : nodeCheckSumMap.entrySet()) {
			String peerIp = nodeId.getKey().split(":")[0];
			int peerPort = Integer.valueOf(nodeId.getKey().split(":")[1]);

			try {
				Socket testSocket = new Socket(peerIp, peerPort);
				result.put(nodeId.getKey(), nodeId.getValue());
				testSocket.close();
			} catch (IOException e) {
				System.out.println("DEAD PEER! Could not establish socket connection with Peer Ip: " + peerIp
						+ " Port: " + peerPort);
			}
		}
		return result;
	}

	public void listenToHeartbeat() {
		heartbeatListerner = new Thread() {

			public void run() {
				System.out.println("Starting thread to listen to heartbeats of tracking server");
				DataInputStream dis;
				DataOutputStream dos;

				try {
					dis = new DataInputStream(nodeToTrackerHeartbeatSocket.getInputStream());
					dos = new DataOutputStream(nodeToTrackerHeartbeatSocket.getOutputStream());

					while (!heartbeatListerner.isInterrupted() && isConnected) {

						String response = dis.readUTF();
						// System.out.println("Received heartbeat from server, " + response);

						dos.writeUTF(response);
						Thread.sleep(7000);
					}

				} catch (SocketException e) {
					try {
						System.out.println("Sleeping because server went down");
						Thread.sleep(5000);
						isConnected = false;
						stopAllThread();

					} catch (InterruptedException e1) {
					}
				} catch (IOException e) {
					// System.out.println("Exception at peer Tracking Server");
				} catch (InterruptedException e) {
					System.out.println(" Thread to listen to heartbeats of tracking server is interrupted");
				}
				System.out.println("Stopping thread to listen to heartbeats of tracking server");
//				new PeerUICommunication(nodeIp, nodePort, fileDir, configFilePath, servletPort, servletSenderPort)
//						.sendOutputtoUI("Tracking Server is down. Cannot download the file until it comes up");

				nodeToTrackerHeartbeatSocket = null;
				nodeToTrackerSocket = null;
				isConnected = false;
				uploader.interrupt();
				listener.interrupt();				

				tryConnection();
				System.out.println("All threads are stopped----------");
			}

		};

		heartbeatListerner.start();
	}

	private void startAllThreads() {
		System.out.println("Starting all the threads again");
		uploadList();
		listenTrackerResponse();
		listenToHeartbeat();

	}

	protected void stopAllThread() {
		System.out.println("sockets are unusable now----------");
		nodeToTrackerHeartbeatSocket = null;
		nodeToTrackerSocket = null;
		System.out.println("Socket set unusable to true");
		heartbeatListerner.interrupt();
		uploader.interrupt();
		listener.interrupt();

		tryConnection();

	}

	private void tryConnection() {
		while (!isConnected) {
			try {
				nodeToTrackerSocket = new Socket(trackSvrIp, trackSvrPort);
				nodeToTrackerHeartbeatSocket = new Socket(trackSvrIp, trackSvrHeartbeatPort);
				isConnected = true;
				System.out.println("Server is up, establishing connection");
				new PeerUICommunication(nodeIp, nodePort, fileDir, configFilePath, servletPort, servletSenderPort)
						.sendOutputtoUI("Server is up, establishing connection");
				updated = true;
				startAllThreads();
				break;

			} catch (SocketException e) {
				isConnected = false;
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e1) {

				}
				System.out.println("Server is not up yet, all communication is stopped");
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}
}