package com.codenvy.example.swing;

import java.io.*;
import java.net.*;
import java.util.Arrays;

public class PeerConnection {
    String IP, id, peerID;
	byte[] infoHash;
    int port;
    private final byte PSTRLEN = (byte) 19;
    private final String PSTR = "BitTorrent protocol";
    
    public PeerConnection(String IP, int port, String id, byte[] infoHash, String peerID) {
        this.IP = IP;
        this.port = port;
        this.id = id;
        this.infoHash = infoHash;
        this.peerID = peerID;
        
        try {
            this.handshake();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }
    
    
    public void handshake() throws IOException{      
    	System.out.println("attempting to open socket");
        Socket clientSocket = new Socket(IP, port);
        
        System.out.println("connected");
        
        DataOutputStream os = new DataOutputStream(clientSocket.getOutputStream());
        DataInputStream is = new DataInputStream(clientSocket.getInputStream());
       
        os.writeByte(PSTRLEN);
        os.write(PSTR.getBytes());
        os.write(new byte[8]);
        os.write(infoHash);
        os.write(peerID.getBytes());

    	while (is.available() == 0) {
    		//wait for a response
    	}
    	
        try {
        	while (is.available() > 0) {
        		int inPSTRLEN = is.readByte();
        		if (inPSTRLEN > 0) {
	                byte[] inPSTR = new byte[inPSTRLEN];
	                is.readFully(inPSTR);
	                byte[] reserved = new byte[8];
	                is.readFully(reserved);
	                byte[] infoHashin = new byte[20];
	                is.readFully(infoHashin);
	                byte[] peerIdin = new byte[20];
	                is.readFully(peerIdin);
	                if (Arrays.equals(infoHashin, infoHash)) {
	                	
	                } else {
	                	is.close();
	                	os.close();
	                	clientSocket.close();
	                }
        		}
        	}
        } 
        catch (EOFException e) {
        	e.printStackTrace();
        	is.close();
        	os.close();
        	clientSocket.close();
        }
        is.close();
        os.close();
        clientSocket.close();
    }
}
