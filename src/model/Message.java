package model;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Message {
	@JsonProperty("senderIp")
	private String senderIp;
	@JsonProperty("receiverIp")
	private String receiverIp;
	@JsonProperty("clientIp")
	private String clientIp;
	@JsonProperty("senderPort")
	private int senderPort;
	@JsonProperty("receiverPort")
	private int receiverPort;
	@JsonProperty("clientPort")
	private int clientPort;
	@JsonProperty("messageType")
	private MessageType messageType;
	@JsonProperty("action")
	private Action action;
	@JsonProperty("fileName")
	private String fileName;
	@JsonProperty("file")
	private byte[] file;
	@JsonProperty("load")
	private int load;
	@JsonProperty("fileContainingNodeswithChecksum")
	private Map<String, String> fileContainingNodeswithChecksum;
	@JsonProperty("listofFilesinNode")
	private List<String> listofFilesinNode;
	@JsonProperty("mapofFilesinNodeWithChecksum")
	private Map<String, String> mapofFilesinNodeWithChecksum;
	@JsonProperty("fileObjectList")
	private List<FileObject> fileObjList;
	@JsonProperty("latencyNodeMap")
	private TreeMap<Integer, String> latencyNodeMap;
	@JsonProperty("outputMsg")
	private String outputMsg;
	@JsonProperty("startTime")
	private long startTime;

	
	@JsonProperty("senderIp")
	public String getSenderIp() {
		return senderIp;
	}

	@JsonProperty("senderIp")
	public void setSenderIp(String senderIp) {
		this.senderIp = senderIp;
	}

	@JsonProperty("receiverIp")
	public String getReceiverIp() {
		return receiverIp;
	}

	@JsonProperty("receiverIp")
	public void setReceiverIp(String receiverIp) {
		this.receiverIp = receiverIp;
	}

	@JsonProperty("clientIp")
	public String getClientIp() {
		return clientIp;
	}

	@JsonProperty("clientIp")
	public void setClientIp(String clientIp) {
		this.clientIp = clientIp;
	}

	@JsonProperty("senderPort")
	public int getSenderPort() {
		return senderPort;
	}

	@JsonProperty("senderPort")
	public void setSenderPort(int senderPort) {
		this.senderPort = senderPort;
	}

	@JsonProperty("receiverPort")
	public int getReceiverPort() {
		return receiverPort;
	}

	@JsonProperty("receiverPort")
	public void setReceiverPort(int receiverPort) {
		this.receiverPort = receiverPort;
	}

	@JsonProperty("clientPort")
	public int getClientPort() {
		return clientPort;
	}

	@JsonProperty("clientPort")
	public void setClientPort(int clientPort) {
		this.clientPort = clientPort;
	}

	@JsonProperty("messageType")
	public MessageType getMessageType() {
		return messageType;
	}

	@JsonProperty("messageType")
	public void setMessageType(MessageType messageType) {
		this.messageType = messageType;
	}

	@JsonProperty("action")
	public Action getAction() {
		return action;
	}

	@JsonProperty("action")
	public void setAction(Action action) {
		this.action = action;
	}

	@JsonProperty("fileName")
	public String getFileName() {
		return fileName;
	}

	@JsonProperty("fileName")
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	@JsonProperty("file")
	public byte[] getFile() {
		return file;
	}

	@JsonProperty("file")
	public void setFile(byte[] file) {
		this.file = file;
	}

	@JsonProperty("load")
	public int getLoad() {
		return load;
	}

	@JsonProperty("load")
	public void setLoad(int load) {
		this.load = load;
	}

	@JsonProperty("fileContainingNodeswithChecksum")
	public Map<String, String> getFileContainingNodeswithChecksum() {
		return fileContainingNodeswithChecksum;
	}

	@JsonProperty("fileContainingNodeswithChecksum")
	public void setFileContainingNodeswithChecksum(Map<String, String> fileContainingNodeswithChecksum) {
		this.fileContainingNodeswithChecksum = fileContainingNodeswithChecksum;
	}

	@JsonProperty("listofFilesinNode")
	public List<String> getlistofFilesinNode() {
		return listofFilesinNode;
	}

	@JsonProperty("listofFilesinNode")
	public void setlistofFilesinNode(List<String> listofFilesinNode) {
		this.listofFilesinNode = listofFilesinNode;
	}

	@JsonProperty("mapofFilesinNodeWithChecksum")
	public Map<String, String> getmapofFilesinNodeWithChecksum() {
		return mapofFilesinNodeWithChecksum;
	}

	@JsonProperty("mapofFilesinNodeWithChecksum")
	public void setmapofFilesinNodeWithChecksum(Map<String, String> mapofFilesinNodeWithChecksum) {
		this.mapofFilesinNodeWithChecksum = mapofFilesinNodeWithChecksum;
	}

	@JsonProperty("fileObjectList")
	public List<FileObject> getFileObjList() {
		return this.fileObjList;
	}

	@JsonProperty("fileObjectList")
	public void setFileObjList(List<FileObject> fileObj) {
		this.fileObjList = fileObj;
	}

	@JsonProperty("latencyNodeMap")
	public TreeMap<Integer, String> getLatencyNodeMap() {
		return latencyNodeMap;
	}

	@JsonProperty("latencyNodeMap")
	public void setLatencyNodeMap(TreeMap<Integer, String> latencyNodeMap) {
		this.latencyNodeMap = latencyNodeMap;
	}

	@JsonProperty("outputMsg")
	public String getOutputMsg() {
		return outputMsg;
	}

	@JsonProperty("outputMsg")
	public void setOutputMsg(String outputMsg) {
		this.outputMsg = outputMsg;
	}
	@JsonProperty("startTime")
	public long getStartTime() {
		return startTime;
	}
	
	@JsonProperty("startTime")
	public void setStartTime(long l) {
		this.startTime = l;
	}
	
	
	@Override
	public String toString() {

		StringBuilder sb = new StringBuilder();

		sb.append("messageType: " + this.getMessageType());
		sb.append(" action: " + this.getAction());
		sb.append(" fileName: " + this.getFileName());
		sb.append(" senderIp: " + this.getSenderIp());
		sb.append(" senderPort: " + String.valueOf(this.getSenderPort()));
		sb.append(" receiverIp: " + this.getReceiverIp());
		sb.append(" receiverPort: " + String.valueOf(this.getReceiverPort()));
		// sb.append("fileSize: " + file == null ? 0 : file.length);
		sb.append(" load: " + this.getLoad());
		sb.append(" fileContainingNodeswithChecksum: " + this.getFileContainingNodeswithChecksum());
		sb.append(" outputMsg:" + outputMsg);
		
		return sb.toString();

	}
}