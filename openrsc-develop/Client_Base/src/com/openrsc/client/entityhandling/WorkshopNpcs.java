package com.openrsc.client.entityhandling;

import com.openrsc.client.entityhandling.defs.NPCDef;
import java.util.List;
import java.util.Properties;
import java.io.File;
import java.io.FileInputStream;

                                                                                        
public final class WorkshopNpcs {
    public static void load(List<NPCDef> npcs) {
        File file = new File("Cache/WorkshopNpcs.properties");
        if (!file.isFile()) return;
        Properties p = new Properties();
        try (FileInputStream input = new FileInputStream(file)) {
            p.load(input);
            int count = Integer.parseInt(p.getProperty("count", "0"));
            int start = Integer.parseInt(p.getProperty("start", String.valueOf(npcs.size())));
            if (count == 0) return;
            if (start != npcs.size()) throw new IllegalStateException("Workshop NPC IDs do not match this client");
            for (int id = start; id < start + count; id++) {
                String key = id + ".";
                int[] sprites = new int[12];
                for (int i = 0; i < sprites.length; i++) sprites[i] = number(p, key, "sprites" + (i+1));
                npcs.add(new NPCDef(p.getProperty(key+"name"), p.getProperty(key+"description"), "", "",
                    number(p,key,"attack"), number(p,key,"strength"), number(p,key,"hits"), number(p,key,"defense"), number(p,key,"attackable")==1, sprites, number(p,key,"hairColour"), number(p,key,"topColour"),
                    number(p,key,"bottomColour"), number(p,key,"skinColour"), number(p,key,"camera1"),
                    number(p,key,"camera2"), number(p,key,"walkModel"), number(p,key,"combatModel"), number(p,key,"combatSprite"), id));
            }
        } catch (Exception error) {
            throw new IllegalStateException("Could not load Rsc World Editor NPCs", error);
        }
    }
    private static int number(Properties p, String key, String field) {
        return Integer.parseInt(p.getProperty(key+field));
    }
}
