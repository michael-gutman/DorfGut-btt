package com.codenvy.example.swing;

import java.io.*;
import java.net.*;

public class PeerConnection {
    String IP, id, infoHash, peerID;
    int port;
    private final byte PSTRLEN = (byte) 19;
    private final String PSTR = "BitTorrent protocol";
    
    public PeerConnection(String IP, int port, String id, String infoHash, String peerID) {
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
        //BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
    	System.out.println("attempting to open socket");
    	
        Socket clientSocket = new Socket(IP, port);
        
        System.out.println("connected");
        
        DataOutputStream os = new DataOutputStream(clientSocket.getOutputStream());
        DataInputStream is = new DataInputStream(clientSocket.getInputStream());
        
        os.writeByte(PSTRLEN);
        os.write(PSTR.getBytes());
        os.write(new byte[8]);
        os.write(infoHash.getBytes());
        os.write(peerID.getBytes());
        //System.out.println(is.read());
        //serverHandshakeMessage = inFromServer.readLine();
        //System.out.println("FROM SERVER: " + serverHandshakeMessage);
//        int inPSTRLEN = is.readByte();
//        byte[] inPSTR = new byte[inPSTRLEN];
//        is.readFully(inPSTR);
//        byte[] reserved = new byte[8];
//        is.readFully(reserved);
//        byte[] infoHash = new byte[20];
//        is.readFully(infoHash);
//        byte[] peerId = new byte[20];
//        is.readFully(peerId);
        byte temp[] = new byte[68];
        try {
        	while (true) {
        		System.out.println("reading...");
        		is.readFully(temp);
        		System.out.println(temp.toString());
        	}
        } catch(EOFException e) {
        	System.out.println("end of file");
        	clientSocket.close();
        }
        clientSocket.close();
    }
}
