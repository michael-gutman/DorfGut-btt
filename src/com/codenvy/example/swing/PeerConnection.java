package com.codenvy.example.swing;

import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Peer Connection class handles connection to and communicating with peers on a seperate thread
 */
public class PeerConnection implements Runnable {
    private Thread       t;
    private String       threadName;
    String               IP, id, peerID;
    byte[]               infoHash;
    int                  port;
    private final byte   PSTRLEN        = (byte)19; 
    private final String PSTR           = "BitTorrent protocol";
    private int          peerStatus; // 1 available, 2 chocked
    private StringBuffer bitfieldBinary = new StringBuffer();
    private int[]        peiceReference; //0 available, 1 requested
    private int          pieceLength; //the length of each 'piece' of the files to request
    private int          chunk          = 0; //the current 'chunk' (offset) of the piece being reuqested
    private int          chunks; // total number of chunks
    private int          index = 0; //current piece (offset of whole file) being requested
    private static int   chunkSize      = 16384; //size of each 'chunk' 16kB
    private HashMap<Integer,byte[]> pieces; //key: index (offset) of the piece in the file, val: byte array of the piece
    private boolean pieceFinished = false; //whether this peerConnection is holding a completed piece
    private boolean connectionStatus = true; //whether a connection was established to the peer (Default true, changed if failed)

    /**
     * Instantiates a PeerConnection object, which can be run on a new thread to attempt a connection to a peer and automatically begin sending and parsing messages
     * @param IP String ip of the peer to attempt a connection to
     * @param port int port of the peer to attempt a connection to
     * @param id the peer's id 
     * @param infoHash byte array info hash of the torrent being requested
     * @param peerID String peerID the client's peer ID
     * @param pieceReference Integer array representing pieces requested or not
     * @param pieceLength int piecelength the total length of each piece in the torrent
     */
    public PeerConnection(String IP, int port, String id, byte[] infoHash, String peerID, int[] pieceReference, int pieceLength) {
        this.IP = IP;
        this.port = port;
        this.id = id;
        this.infoHash = infoHash;
        this.peerID = peerID;
        threadName = id;
        this.peiceReference = pieceReference; //0 available, 1 requested
        this.pieceLength = pieceLength;
        pieces = new HashMap<Integer,byte[]>();
        chunks = (int)Math.ceil((double)pieceLength / chunkSize);
    }
    /**
     * Stop the thread
     */
    public void stop() {
        t = null;
    } 
    /**
     * Automatically called when the thread starts, attempts to connect and send the handshake message
     * then parses have and bitfield messages and requestes and receives pieces
     */
    public void run() {
        try {
            this.handshake();
        } catch (IOException e) {
            e.printStackTrace();
            connectionStatus = false;
            t.stop();
        }
    }
    /**
     * Starts the thread
     */
    public void start() {
        System.out.println("Starting " + threadName);
        if (t == null) {
            t = new Thread(this, threadName);
            t.start();
        }
    }
    
    /**
     * @return peerStatus where the peer has us chocked or not
     */
    public int checkPeerStatus() {
        return peerStatus;
    }

    /**
     * @return bitfield string representing the pieces this peer can serve
     */
    public String getAvailablePieces() {
        return bitfieldBinary.toString();
    }

