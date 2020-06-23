package communication;
/**
 * Handles communication between node and clients
 * @author vcroopana
 *
 */
public class NodeClientCommunication {
	
	public void getFile() {

	}
	public static void main(String args[]) {
		
		String configFilePath = System.getProperty("user.dir") + "/config.properties";
		PeerNode node1 = new PeerNode("localhost", 5051, System.getProperty("user.dir") +"/src/dirs/", configFilePath, 1234);
		node1.updateFileList();
		node1.startServer();
		node1.getFile("nene.txt");

//		Thread t = new Thread() {
//			@Override
//			public void run(){
//				while(true) {
//					System.out.println("Entre filename:");
//
//				}
//			});
//		t.start();
		
	
	}
}