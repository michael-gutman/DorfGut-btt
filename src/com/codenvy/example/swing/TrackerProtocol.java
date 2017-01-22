package com.codenvy.example.swing;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.hypirion.bencode.BencodeReadException;
import com.hypirion.bencode.BencodeReader;

public class TrackerProtocol {
	private String announceKey, infoHashURL;
	private byte[] infoHash;
	private final String PEER_ID = "ST00001SKETCHTORRENT";
	private final String A = "&";
	private final String Q = "?";
	private final int PORT = 9876;
	private long fileSize;
	private long downloaded = 0;
	private long uploaded = 0;
	private long remaining;
	private String event = "started";


	private void importData(MetaInfo m) {
		announceKey = (String) m.getValues('a');
		fileSize = (long) m.getValues('f');
		infoHashURL = (String) m.getValues ('g');
		infoHash = (byte[]) m.getValues('h');
		remaining = fileSize;
	}

	private String generateURL() {
		return announceKey + 
				Q + "info_hash=" + infoHashURL 
				+ A + "peer_id=" + PEER_ID 
				+ A + "port=" + PORT 
				+ A + "uploaded=" + uploaded 
				+ A + "downloaded=" + downloaded 
				+ A + "left=" + remaining 
				+ A + "compact=0" 
				+ A +"no_peer_id=0" 
				+ A + "event=" + event 
				+ A + "key=" + PEER_ID;
	}

	public byte[] sendGet(MetaInfo m) {
		importData(m);
		String url = generateURL();
		URL obj = null;
		try {
			obj = new URL(url);
		}
		catch (MalformedURLException e) {
			e.printStackTrace();
		}
		HttpURLConnection con = null;
		try {
			con = (HttpURLConnection) obj.openConnection();
		}
		catch (IOException e){
			e.printStackTrace();
		}

		// optional default is GET
		try {
			con.setRequestMethod("GET");
		}
		catch (ProtocolException e){
			e.printStackTrace();
		}

		//add request header
		//con.setRequestProperty("User-Agent", USER_AGENT);
		int responseCode = 212321;
		try {
			responseCode = con.getResponseCode();
		}
		catch (IOException e){
			e.printStackTrace();
		}

		System.out.println("\nSending 'GET' request to URL : " + url);
		System.out.println("Response Code : " + responseCode);
		DataInputStream in = null;
		byte[] response = null;
		try {
			 in = new DataInputStream(con.getInputStream());
			 response = new byte[in.available()];
			 in.readFully(response);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return response;
	}

	@SuppressWarnings("unchecked")
	private HashMap<String, Object> parseResponse(byte[] response) throws BencodeReadException{
		ByteArrayInputStream bais = new ByteArrayInputStream(response);
		BencodeReader br = new BencodeReader(bais);
		HashMap<String, Object> j = null;
		try {
			j = (HashMap<String, Object>) br.read();
			br.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return j;
	}


	public static void main(String args[]) throws FileNotFoundException{
		MetaInfo m = new MetaInfo("Tester1.torrent");
		//MetaInfo m = new MetaInfo("Tester2.torrent");
		TrackerProtocol tp = new TrackerProtocol();
		HashMap<String, Object> j = null;
		while (j==null) {
			try {
				j = tp.parseResponse(tp.sendGet(m));
			}catch (BencodeReadException e) {
				System.out.println("failed to parse response");
			}
		}
		ArrayList<PeerConnection> connections = new ArrayList<PeerConnection>();
		if (j.get("peers") instanceof List) {
			ArrayList<HashMap<String, Object>> peerList = (ArrayList<HashMap<String, Object>>) j.get("peers");
			//HashMap<String, Object> peer = peerList.get(0);
			//System.out.println("ip: " + peer.get("ip"));
			//System.out.println("port: " + (int)(long)peer.get("port"));
			//PeerConnection one = new PeerConnection((String) peer.get("ip"),(int)(long)peer.get("port"), (String) peer.get("peer id"), tp.infoHash, tp.PEER_ID);
			
			for (HashMap<String, Object> peer : peerList) {
				System.out.println("ip: " + peer.get("ip"));
				System.out.println("port: " + (int)(long)peer.get("port"));
				try {
				connections.add(new PeerConnection((String) peer.get("ip"),(int)(long)peer.get("port"), (String) peer.get("peer id"), tp.infoHash, tp.PEER_ID));
				} catch (Exception e) {
					System.out.println(e.getMessage());
				}
				System.out.println("======================");
			}
		}
	}

}
