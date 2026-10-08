package com.openrsc.server.model;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

   
               
                                                                   
                      
   
public class Cache {

	   
                            
    
	private ConcurrentMap<String, Object> storage = new ConcurrentHashMap<String, Object>();

	public Map<String, Object> getCacheMap() {
		return storage;
	}

	   
                                                              
   
              
           
    
	public boolean hasKey(String key) {
		return storage.containsKey(key);
	}

	   
                                 
   
              
            
                                                                                 
    
	public void set(String key, int i) {
		storage.put(key, i);
	}

	   
                               
   
              
            
                                                                                 
    
	public void store(String key, String s) {
		storage.put(key, s);
	}

	   
                                
   
              
            
                                                                                 
    
	public void store(String key, Boolean b) {
		storage.put(key, b);
	}

	   
                             
   
              
            
                                                                                 
    
	public void store(String key, long l) {
		storage.put(key, l);
	}

	   
                                 
   
              
           
                                                                        
                                                                                              
                                             
    
	public int getInt(String key) {
		if (!storage.containsKey(key))
			throw new NoSuchElementException("No object found for that key: " + key);

		Object value = storage.get(key);

		                                       
		if (value instanceof String) {
			value = Integer.parseInt((String)value);
		}

		if (!(value instanceof Integer)) {
			throw new IllegalArgumentException(
				"Object found, but not an Integer: " + key);
		}
		return (Integer) value;
	}

	   
                               
   
              
           
                                                                        
                                                                              
    
	public String getString(String key) {
		if (!storage.containsKey(key))
			throw new NoSuchElementException("No object found for that key: " + key);
		if (!(storage.get(key) instanceof String)) {
			throw new IllegalArgumentException(
				"Object found, but not an String: " + key);
		}
		return (String) (storage.get(key));
	}

	   
                                
   
              
           
                                                                        
                                                                               
    
	public Boolean getBoolean(String key) {
		if (!storage.containsKey(key))
			throw new NoSuchElementException("No object found for that key: " + key);
		if (!(storage.get(key) instanceof Boolean)) {
			throw new IllegalArgumentException(
				"Object found, but not a Boolean: " + key);
		}
		return (Boolean) (storage.get(key));
	}

	   
                             
   
              
           
                                                                        
                                                                            
    
	public long getLong(String key) {
		if (!storage.containsKey(key))
			throw new NoSuchElementException("No object found for that key: " + key);

		Object value = storage.get(key);
		if (!(value instanceof Long)) {
			throw new IllegalArgumentException("Object found, but not a Long: " + key);
		}
		return (Long) value;
	}

	   
                                  
   
              
    
	public void remove(String key) {
		storage.remove(key);
	}

	public void remove(String... key) {
		for (String s : key) {
			if (storage.containsKey(s))
				storage.remove(s);
		}
	}

	public void put(String key, Object o) {
		storage.put(key, o);
	}
}
