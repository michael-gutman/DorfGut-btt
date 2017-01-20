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
        String serverHandshakeMessage;
        
        //BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        Socket clientSocket = new Socket(IP, port);
        
        DataOutputStream os = new DataOutputStream(clientSocket.getOutputStream());
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        os.writeByte(PSTRLEN);
        os.write(PSTR.getBytes());
        os.write(new byte[8]);
        os.write(infoHash.getBytes());
        os.write(peerID.getBytes());
        serverHandshakeMessage = inFromServer.readLine();
        System.out.println("FROM SERVER: " + serverHandshakeMessage);
        clientSocket.close();
    }
}