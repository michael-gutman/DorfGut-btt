package com.codenvy.example.swing;

import java.io.File;
import java.io.IOException;
import com.hypirion.bencode.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.ArrayList;

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
    
    public MetaInfo(String filePath) throws FileNotFoundException{
        FileInputStream torrent = new FileInputStream(new File(filePath));

        BencodeReader br = new BencodeReader(torrent);
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
        
        ArrayList<ArrayList<String>> doubleList = ((ArrayList<ArrayList<String>>)j.get("announce-list"));
        HashMap<String, Object> info = (HashMap<String, Object>)j.get("info");
        
        announce = j.get("announce").toString();
//        for (int i = 0; i < doubleList.size(); i++){
//            announceList.add(doubleList.get(i).get(0)); 
//        }
    
        pieceLength = (long)info.get("piece length");
        pieces = info.get("pieces").toString();
        
        if (info.containsKey("files")) multiFile = true;
        if (multiFile) {
            files = (ArrayList<HashMap<String,Object>>) info.get("files");  
        } else {
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
       
        //System.out.println("announce: " + announce);
        //System.out.println("announce-list: " + announceList);
        //System.out.println("piece-length: " + pieceLength);
        //System.out.println("pieces: " + pieces);
        
        
    }
    public MetaInfo(File file) throws FileNotFoundException{
        FileInputStream torrent = new FileInputStream(file);

        BencodeReader br = new BencodeReader(torrent);
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
        
        ArrayList<ArrayList<String>> doubleList = ((ArrayList<ArrayList<String>>)j.get("announce-list"));
        HashMap<String, Object> info = (HashMap<String, Object>)j.get("info");
        
        announce = j.get("announce").toString();
        for (int i = 0; i < doubleList.size(); i++){
            announceList.add(doubleList.get(i).get(0)); 
        }
    
        pieceLength = (long)info.get("piece length");
        pieces = info.get("pieces").toString();
        
        if (info.containsKey("files")) multiFile = true;
        if (multiFile) {
            files = (ArrayList<HashMap<String,Object>>) info.get("files");  
        } else {
            HashMap<String,Object> aTadBitSketchy = new HashMap<String,Object>();
            aTadBitSketchy.put("name", (String) info.get("name"));
            aTadBitSketchy.put("length", (long) info.get("length"));
            files.add(aTadBitSketchy);
        }
        
        for (int i = 0; i < files.size(); i++) {
            totalFileSize += (long) files.get(i).get("length");
        }
    }
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
            default: return 'a';
        }
        
    }
}

