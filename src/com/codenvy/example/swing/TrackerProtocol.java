package com.codenvy.example.swing;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import com.hypirion.bencode.*;

/**
 * TrackerProtocol sends the initial request to the tracker to get the list of peers for this torrent and parses the response.
 * Builds the files once they have been fully downloaded
 */
public class TrackerProtocol {
	private String announceKey, infoHashURL; 
	private byte[] infoHash;
	private final String PEER_ID = "ST00001SKETCHTORRENT";
	private final String A = "&"; //separators for URL
	private final String Q = "?";
	private final int PORT = 9876; //arbitrary/randomly chosen port
	private long fileSize;
	private long downloaded = 0;
	private long uploaded = 0;
	private long remaining; 
	private String event = "started";
	private int[] pieceReference; //0 unrequested, 1 requested - used to determine if a 'piece' of the file has been requested from a peer or needs to be
	private long pieceLength; //the length of each 'piece' in the file
	private String downloadPath = "";
	private ArrayList<HashMap<String,Object>> files;

	/**
	 * ImportData gets the necessary values from the metainfo/torrent file
	 * @param m MetaInfo object referencing the relevant torrent file
	 */
	private void importData(MetaInfo m) {
		announceKey = (String) m.getValues('a');
		fileSize = (long) m.getValues('f');
		infoHashURL = (String) m.getValues ('g');
		infoHash = (byte[]) m.getValues('h');
		pieceLength = (long) m.getValues('d');
		remaining = fileSize;
		pieceReference = new int[(int)(fileSize/pieceLength)];
		files = (ArrayList<HashMap<String,Object>>) m.getValues('e');
	}

	/**
	 * @return announce url as a string to send the get request to the tracker
	 */
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

