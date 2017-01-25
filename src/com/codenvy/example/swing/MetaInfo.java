package com.codenvy.example.swing;

import java.io.*;

import java.io.File;
import java.io.IOException;
import com.hypirion.bencode.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * MetaInfo parses the torrent file to extract necessary fields and data
 */
@SuppressWarnings("unchecked")
public class MetaInfo {
    private String announce, pieces;
    private ArrayList<String> announceList = new ArrayList<String>();
    private ArrayList<HashMap<String,Object>> files = new ArrayList<HashMap<String,Object>>();
    private long pieceLength;
    private boolean multiFile;
    private long totalFileSize = 0;
    private String sha1URL = "";
    private byte[] sha1;
    
    /**
     * Parses the BEncoded torrent file and stores relevant fields
     * @param filePath path to the torrent file
     * @throws FileNotFoundException
     */
    public MetaInfo(String filePath) throws FileNotFoundException{
        FileInputStream torrent = new FileInputStream(new File(filePath)); // Instantiates file input stream with the torrent file found at filepath

        BencodeReader br = new BencodeReader(torrent); // Instantiates bencode reader to read from torrent file input stream
        HashMap<String, Object> j = null;
        
        try {
            j = (HashMap<String, Object>)br.read();
            br.close();
        }
        catch (BencodeReadException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        
        HashMap<String, Object> info = (HashMap<String, Object>)j.get("info");
        
        announce = j.get("announce").toString();
    
        pieceLength = (long)info.get("piece length");
        pieces = info.get("pieces").toString();
        
        if (info.containsKey("files")) multiFile = true; // if the torrent is for multiple files or just 1
        if (multiFile) { //if it ise construct an arraylist of hashmaps with the name of the file and its designated path
            files = (ArrayList<HashMap<String,Object>>) info.get("files");
            for (int i = 0; i < files.size(); i++) { 
                System.out.println(files.get(i).get("name") + ": " + files.get(i).get("path"));
            }
        } else { ///otherwise construct the same system to hold the file
            HashMap<String,Object> aTadBitSketchy = new HashMap<String,Object>();
            aTadBitSketchy.put("name", (String) info.get("name"));
            aTadBitSketchy.put("length", (long) info.get("length"));
            files.add(aTadBitSketchy);
        }
        
        
        for (int i = 0; i < files.size(); i++) {
            totalFileSize += (long) files.get(i).get("length");
        }
        
        Object[] hashs = InfoHashGen.generate(filePath);
        sha1URL = (String) hashs[0];
        sha1 = (byte[]) hashs[1];
    }
    
    @SuppressWarnings("unchecked")
	public HashMap<String, Object> decodePath(byte[] path) throws BencodeReadException{
		ByteArrayInputStream bais = new ByteArrayInputStream(path);
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
     * @param type, a letter representing different fields to return: a - announce, b - announce list, c - pieces byte string, d - piece length, e - files, f - total file size, g - url safe info hash, h - byte array info hash
     * @return relevant field
     */
    public Object getValues(char type) {
        switch (type) {
            case 'a': return announce;
            case 'b': return announceList;
            case 'c': return pieces;
            case 'd': return pieceLength;
            case 'e': return files;
            case 'f': return totalFileSize;
            case 'g': return sha1URL;
            case 'h': return sha1;
            default: return type;
        }
        
    }
}
