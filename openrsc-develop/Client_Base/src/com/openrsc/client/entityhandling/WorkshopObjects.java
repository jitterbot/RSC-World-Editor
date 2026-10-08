package com.openrsc.client.entityhandling;

import com.openrsc.client.entityhandling.defs.GameObjectDef;
import java.util.List;
import java.util.Properties;
import java.io.File;
import java.io.FileInputStream;

                                                                              
public final class WorkshopObjects {
    public static void load(List<GameObjectDef> objects) {
        File file = new File(System.getProperty("rscworldeditor.objects", "Cache/WorkshopObjects.properties"));
        if (!file.isFile()) return;
        Properties p = new Properties();
        try (FileInputStream input = new FileInputStream(file)) {
            p.load(input);
            int count = Integer.parseInt(p.getProperty("count", "0"));
            int start = Integer.parseInt(p.getProperty("start", String.valueOf(objects.size())));
            if (count == 0) return;
            if (count < 0 || count > 100 || start != objects.size()) throw new IllegalStateException("Workshop scenery IDs do not match this client");
            for (int id = start; id < start + count; id++) {
                String key = id + ".";
                objects.add(new GameObjectDef(p.getProperty(key+"name"), p.getProperty(key+"description"),
                    p.getProperty(key+"command1"), "Examine", number(p,key,"type"), number(p,key,"width"),
                    number(p,key,"height"), number(p,key,"groundItemVar"), p.getProperty(key+"objectModel"), id));
            }
        } catch (Exception error) {
            throw new IllegalStateException("Could not load Rsc World Editor scenery", error);
        }
    }
    private static int number(Properties p, String key, String field) {
        return Integer.parseInt(p.getProperty(key+field));
    }
}
