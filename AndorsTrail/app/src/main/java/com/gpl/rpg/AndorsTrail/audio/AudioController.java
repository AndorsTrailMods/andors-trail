package com.gpl.rpg.AndorsTrail.audio;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.preference.PreferenceManager;
import android.util.Log;

import com.gpl.rpg.AndorsTrail.R;

public final class AudioController implements SharedPreferences.OnSharedPreferenceChangeListener {
	private static final String TAG = "AudioController";
	private static final String EFFECTS_VOLUME_KEY = "effects_volume";
	private static final int DEFAULT_EFFECTS_VOLUME = 85;

	private final SharedPreferences preferences;
	private final SoundPool soundPool;
	private final int hitSound;
	private final int missSound;
	private final int monsterHitSound;
	private final int monsterMissSound;
	private final int magicSound;
	private final int ravenMarkSound;
	private final int crowFormSound;
	private final int murderOfCrowsSound;
	private int effectsVolume;
	private boolean foreground;

	public AudioController(Context context) {
		Context appContext = context.getApplicationContext();
		this.preferences = PreferenceManager.getDefaultSharedPreferences(appContext);
		this.soundPool = new SoundPool.Builder()
				.setMaxStreams(8)
				.setAudioAttributes(new AudioAttributes.Builder()
						.setUsage(AudioAttributes.USAGE_GAME)
						.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
						.build())
				.build();
		this.soundPool.setOnLoadCompleteListener((pool, sampleId, status) -> {
			if (status != 0) Log.e(TAG, "Unable to load sound effect " + sampleId + " (status " + status + ")");
		});
		this.hitSound = soundPool.load(appContext, R.raw.sfx_hit, 1);
		this.missSound = soundPool.load(appContext, R.raw.sfx_miss, 1);
		this.monsterHitSound = soundPool.load(appContext, R.raw.sfx_monster_hit, 1);
		this.monsterMissSound = soundPool.load(appContext, R.raw.sfx_monster_miss, 1);
		this.magicSound = soundPool.load(appContext, R.raw.sfx_magic, 1);
		this.ravenMarkSound = soundPool.load(appContext, R.raw.sfx_ravenmark, 1);
		this.crowFormSound = soundPool.load(appContext, R.raw.sfx_crowform, 1);
		this.murderOfCrowsSound = soundPool.load(appContext, R.raw.sfx_murder_of_crows, 1);
		this.effectsVolume = readEffectsVolume();
		this.preferences.registerOnSharedPreferenceChangeListener(this);
	}

	public void resumeAudio() {
		foreground = true;
		soundPool.autoResume();
	}

	public void pauseAudio() {
		foreground = false;
		soundPool.autoPause();
	}

	public void playPlayerHit() {
		playEffect(hitSound);
	}

	public void playPlayerMiss() {
		playEffect(missSound);
	}

	public void playMonsterHit() {
		playEffect(monsterHitSound);
	}

	public void playMonsterMiss() {
		playEffect(monsterMissSound);
	}

	public void playMagic() {
		playEffect(magicSound);
	}

	public void playCrowForm() {
		playEffect(crowFormSound);
	}

	public void playRavenMark() {
		playEffect(ravenMarkSound);
	}

	public void playMurderOfCrows() {
		playEffect(murderOfCrowsSound);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (EFFECTS_VOLUME_KEY.equals(key)) effectsVolume = readEffectsVolume();
	}

	private int readEffectsVolume() {
		String storedValue = preferences.getString(EFFECTS_VOLUME_KEY, Integer.toString(DEFAULT_EFFECTS_VOLUME));
		try {
			int volume = Integer.parseInt(storedValue);
			return Math.max(0, Math.min(100, volume));
		} catch (NumberFormatException e) {
			Log.e(TAG, "Invalid volume preference for " + EFFECTS_VOLUME_KEY + ": " + storedValue, e);
			return DEFAULT_EFFECTS_VOLUME;
		}
	}

	private void playEffect(int soundId) {
		if (foreground && effectsVolume > 0) {
			float volume = effectsVolume / 100f;
			soundPool.play(soundId, volume, volume, 1, 0, 1f);
		}
	}
}
