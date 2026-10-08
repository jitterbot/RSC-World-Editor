package com.openrsc.server.plugins;

import com.openrsc.server.constants.ItemId;
import com.openrsc.server.model.entity.npc.Npc;
import com.openrsc.server.model.entity.player.Player;
import java.util.Properties;
import java.io.File;
import java.io.FileInputStream;
import static com.openrsc.server.plugins.Functions.*;

                                                                                     
public final class WorkshopGhostspeak {
    private WorkshopGhostspeak() {}
    public static boolean blocked(Player player, Npc npc) {
        Properties p = new Properties();
        File file = new File(player.getConfig().CONFIG_DIR, "defs/WorkshopDialogue.properties");
        if (!file.isFile()) return false;
        try (FileInputStream input = new FileInputStream(file)) { p.load(input); }
        catch (Exception error) { throw new IllegalStateException("Cannot read Workshop dialogue", error); }
        String prefix = npc.getID()+".";
        if (!requiresAmulet(p, prefix) || player.getCarriedItems().getEquipment().hasEquipped(ItemId.AMULET_OF_GHOSTSPEAK.id())) return false;
        play(player, npc, p, prefix+"ghost.");
        return true;
    }
    static boolean requiresAmulet(Properties p, String prefix) {
        return Boolean.parseBoolean(p.getProperty(prefix+"requiresGhostspeak", "false"));
    }
    private static void play(Player player, Npc npc, Properties p, String prefix) {
        int node = Integer.parseInt(p.getProperty(prefix+"start", "-1"));
                                                                                  
        for (int visited = 0; node >= 0 && visited < 100; visited++) {
            String key = prefix+node+".";
            String line = p.getProperty(key+"text");
            if (line == null) return;
            int lineCount = Integer.parseInt(p.getProperty(key+"lineCount", "0"));
            if (lineCount == 0) npcsay(player, npc, line.split("\\n"));
            else {
                if (lineCount < 1 || lineCount > 10) return;
                for (int i=0; i<lineCount; i++) {
                    String spoken = p.getProperty(key+"line."+i+".text");
                    if (spoken == null) return;
                    if ("player".equals(p.getProperty(key+"line."+i+".speaker"))) say(player, npc, spoken);
                    else npcsay(player, npc, spoken);
                }
            }
            int count = Integer.parseInt(p.getProperty(key+"count", "0"));
            if (count <= 0 || count > 4) return;
            String[] replies = new String[count];
            for (int i=0; i<count; i++) replies[i]=p.getProperty(key+i+".text");
            int selected = multi(player, npc, replies);
            if (selected < 0 || selected >= count) return;
            node = Integer.parseInt(p.getProperty(key+selected+".next", "-1"));
        }
    }
}