	/**
	 * Sends the 'announce' get request to the tracker to get the list of peers for this torrent
	 * @param m MetaInfo object referencing the relevant torrent file
	 * @return the response from the tracker
	 */
	public byte[] sendGet(MetaInfo m) {
		importData(m); //get the necessary values from the metainfo/torrent file
		String url = generateURL(); //generate the 'announce' request url to the tracker
		URL obj = null;
		try {
			obj = new URL(url);
		}
		catch (MalformedURLException e) {
			e.printStackTrace();
		}
		HttpURLConnection con = null;
		try {
			con = (HttpURLConnection) obj.openConnection(); //connect to the url
		}
		catch (IOException e){
			e.printStackTrace();
		}

		// optional default is GET
		try {
			con.setRequestMethod("GET"); //send the get request
		}
		catch (ProtocolException e){
			e.printStackTrace();
		}

		//add request header
		//con.setRequestProperty("User-Agent", USER_AGENT);
		int responseCode = 212321; //how we know if it failed
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
		//get the response from the server
		try {
			 in = new DataInputStream(con.getInputStream());
			 response = new byte[in.available()];
			 in.readFully(response);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return response;
	}
	
	/**
	 * Parses the BEncoded response from the tracker
	 * @param response The byte array response from the tracker
	 * @return Hashmap containing the decoded response from the tracker
	 * @throws BencodeReadException
	 */
	@SuppressWarnings("unchecked")
	public HashMap<String, Object> parseResponse(byte[] response) throws BencodeReadException{
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
    
	/**
	 * @return info hash as a byte array
	 */
	public byte[] getInfoHash() {
	    return infoHash;
	}
	/**
	 * @return the client's peer id
	 */
	public String getPEER_ID() {
	    return PEER_ID;
	}
	/**
	 * @return the total size of all files
	 */
	public long getFileSize() {
	    return fileSize;
	}
	/**
	 * @return the size of each 'piece' of the file
	 */
	public long getPieceLength() {
	    return pieceLength;
	}
	/**
	 * @return the piece reference array used to determine pieces to request
	 */
	private int[] getPieceReference() {
		return pieceReference;
	}
	
	/**
	 * Builds the file system and constructs each file based on byte offsets in the temp file
	 * @param tmpFile the temp file holding the byte array of each file in sequence
	 * @param files the arraylist of files and file systems
	 * @param downloadPath the file path to build the downloaded filesystem in
	 * @throws IOException
	 */
    public void buildFiles(File tmpFile, ArrayList<HashMap<String,Object>> files, String downloadPath) throws IOException{
        File f = new File(files.get(0).get("name").toString());
        FileOutputStream fos = new FileOutputStream(f);
        DataInputStream in = new DataInputStream(new FileInputStream(tmpFile));
        for (int i = 0; i < files.size(); i++) {
            byte[] tempByteArray = new byte[(int) files.get(i).get("length")];
            if (files.size() == 1) {
                f = new File(files.get(0).get("name").toString());
                in.read(tempByteArray);
            }
            else {
                f = new File(files.get(i).get("path").toString());
                if (i == 0) {
                    in.read(tempByteArray, 0, (int) files.get(i).get("length"));
                }
                else {
                    in.read(tempByteArray, (int) files.get(i-1).get("length"), (int) files.get(i).get("length"));
                }
            }
            checkParent(f);
            f.createNewFile();
            fos.write(tempByteArray);
        }
        in.close();
        fos.close();
        return;
    }
    
    /**
     * Creates the file system to download the files to
     * @param file the file to check if the parent exists
     */
    private void checkParent(File file) {
        while (file.getParentFile() != null) {
              checkParent(file.getParentFile());
              file.getParentFile().mkdir();
        }
    }

	public static void main(String args[]) throws FileNotFoundException{
		MetaInfo m = new MetaInfo("Tester1.torrent"); //instantiate the metainfo for this torrent
		TrackerProtocol tp = new TrackerProtocol(); //instantiate a trackerprotocol to manage this torrent
		HashMap<String, Object> j = null;
		while (j==null) {
			try {
				j = tp.parseResponse(tp.sendGet(m)); //send the announce and parse the response from the tracker
			}catch (BencodeReadException e) {
				System.out.println("failed to parse response");
			}
		}
		m = null; //garbage collection
		ArrayList<PeerConnection> connections = new ArrayList<PeerConnection>(); //an arraylist to hold the connection objects to each peer
		if (j.get("peers") instanceof List) {
			ArrayList<HashMap<String, Object>> peerList = (ArrayList<HashMap<String, Object>>) j.get("peers"); //store the list of peers from the trackers response			
			for (HashMap<String, Object> peer : peerList) { //try to connect to each peer
				System.out.println("ip: " + peer.get("ip")); 
				System.out.println("port: " + (int)(long)peer.get("port"));
				connections.add(new PeerConnection((String) peer.get("ip"),(int)(long)peer.get("port"), (String) peer.get("peer id"), tp.getInfoHash(), tp.getPEER_ID(), tp.getPieceReference(), (int)tp.getPieceLength()));
				connections.get(connections.size()-1).start(); //start the thread to connect to the peer and communcate with them
				System.out.println("======================");
			}
			for (int i = 0; i < connections.size(); i++) { //if any peers didnt connect remove them from the list of connections
			    if (connections.get(i).getConnectionStatus() == false) {
			        connections.remove(i);
			    }
			}
			if (connections.size() > 10) { //limit the number of connections to 10 to reduce ram and cpu load
			    while (connections.size() != 10) {
			        connections.remove(connections.size()-1);
			    } 
			}
			
			File tempFile; //for holding data received before the whole file is downloaded, to reduce load on ram (and in case the file is larger than ram)
			FileOutputStream fos;
			int lastPiece = 0; //the last piece offset written to the temp file, so the data is written in order
			HashMap<Integer,byte[]> pieces = new HashMap<Integer,byte[]>(); // key: index/offset of the byte array in the whole file, val: piece of the file as a byte array
			try {
			     tempFile = File.createTempFile("torrent", null);
			     fos = new FileOutputStream(tempFile);
				 while(true) {
				     Thread.sleep(200); //check for finished pieces every 200ms
			             for (PeerConnection pc : connections) {
			                     if (pc.getPieceStatus()) { //if the connection has a full peice
			                         pieces.putAll(pc.getPieces()); //copy it be written to temp
			                         pc.clear(); // clear the connections data to save ram
			                     }
			             }
			             if (pieces.containsKey(lastPiece)) { //if the piece we need to write is completed and saved
			                 fos.write(pieces.get(lastPiece), (int) tp.getPieceLength() * lastPiece,  (int) tp.getPieceLength()); //write it to the temp file
                             lastPiece++; 
                             fos.flush(); 
                             pieces.remove(lastPiece); //clear data to save ram
			             }
			             if (tp.getPieceLength() * lastPiece >= tp.getFileSize()) break; //break the loop once the file is done
		         }

			}
			catch (IOException e) {
			    e.printStackTrace();
			}
			catch (InterruptedException e) {
			    e.printStackTrace();
			}
						
		}
}


}