    /**
     * The main PeerConnection process. Connects to the peer and sends and receives handshake messages. Parses messages from the peer and decides when to request peices
     * @throws IOException
     */
    public void handshake() throws IOException {
        bitfieldBinary.setLength(0);
        //System.out.println("attempting to open socket");
        Socket clientSocket = new Socket(IP, port);

        //System.out.println("connected");

        DataOutputStream os = new DataOutputStream(clientSocket.getOutputStream());
        DataInputStream is = new DataInputStream(clientSocket.getInputStream());

        //send the 'handshake' message announcing ourselves to the peer
        os.writeByte(PSTRLEN);
        os.write(PSTR.getBytes());
        os.write(new byte[8]);
        os.write(infoHash);
        os.write(peerID.getBytes());
        os.flush();
        while (is.available() == 0) {
            //wait for a response
        }

        try {
            while (is.available() > 0) {
                int inPSTRLEN = is.readByte(); //parse the peers handshake
                if (inPSTRLEN > 0) {
                    byte[] inPSTR = new byte[inPSTRLEN];
                    is.readFully(inPSTR);
                    byte[] reserved = new byte[8];
                    is.readFully(reserved);
                    byte[] infoHashin = new byte[20];
                    is.readFully(infoHashin);
                    byte[] peerIdin = new byte[20];
                    is.readFully(peerIdin);
                    if (Arrays.equals(infoHashin, infoHash)) { //if the hashes match, we are 'talking' about the same torrent
                        os.write(new byte[]{0, 0, 0, 1, 2}); //tell the peer we are 'interested' in pieces they have to offer
                        os.flush();
                        try {
                            while (true) {
                            	//if the peer is not sending messages, does not have us choked, we have not already downloaded a piece AND the peer has told us their available pieces
                                if (is.available() == 0 && peerStatus == 1 && !pieceFinished && bitfieldBinary.length() > 0) {
                                    request(os); //request a piece
                                }
                                while (is.available() == 0) {
                                    //wait for a message from the peer
                                }
                                if (is.available() != 0) { //if the peer has sent a message
                                    int length = is.readInt(); //get the 4 byte int length of the message
                                    int responseID = is.readByte(); //get the 1 byte ID type of message
                                    if (responseID == 1) { 
                                        //the peer has 'unchoked' us meaning they will serve us pieces of files
                                        peerStatus = 1;
                                    } else if (responseID == 0) {
                                        //the peer has 'choked' us meaning they will ignore our piece requests
                                        peerStatus = 2;
                                    } else if (responseID == 4) {
                                        //the peer is telling us they have a piece of the files
                                        int index = is.readInt(); //index of the piece the peer is telling us they have available
                                        if (index > bitfieldBinary.length()) //if our current bitfield representation for this peer is too small make space for this peice
                                            bitfieldBinary.setLength(index + 1);
                                        bitfieldBinary.setCharAt(index, '1');
                                        //System.out.println(id + ": added " + index + " to bitfield");
                                        //System.out.println(id + ": " + bitfieldBinary.toString());
                                    } else if (responseID == 5) {
                                        //the peer is sending a (possibly incomplete/'lazy') bitfield representing the pieces of the files it has as a byte array
                                        int bitFieldLength = length - 1;
                                        byte[] bitfield = new byte[bitFieldLength];
                                        is.readFully(bitfield);
                                        //System.out.println(bitfield.toString());
                                        //String bf = "";
                                        for (byte b : bitfield) {
                                            //System.out.println(String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
                                            bitfieldBinary.append(String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
                                        }
                                        //bitfieldBinary = bf;
                                        //System.out.println("Bitfield = " + bitfieldBinary.toString());
                                    } else if (responseID == 7) { //sending piece
                                        int blockLength = length - 9; //get the length of the block. SHOULD BE 16kB
                                        int index = is.readInt(); //get the index/offset of the piece
                                        int offset = is.readInt(); //get the chunk/offset WITHIN the piece
                                        byte[] piece = pieces.get(index); //reference the byte array in the pieces hashmap for this piece/index
                                        is.read(piece, offset, blockLength); //read the chunk into the byte array at the correct offset
                                        System.out.println("piece: " + index + ", block: " + offset + " , size: " + blockLength);
                                        if (offset == chunks) pieceFinished = true; //if this is the last chunk, we have downloaded a whole piece and should wait to be cleared
                                    } else {
                                    	//a message we dont care about
                                        int messagelength = length - 1;
                                        if (messagelength > 0) {
                                            byte[] temp = new byte[messagelength];
                                            is.readFully(temp); //get rid of the content of the message
                                         }
                                    }
                                }
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    } else {
                        is.close();
                        os.close();
                        clientSocket.close();
                    }
                }
            }
        } catch (EOFException e) {
            e.printStackTrace();
            is.close();
            os.close();
            clientSocket.close();
        }
        is.close();
        os.close();
        clientSocket.close();
    }
    
    /**
     * 
     * @param start The index to start searching at
     * @return the index of a piece that has not been requested by any peerConnection Object, or -1.
     */
    private int findUnrequested(int start) {
        for (int i = start; i < peiceReference.length; i++) {
            if (peiceReference[i] == 0) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Automatically request the next sequential piece that this peer can provide. Request every chunk in the peice.
     * @param os DataOutputStream to write/send messages to
     * @throws IOException
     */
    private void request(DataOutputStream os) throws IOException {
            do {
                index = findUnrequested(index); //get the index of a peice in pieceReference that has not been written or requested
                //System.out.println(id + "Piece at index: " + index + " is linked to a bitFieldBinary character at: " + bitfieldBinary.charAt(index));
                //System.out.println("BitFieldBinary character at index: " + bitfieldBinary.charAt(index));
                if (Character.getNumericValue(bitfieldBinary.charAt(index)) == 1) { //if the peer can serve us this piece
                    pieces.put(index, new byte[pieceLength]); //put a byte array into the hashmap to hold this piece
                    peiceReference[index] = 1; //set this piece to 'requested' so other peerConnections do not double up
                    do {
                        System.out.println("requesting chunk "  + chunk + " of piece " + index + " from " + id);
                        os.writeInt(13); //static request length
                        os.writeByte(6); //request id
                        os.writeInt(index); //piece index
                        os.writeInt(chunk); //'chunk' of that piece (offset)
                        os.writeInt(chunkSize); //get a full size chunk
                        chunk++; //next chunk
                } while (chunk < chunks);
                os.writeInt(13); //static request length
                os.writeByte(6); //request id
                os.writeInt(index); //piece index
                os.writeInt(chunk);
                os.writeInt(pieceLength - chunks * chunkSize); //get the remainder of the peice
                chunk = 0; //set chunk to 0 to find a new piece
                }
                else {
                    index++;
                } 
            } while (index != -1 && index != peiceReference.length && chunks == 0); //check that there is a piece and that the peer's bitfield says the peer has it to server
    }
    
    /**
     * @return whether a complete piece has been received
     */
    public boolean getPieceStatus() {
        return pieceFinished;
    }
    /**
     * @return whether a connection was succesful made to the peer
     */
    public boolean getConnectionStatus() {
        return connectionStatus;
    }
    /**
     * @return hashmap containing the downloaded piece and its index as the key
     */
    public HashMap<Integer, byte[]> getPieces() {
        return pieces;
    }
    /**
     * clear the received piece and reset values to search for a new one
     */
    public void clear() {
        pieces.clear();
        index = 0;
        chunk = 0;
        pieceFinished = false;
    }
  
}
