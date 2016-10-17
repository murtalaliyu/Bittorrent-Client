/*
Authors
Murtala Aliyu
Anrew Marshall
*/

import java.net.Socket;
import java.net.ServerSocket;
import java.io.*;
import java.nio.file.*;
import GivenTools.*;
import java.net.*;
import java.util.*;
import java.nio.ByteBuffer;
import java.lang.*;

public class RUBTClient{
	public static void main(String[] args) throws Exception {

		//return error message if torrent file and file name arguments aren't entered
		if (args.length != 2) {
			System.err.println("\nUsage error: java RUBTClient <.torrent file> <.mov file>\n");
			return;
		}

		//get torrent file path and print it
		Path filePath = Paths.get("/Users/Murtala/Desktop/IT-Projects-Fall16/IT/Project_Phase_1/GivenTools/CS352_Exam_Solutions.mp4.torrent");
		//System.out.println("\nTorrent file path is: " + filePath + "\n");

		//open torrent path and parse it
		byte[] byteFilePathArray = Files.readAllBytes(filePath);

		//decode data using TorretInfo.java
		TorrentInfo decodedTorrentByteFile = new TorrentInfo(byteFilePathArray);
		//System.out.println("Decoded torrent byte file: " + decodedTorrentByteFile + "\n");

		//get tracker url
		URL url = decodedTorrentByteFile.announce_url;
		String urlString = url.toString();
		urlString += "?";

		//get infoHash in the form of ByteBuffer and convert to hex string
		ByteBuffer infoHash = decodedTorrentByteFile.info_hash;
		String hex = byteBufferToHexString(infoHash);

		//escape string
		String hexString = escapeStr(hex);

		//**************generate random peer id*********************
		String peerId = "%25%85%04%26%23%e3%32%0d%f2%90%e2%51%f6%15%92%2f%d9%b0%ef%a9";

		//assemble final url
		urlString += "info_hash=";
		urlString += hexString;
		urlString += "&peer_id=";
		urlString += peerId;
		urlString += "&port=6882&uploaded=0&downloaded=0&left=";
		urlString += decodedTorrentByteFile.file_length;
		urlString += "&event=started";

		//send HTTP get request to tracker
  		HttpURLConnection connect = (HttpURLConnection) new URL(urlString).openConnection();
 		DataInputStream input = new DataInputStream(connect.getInputStream());
 
 		int size = connect.getContentLength();
 		byte[] encodedTrackerResponse = new byte[size];
 
  		input.readFully(encodedTrackerResponse);
  		input.close();

		//get list of peers from tracker response
 		Object o = null;
 		o = Bencoder2.decode(encodedTrackerResponse);
 		//System.out.println(o);
 		Map<ByteBuffer, Object> response = (HashMap<ByteBuffer, Object>) o;
 
 		//print response
 		ToolKit.printMap(response, 0);

		//open socket connection to peers
 		ArrayList<Peer> peers = getListOfPeers(encodedTrackerResponse);
 		System.out.println(peers);

	}

	//implement byteBuffer to hex string
	public static String byteBufferToHexString(ByteBuffer byteBuffer) {
		byte[] b = new byte[byteBuffer.remaining()];
		byteBuffer.get(b);
		
		final char[] hexArray = "0123456789ABCDEF".toCharArray();
		char[] hexChars = new char[b.length * 2];
		for ( int j = 0; j < b.length; j++ ) {
			int v = b[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		String hex = new String(hexChars);
		return hex;
	}

	//implement byteBufferToString 
	public static String byteBufferToString(ByteBuffer myByteBuffer) {
	
		if (myByteBuffer.hasArray()) {
	    	return new String(myByteBuffer.array(),
	        myByteBuffer.arrayOffset() + myByteBuffer.position(),
	        myByteBuffer.remaining());
		} else {
		    final byte[] b = new byte[myByteBuffer.remaining()];
		    myByteBuffer.duplicate().get(b);
		    return new String(b);
		}
	}

	//pertaining to peer list
	public final static ByteBuffer PEER_KEY = ByteBuffer.wrap(new byte[] {'p', 'e', 'e', 'r', 's'});
	public final static ByteBuffer PEER_ID = ByteBuffer.wrap(new byte[] {'p', 'e', 'e', 'r', ' ', 'i', 'd'});
	public final static ByteBuffer PEER_IP = ByteBuffer.wrap(new byte[] {'i', 'p'});
	public final static ByteBuffer PEER_PORT = ByteBuffer.wrap(new byte[] {'p', 'o', 'r', 't'});

	//get list of peers from tracker response
	public static ArrayList<Peer> getListOfPeers(byte[] encodedTrackerResponse) throws BencodingException {
		Object o = Bencoder2.decode(encodedTrackerResponse);

		HashMap<ByteBuffer, Object> response = (HashMap<ByteBuffer, Object>) o;

		ArrayList peerResponse = (ArrayList) response.get(PEER_KEY);
		ArrayList<Peer> peerList = new ArrayList<Peer>();

		for (int i = 0; i < peerResponse.size(); i++) {
			HashMap tmp = (HashMap) peerResponse.get(i);
			String name = null, ip = null;

			name = byteBufferToString((ByteBuffer) tmp.get(PEER_ID));
			ip = byteBufferToString((ByteBuffer) tmp.get(PEER_IP));

			int port = (int) tmp.get(PEER_PORT);

			//if (name.contains("RU")) {
				Peer peer = new Peer(name, port, ip);
				peerList.add(peer);
			//}
		}

		return peerList;
	}
	
	//escape given string
	public static String escapeStr(String hex){
		String percent = "%", hexString = "";
		hexString += percent;
		int a = 0, i = 0;
		while (i < 19) {
			hexString += hex.substring(a, a+2);
			hexString += percent;
			a += 2;
			i++;
		}
		hexString += hex.substring(38,40);

		return hexString;
	}
}

//peer class
class Peer{
		
	public String name;
	public int port;
	public String ip;

	public Peer(String name, int port, String ip) {
		this.name = name;
		this.port = port;
		this.ip = ip;
	}

	public String toString() {
		String returnStr = "Peer: " + name + "Port: " + port + "IP: " + ip;
		return returnStr;
	}
}