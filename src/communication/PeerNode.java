package communication;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import model.Action;
import model.FileObject;
import model.Message;
import model.MessageType;
import utilities.ChecksumGenerator;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.DatagramSocket;
import javafx.util.Pair;

public class PeerNode {

	// used in 2 threads. 1: to update list 2: to upload list to trackngsvr
	// access using getFileNames()
	private static List<String> fileNames = new ArrayList<String>();

	private static Map<String, String> filesWithChecksum = new HashMap<String, String>();
	private static int nRequests = 0;
	public static boolean updated = false;
	public static List<FileObject> localFiles = new ArrayList<FileObject>();

	String fileDir = null;
	String nodeIp = null;
	int nodePort = 0;
	public static String trackSvrIp = "localhost";
	public static int trackSvrPort = 4000;
	public static int trackSvrHeartbeatPort = 6000;
	public String servletIp;
	public static int servletPort;
	public static int servletSenderPort;
	public static Socket nodeToTrackerSocket;
	public static Socket nodeToTrackerHeartbeatSocket;
	public static DatagramSocket ds;
	private ChecksumGenerator generator;
	public static final String FILE_TO_DWNLD_FROM_T_SERVER = "file1.txt";
	public static final String DEFAULT_DIRECTORY_PATH = "/src/dirs/localhost_5051/";
	public static String configFilePath = System.getProperty("user.dir") + "/config.properties";
	public static final int NO_OF_ARGS = 6;

	public PeerNode(String nodeIp, int nodePort, String fileDir, String configFilePath, int servletPort) {
		this.fileDir = fileDir;
		this.nodeIp = nodeIp;
		this.nodePort = nodePort;
		PeerNode.configFilePath = configFilePath;
		this.servletIp = nodeIp;
		this.servletPort = servletPort;
		this.generator = new ChecksumGenerator();
	}

	// servletport , node IP, node port, file dir path, configFilePath, tracksvrIP
	public static void main(String args[]) {
		String nodeIp = "";
		int nodePort = 0;
		String dirPath = "";
		int port = 0;
		
		
		if (args.length < NO_OF_ARGS) {
			System.out.println("Please enter port to connect with Servlet as argument "+NO_OF_ARGS+" arguments are required");

		} else {
			servletPort = Integer.parseInt(args[0]);
			nodeIp = args[1];
			nodePort = Integer.parseInt(args[2]);
			dirPath = args[3];
			configFilePath  = args[4];
			trackSvrIp = args[5];
			
			//String nodeIp = "localhost";
			//int nodePort = 5051;
			//String fileDir = System.getProperty("user.dir") + "/src/dirs/" + nodeIp + "_" + nodePort + "/";
		//	String fileDir = System.getProperty("user.dir") + dirPath + nodeIp + "_" + nodePort + "/";
			//PeerNode node1 = new PeerNode("localhost", 5051, fileDir, configFilePath, 1231);
			PeerNode node1 = new PeerNode(nodeIp, nodePort, dirPath, configFilePath, servletPort);
			node1.updateFileList();

			node1.startServer();
			//node1.downloadFileFromTrackingServer();

		}

	}

