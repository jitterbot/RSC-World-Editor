package com.openrsc.server.util;

import org.apache.logging.log4j.LogManager;
import java.time.LocalDate;
import java.time.Month;

public class SystemUtil {
    private SystemUtil() {}

    public static void exit(int statusCode) {
                                                                                                                
        LogManager.shutdown();
        System.exit(statusCode);
    }

    public static boolean isJune() {
    	return LocalDate.now().getMonth() == Month.JUNE;
	}
}
