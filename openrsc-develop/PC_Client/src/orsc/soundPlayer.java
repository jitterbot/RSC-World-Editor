package orsc;

import orsc.util.GenUtil;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import java.io.File;

public class soundPlayer {
	public static void playSoundFile(String key) {
		try {
			if (!mudclient.optionSoundDisabled) {
				File sound = mudclient.soundCache.get(key + ".wav");
				if (sound == null)
					return;
				try {
					                 
					final Clip clip = AudioSystem.getClip();
					clip.addLineListener(myLineEvent -> {
						if (myLineEvent.getType() == LineEvent.Type.STOP)
							clip.close();
					});
					clip.open(AudioSystem.getAudioInputStream(sound));
					clip.start();

					                      
					                                                                             
					                                                                         
					                                                      
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}

		} catch (RuntimeException var6) {
			throw GenUtil.makeThrowable(var6, "client.SC(" + "dummy" + ',' + (key != null ? "{...}" : "null") + ')');
		}
	}
}
