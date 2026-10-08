package com.openrsc.server.model.snapshot;

public class Activity extends Snapshot {

	   
                              
    
	private String activity;

	   
               
   
                                                      
                                                   
    
	public Activity(String sender, String activity) {
		super(sender);
		this.setActivity(activity);
	}

	public String getActivity() {
		return activity;
	}

	public void setActivity(String activity) {
		this.activity = activity;
	}
}

