package communication;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import model.Action;
import model.Message;
import model.MessageType;
import utilities.ChecksumGenerator;
import utilities.UtilityMethods;

public class PeerResponseHandler {
	public static volatile Map<String, HashMap<String, ArrayList<Integer>>> reqIdNodeIdLoadMap = new HashMap<String, HashMap<String, ArrayList<Integer>>>();
	// map of load req id and times
	public static volatile Map<String, Long> reqIdTimeStamp = new HashMap<String, Long>();
	public static volatile Map<String, ArrayList<Message>> reqIdGetLoadResponses = new HashMap<String, ArrayList<Message>>();
	final static ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();

	// private static final Long DOWNLOAD_REQUEST_TIME_OUT_THRESHOLD = 10000L;
	String configFilePath = null;

	public static synchronized String handle(Message response, String fileDir, String configFilePath, int servletPort,
			int servletSenderPort) {

		String output = "";
		PeerUICommunication peerUIComm = new PeerUICommunication(response.getReceiverIp(), response.getReceiverPort(),
				fileDir, configFilePath, servletPort, servletSenderPort);
		if (response.getAction() == Action.DOWNLOAD) {
			byte[] receivedFile = response.getFile();
			output = "Received file from Peer:";
			System.out.println("Received file from Peer: " + response.getFileName() + " : " + response.getFile());

			if (receivedFile == null || receivedFile.length <= 0) {
				output = response.getOutputMsg() + "| Received empty file";
				peerUIComm.sendOutputtoUI(output);
				System.out.println("Received empty file");

			} else if (isCheckSumMatch(response)) {
				saveFileAtPeer(fileDir, response.getReceiverIp() + "_" + response.getReceiverPort(),
						response.getFileName(), receivedFile);
				long timeTaken = System.currentTimeMillis()- response.getStartTime();
				output = response.getOutputMsg() + "|Downloaded from Peer " + response.getSenderPort()
						+ ". Checksum match successful. Saved file at Current Node|"+"Time taken to process download request: "+ String.valueOf(timeTaken)+" ms";
				peerUIComm.sendOutputtoUI(output);

				System.out.println("Checksum match successful. Saved file at Current Node");

			} else {
				output = response.getOutputMsg() + "| File downloaded from " + response.getSenderPort()
						+ " is corrupt. Check sum did not match";
				System.out.println(
						"Downloaded file from " + response.getSenderPort() + " is corrupt. Check sum did not match");
				TreeMap<Integer, String> latencyMap = response.getLatencyNodeMap();

				if (latencyMap.size() == 0) {
					System.out.println("No node available to download file");
					output = output + "|" + "No other node available to download file";
					peerUIComm.sendOutputtoUI(output);

				} else {
					String bestPeer = latencyMap.pollFirstEntry().getValue();
					String receiverIp = bestPeer.split(":")[0];
					int receiverPort = Integer.valueOf(bestPeer.split(":")[1]);

					DataOutputStream dosPeer = null;
					Message downloadRequest = null;

					try {
						dosPeer = new DataOutputStream(new Socket(receiverIp, receiverPort).getOutputStream());
						downloadRequest = createDownloadRequest(receiverIp, receiverPort, response, latencyMap, output);
						dosPeer.writeUTF(new ObjectMapper().writeValueAsString(downloadRequest));

					} catch (JsonProcessingException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
					System.out.println("Sent download Request to peer: " + downloadRequest.toString());
				}
			}

		} else if (response.getAction() == Action.GETLOAD) {
			System.out.println(
					"Received response for GETLOAD from: " + response.getSenderIp() + "_" + response.getSenderPort());

			String loadReqId = response.getReceiverIp() + ":" + response.getReceiverPort() + ":"
					+ response.getFileName();
			String senderNodeId = response.getSenderIp() + ":" + response.getSenderPort();

			// final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
			rwl.readLock().lock();

			if (reqIdNodeIdLoadMap.get(loadReqId) == null) {
				System.out.println("creating new map");
				rwl.readLock().unlock();

				HashMap<String, ArrayList<Integer>> nodeIdLoadMap = new HashMap<String, ArrayList<Integer>>();

				ArrayList<Integer> latency_FileContainingNodeSize = new ArrayList<Integer>();
				latency_FileContainingNodeSize.add(response.getLoad());
				latency_FileContainingNodeSize.add(response.getFileContainingNodeswithChecksum().size());

				nodeIdLoadMap.put(senderNodeId, latency_FileContainingNodeSize);

				ArrayList<Message> responseList = new ArrayList<Message>();
				responseList.add(response);

				rwl.writeLock().lock();
				reqIdNodeIdLoadMap.put(loadReqId, nodeIdLoadMap);
				reqIdTimeStamp.put(loadReqId, System.currentTimeMillis());
				reqIdGetLoadResponses.put(loadReqId, responseList);
				rwl.writeLock().unlock();

			} else {

				rwl.readLock().unlock();

				ArrayList<Integer> latency_FileContainingNodeSize = new ArrayList<Integer>();
				latency_FileContainingNodeSize.add(response.getLoad());
				latency_FileContainingNodeSize.add(response.getFileContainingNodeswithChecksum().size());

				rwl.readLock().lock();
				HashMap<String, ArrayList<Integer>> nodeIdLoadMap = reqIdNodeIdLoadMap.get(loadReqId);
				rwl.readLock().unlock();

				rwl.writeLock().lock();
				nodeIdLoadMap.put(senderNodeId, latency_FileContainingNodeSize);
				reqIdNodeIdLoadMap.put(loadReqId, nodeIdLoadMap);
				rwl.writeLock().unlock();

				rwl.readLock().lock();
				ArrayList<Message> updatedListResponses = reqIdGetLoadResponses.get(loadReqId);
				rwl.readLock().unlock();

				rwl.writeLock().lock();
				updatedListResponses.add(response);
				reqIdGetLoadResponses.put(loadReqId, updatedListResponses);

				rwl.writeLock().unlock();
			}

			rwl.readLock().lock();
			System.out.println("Actual response size = " + reqIdNodeIdLoadMap.get(loadReqId).size());
			if (reqIdNodeIdLoadMap.get(loadReqId)
					.size() == getExpectedNoOfResponses(reqIdNodeIdLoadMap.get(loadReqId))) {

				TreeMap<Integer, String> latencyNodeMap = evaluateBestPeer(reqIdNodeIdLoadMap.get(loadReqId),
						response.getReceiverPort(), configFilePath);
				rwl.readLock().unlock();
				sendDownloadRequest(latencyNodeMap, loadReqId, response);

			} else {
				rwl.readLock().unlock();
			}
		}
		return output;
	}

	private static void sendDownloadRequest(TreeMap<Integer, String> latencyNodeMap, String loadReqId,
			Message response) {

		boolean requestSucessful = false;
		while (!requestSucessful) {
			// Polling (get and remove) first entry so that next nodes will be used when
			// download fails
			String bestPeer = latencyNodeMap.pollFirstEntry().getValue();
			// final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
			rwl.writeLock().lock();
			reqIdNodeIdLoadMap.remove(loadReqId);
			reqIdTimeStamp.remove(loadReqId);
			reqIdGetLoadResponses.remove(loadReqId);
			rwl.writeLock().unlock();
			String receiverIp = bestPeer.split(":")[0];
			int receiverPort = Integer.valueOf(bestPeer.split(":")[1]);
			DataOutputStream dosPeer = null;
			Message downloadRequest = null;
			try {
				// Thread.sleep(10000); use this to test peer failure
				Socket peerSocket = new Socket(receiverIp, receiverPort);
				dosPeer = new DataOutputStream(peerSocket.getOutputStream());
				downloadRequest = createDownloadRequest(receiverIp, receiverPort, response, latencyNodeMap,
						response.getOutputMsg());
				dosPeer.writeUTF(new ObjectMapper().writeValueAsString(downloadRequest));
				requestSucessful = true;
				// peerSocket.close();
				System.out.println("Sent download Request to peer: " + downloadRequest.toString());
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			} catch (IOException e) {
				System.out.println("Sending download Request failed. Contacting next best peer");
			}
		}
	}

	private static int getExpectedNoOfResponses(HashMap<String, ArrayList<Integer>> nodeIdLatencyMap) {

		int min = Integer.MAX_VALUE;
		// final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
		rwl.readLock().lock();
		for (Entry<String, ArrayList<Integer>> entry : nodeIdLatencyMap.entrySet()) {
			if (entry.getValue().get(1) < min) {
				min = entry.getValue().get(1);
			}
		}
		rwl.readLock().unlock();
		System.out.println("Expected number of responses:" + min);
		return min;
	}

	private static Message createDownloadRequest(String receiverIp, int receiverPort, Message request,
			TreeMap<Integer, String> latencyNodeMap, String outputMsg) {

		Message dwnldReq = new Message();
		dwnldReq.setSenderIp(request.getReceiverIp());
		dwnldReq.setSenderPort(request.getReceiverPort());
		dwnldReq.setReceiverIp(receiverIp);
		dwnldReq.setReceiverPort(receiverPort);
		dwnldReq.setMessageType(MessageType.REQUEST);
		dwnldReq.setAction(Action.DOWNLOAD);
		dwnldReq.setFileName(request.getFileName());
		dwnldReq.setFileContainingNodeswithChecksum(request.getFileContainingNodeswithChecksum());
		dwnldReq.setLatencyNodeMap(latencyNodeMap);
		dwnldReq.setOutputMsg(outputMsg + "|" + "Send DOWNLOAD request to Peer: "+ dwnldReq.getReceiverIp()+"_"+ dwnldReq.getReceiverPort());
		dwnldReq.setStartTime(request.getStartTime());
		return dwnldReq;

	}

	private static boolean isCheckSumMatch(Message response) {
		File tempFile;
		FileOutputStream fos;
		try {
			tempFile = File.createTempFile(
					response.getReceiverIp() + "_" + response.getReceiverPort() + "_" + response.getFileName(), null,
					null);
			fos = new FileOutputStream(tempFile);
			fos.write(response.getFile());

			String sourceCheckSum = response.getFileContainingNodeswithChecksum()
					.get(response.getSenderIp() + ":" + response.getSenderPort());

			System.out.println("source checksum: " + sourceCheckSum);

			if (ChecksumGenerator.isCheckSumCorrect(tempFile, sourceCheckSum)) {
				tempFile.delete();
				return true;
			} else {
				System.out.println("Check sum of downloaded file did not match");
				return false;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	private static void saveFileAtPeer(String fileDir, String nodeip_port, String fileName, byte[] file) {

		File outputFile = new File(fileDir + fileName);
		FileOutputStream fos = null;

		try {
			fos = new FileOutputStream(outputFile);
			fos.write(file);

		} catch (FileNotFoundException e) {
			System.out.println("File not found" + e);
		} catch (IOException ioe) {
			System.out.println("Exception while writing file " + ioe);
		} finally {
			// close the stream
			try {
				if (fos != null) {
					fos.close();
				}
			} catch (IOException ioe) {
				System.out.println("Error while closing stream: " + ioe);
			}
		}
	}

	public static TreeMap<Integer, String> evaluateBestPeer(HashMap<String, ArrayList<Integer>> nodeIdLoadMap,
			int targetPort, String configFilePath) {

		String bestPeerId = null;
		System.out.println("# of Peers containing the file: " + nodeIdLoadMap.size());
		// TreeMap of latency and nodes to sort based on latency
		TreeMap<Integer, String> latencyNodeMap = new TreeMap<Integer, String>();

		for (Entry<String, ArrayList<Integer>> entry : nodeIdLoadMap.entrySet()) {

			String nodeIpPort = entry.getKey();

			int currPort = Integer.valueOf(nodeIpPort.split(":")[1]);
			int load = entry.getValue().get(0);
			int latencyForLoad = getLatencyForQueue(load);
			int latencyForNode = UtilityMethods.getLatencyBnNodes(configFilePath, currPort, targetPort);
			int latency = latencyForLoad + latencyForNode;
			latencyNodeMap.put(latency, nodeIpPort);
		}

		return latencyNodeMap;
	}

	/**
	 * gives latency of request as per the given size. adds up 5 ms latency for
	 * every item in the queue
	 * 
	 * @param qSize
	 * @return
	 */
	public static int getLatencyForQueue(int qSize) {
		if (qSize == 0) {
			return 0;
		} else {
			return qSize * 5;
		}
	}

//	private void processTimedOutRequests() {
//		Thread collateGetLoadRequests = new Thread() {
//			@Override
//			public void run() {
//				// if still exists in the node beyond a time limit
//				// start sending download request based on existing map
//				while (true) {
//					rwl.readLock().lock();
//
//					Set<Entry<String, Long>> entrySet = new HashSet<Entry<String, Long>>();
//					entrySet.addAll(reqIdTimeStamp.entrySet());
//
//					rwl.readLock().unlock();
//
//					for (Entry<String, Long> entry : entrySet) {
//						if (System.currentTimeMillis() - entry.getValue() > DOWNLOAD_REQUEST_TIME_OUT_THRESHOLD) {
//							rwl.writeLock().lock();
//							reqIdTimeStamp.remove(entry.getKey());
//							rwl.writeLock().unlock();
//							rwl.readLock().lock();
//							if (reqIdGetLoadResponses.get(entry.getKey()).size() > 0) {
//								Message response = reqIdGetLoadResponses.get(entry.getKey()).get(0);
//								TreeMap<Integer, String> latencyNodeMap = evaluateBestPeer(
//										reqIdNodeIdLoadMap.get(entry.getKey()), response.getReceiverPort(),
//										configFilePath);
//								rwl.readLock().unlock();
//								sendDownloadRequest(latencyNodeMap, entry.getKey(), response);
//							} else {
//								rwl.readLock().unlock();
//								System.out.println("Responses size is 0 for the get load request: " + entry.getKey());
//							}
//						}
//					} // for
//				} // while
//			}// run
//		};
//		collateGetLoadRequests.start();
//	}

}