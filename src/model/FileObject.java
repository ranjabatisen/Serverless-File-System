/**
 * 
 */
package model;

import java.util.List;

/**
 * @author nenem
 *
 */
public class FileObject {
	
	String filename;
	String checksum;
	List<String> nodesList;
	byte[] contents;
	
	
	public FileObject() {
		
		
	}
	
	public FileObject(String filename, String checksum) {
		this.filename = filename;
		this.checksum = checksum;
		this.contents = null;
		this.nodesList = null;
		
	}
	
	public FileObject(String filename, String checksum, List<String> nodesList ) {
		this.filename = filename;
		this.checksum = checksum;
		this.contents = null;
		this.nodesList = nodesList;
		
	}
	
	public FileObject(String filename, String checksum, byte[] contents ) {
		this.filename = filename;
		this.checksum = checksum;
		this.contents = contents;
		this.nodesList = null;
	}
	
	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public String getChecksum() {
		return checksum;
	}

	public void setChecksum(String checksum) {
		this.checksum = checksum;
	}

	public List<String> getNodesList() {
		return nodesList;
	}

	public void setNodesList(List<String> nodesList) {
		this.nodesList = nodesList;
	}

	public byte[] getContents() {
		return contents;
	}

	public void setContents(byte[] contents) {
		this.contents = contents;
	}

}