	public void startServer() {
		try {
			nodeToTrackerSocket = new Socket(trackSvrIp, trackSvrPort);
			nodeToTrackerHeartbeatSocket = new Socket(trackSvrIp, trackSvrHeartbeatPort);
			System.out.println("Servlet port :" + servletPort);
			ds = new DatagramSocket(servletPort);

			// Thread to communicate with Tracking Server
			new PeerTrackerCommunication(nodeIp, nodePort, fileDir, configFilePath, servletPort, servletSenderPort);

			// Thread to communicate with peers
			PeerCommunication peerComm = new PeerCommunication(nodeIp, nodePort, fileDir, configFilePath, servletPort,
					servletSenderPort);
			peerComm.establishConnWithAllPeers();

			while (true) {
				// Communicate with Servlet
				PeerUICommunication peerUIComm = new PeerUICommunication(nodeIp, nodePort, fileDir, configFilePath,
						servletPort, servletSenderPort);
				Pair<Integer, String> p = peerUIComm.receiveFileName();
				servletSenderPort = p.getKey();
				System.out.println("Servlet sender port is: " + servletSenderPort);
				String filename = p.getValue();
				String[] arr = filename.split(";");
				for (String file_name : arr) {
					getFile(file_name);
				}

			}

		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Thread to update available file list by polling local directory updates
	 * happens every 60 sec
	 * 
	 * @return
	 */
	public synchronized void updateFileList() {
		Thread t1 = new Thread() {
			public void run() {
				System.out.println("Starting thread to poll local files directory");

				while (fileDir != null) {
					//File folder = new File(fileDir + nodeIp + "_" + nodePort + "/");
					File folder = new File(fileDir);
					folder.mkdir();
					System.out.println("folder: " + folder);
					File[] listOfFiles = folder.listFiles();
					System.out.println("No of files in the folder: " + listOfFiles.length);
					boolean isDeleted = listOfFiles.length < localFiles.size() ? true : false;
					System.out.println(
							"No of files in the local files list: " + localFiles.size() + " is Deleted " + isDeleted);

					for (File temp : listOfFiles) {
						if (!fileNames.contains(temp.getName())) {
							fileNames.add(temp.getName());
							String checksum = generator.generateChecksum(temp);

							FileObject newFile = new FileObject(temp.getName(), checksum);
							localFiles.add(newFile);
							System.out.println("Checksum is " + checksum + "file name is " + temp.getName());
							setUpdate(true);
						}
					}
					if (isDeleted) {
						List<String> newFilesNameList = new ArrayList<String>();
						for (File file : listOfFiles) {
							newFilesNameList.add(file.getName());
						}

						Iterator<String> itr = fileNames.iterator();
						while (itr.hasNext()) {
							String fileName = itr.next();
							if (!newFilesNameList.contains(fileName)) {
								itr.remove();
								deleteFileFromLocalFilesList(fileName);
							}
						}
						setUpdate(true);
					}

					try {
						Thread.sleep(30000);
					} catch (InterruptedException e) {

						e.printStackTrace();
					}
				}
				System.out.println("Given file directory is null. Please check");
			}

			private void deleteFileFromLocalFilesList(String fileName) {
				Iterator<FileObject> itr = localFiles.iterator();

				while (itr.hasNext()) {
					FileObject fileObj = itr.next();
					if (fileObj.getFilename().equals(fileName)) {
						itr.remove();
						break;
					}
				}
			}
		};
		t1.start();
	}

	// the value is set by thread checking for files in local directory
	private synchronized void setUpdate(boolean val) {

		final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
		rwl.writeLock().lock();
		this.updated = val;
		rwl.writeLock().unlock();

		System.out.println("Identified new file in the folder this.updated " + this.updated);
	}

	// used by thread handling communication between Node & trackingsvr
	public synchronized boolean hasUpdate() {
		return this.updated;
	}

	public synchronized List<String> getFileNames() {
		return fileNames;
	}

	public synchronized Map<String, String> getFileNamesWithChecksum() {
		return filesWithChecksum;
	}

	public synchronized List<FileObject> getFileObjList() {
		return localFiles;
	}

	public synchronized int getnRequests() {
		return PeerNode.nRequests;
	}

	public synchronized void incrementRequests() {
		PeerNode.nRequests = PeerNode.nRequests + 1;
	}

	public synchronized void decrementRequests() {
		PeerNode.nRequests = PeerNode.nRequests - 1;
	}

	public void getFile(String fileName) {
		System.out.println("In get file");
		System.out.println(
				"Processing GET file request. File: " + fileName + "nodeIp:" + nodeIp + " nodePort:" + nodePort);
		byte[] fileByteArr = getLocalFile(fileName);

		if (fileByteArr != null) {
			System.out.println("Found file in Local:" + fileByteArr);
			new PeerUICommunication(nodeIp, nodePort, fileDir, configFilePath, servletPort, servletSenderPort)
					.sendOutputtoUI("Chosen file name: " + fileName + "|File is already available in Local");

		} else {
			DataOutputStream dosTrackSvr;
			try {

				Message msg = new Message();
				msg.setAction(Action.FIND);
				msg.setMessageType(MessageType.REQUEST);
				msg.setFileName(fileName);
				msg.setSenderIp(nodeIp);
				msg.setSenderPort(nodePort);
				msg.setReceiverIp(trackSvrIp);
				msg.setReceiverPort(trackSvrPort);
				msg.setStartTime(System.currentTimeMillis());
				msg.setOutputMsg("Chosen file name: " + fileName + "|Send FIND Request to Tracking Server");
				dosTrackSvr = new DataOutputStream(nodeToTrackerSocket.getOutputStream());

				dosTrackSvr.writeUTF(new ObjectMapper().writeValueAsString(msg));
				System.out.println("Sent request to tracking server to get list of nodes containing file");

			} catch (Exception e) {
				System.out.println("Tracking Server is down. Could not send request");
				new PeerUICommunication(nodeIp, nodePort, fileDir, configFilePath, servletPort, servletSenderPort)
						.sendOutputtoUI("Tracking Server is down. Could not send request");

			}
		}
	}

	/**
	 * Return byte array of file in local directory
	 * 
	 * @param fileName
	 */
	public byte[] getLocalFile(String fileName) {
		byte[] byteArr;
		System.out.println("Looking for the local file:" + fileName);
		if (!getFileNames().contains(fileName)) {
			System.out.println(getFileNames());
			return null;
		}
		File file = new File(fileDir  + fileName);
		System.out.println("looking in the path:" + fileDir  + fileName);
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

	public void downloadFileFromTrackingServer() {

		DataOutputStream dos;
		try {
			dos = new DataOutputStream(nodeToTrackerSocket.getOutputStream());
			Message msg = new Message();
			msg.setAction(Action.DOWNLOAD_FROM_TRACK_SERVER);
			msg.setMessageType(MessageType.REQUEST);
			msg.setSenderIp(nodeIp);
			msg.setSenderPort(nodePort);
			msg.setReceiverIp(trackSvrIp);
			msg.setReceiverPort(trackSvrPort);
			msg.setFileName(FILE_TO_DWNLD_FROM_T_SERVER);

			ObjectMapper mapper = new ObjectMapper();
			dos.writeUTF(mapper.writeValueAsString(msg));
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}