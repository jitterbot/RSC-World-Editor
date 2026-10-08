package com.openrsc.server.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class YMLReader {
	private static final Logger LOGGER = LogManager.getLogger();

	private final HashMap<String, String> settings = new HashMap<>();

	public void loadFromYML(String fileName) throws IOException {
		List<String> lines = Collections.emptyList();

		                                                               
		                                    
		try {
			lines = Files.readAllLines(Paths.get(fileName));
		}

		catch (IOException e) {
			throw e;
		}

		for (String line : lines) {
			                  
			if (line.contains("#")) {
				                            
				if (line.split("#").length < 2)
					continue;

				                                  
				if (line.indexOf('#') < line.indexOf(':'))
					continue;

				                          
				String[] sublines = line.split("#");
				for (String subline : sublines) {
					if (subline.contains(":")) {
						line = subline;
						break;
					}
				}
			}

			String[] elems = line.split(":");

			                      
			for (int i = 0; i < elems.length; ++i) {
				elems[i] = elems[i].trim();
			}

			switch (elems.length) {
				case 2:
					                                                 
					                                               
					if (elems[1].equalsIgnoreCase("null")) {
						LOGGER.info(fileName + ": Key \"" + elems[0] +
							"\" has null value.");
						settings.put(elems[0], elems[1]);
					}
					                      
					else {
						                                               
						if (!(settings.containsKey(elems[0]))) {
							settings.put(elems[0], elems[1]);
						}
						else {
							LOGGER.info(fileName + ": Duplicate key: " + elems[0]);
						}
					}
					break;
				case 3:
					                                                                     
					settings.put(elems[0], (elems[1] + ":" + elems[2]));
					break;
			}
		}
	}

	                                                         
	                                                                                            
	public String getAttribute(String key) {
		if (settings.containsKey(key)) {
			return settings.get(key);
		}

		return "NOT_HERE";
	}

	                                                      
	                
	public boolean keyExists(String key) {
		return settings.containsKey(key);
	}
}

