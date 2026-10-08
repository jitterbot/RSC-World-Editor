package com.openrsc.server.model.snapshot;

import java.util.ArrayList;

public class Chatlog extends Snapshot {

	   
                              
    
	private String message;
	   
                                             
    
	private ArrayList<String> recievers = new ArrayList<String>();

	   
               
   
                                                  
                                               
                                                         
    
	public Chatlog(String sender, String chatstring, ArrayList<String> recievers) {
		super(sender);
		this.setMessage(chatstring);
		this.recievers = recievers;
	}

	public Chatlog(String sender, String chatstring) {
		super(sender);
		this.setMessage(chatstring);
	}

	public ArrayList<String> getRecievers() {
		return recievers;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}

