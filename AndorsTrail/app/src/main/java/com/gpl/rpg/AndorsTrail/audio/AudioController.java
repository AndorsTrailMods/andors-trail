package com.gpl.rpg.AndorsTrail.audio;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import com.gpl.rpg.AndorsTrail.R;

public final class AudioController implements SharedPreferences.OnSharedPreferenceChangeListener {
	private static final String TAG = "AudioController";
	private static final String MUSIC_VOLUME_KEY = "music_volume";
	private static final String EFFECTS_VOLUME_KEY = "effects_volume";
	private static final int DEFAULT_MUSIC_VOLUME = 70;
	private static final int DEFAULT_EFFECTS_VOLUME = 85;

	private final Context context;
	private final SharedPreferences preferences;
	private final AudioManager audioManager;
	private final SoundPool soundPool;
	private final int hitSound;
	private final int missSound;
	private final int monsterHitSound;
	private final int monsterMissSound;
	private final int magicSound;
	private final int ravenMarkSound;
	private final int crowFormSound;
	private final int murderOfCrowsSound;
	private final AudioManager.OnAudioFocusChangeListener audioFocusChangeListener = this::onAudioFocusChange;
	private final AudioFocusRequest audioFocusRequest;
	private MediaPlayer musicPlayer;
	private int currentMusicResource;
	private int musicVolume;
	private int effectsVolume;
	private boolean foreground;
	private boolean inCombat;
	private boolean hasAudioFocus;

	public AudioController(Context context) {
		this.context = context.getApplicationContext();
		this.preferences = PreferenceManager.getDefaultSharedPreferences(this.context);
		this.audioManager = (AudioManager) this.context.getSystemService(Context.AUDIO_SERVICE);
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
		this.hitSound = soundPool.load(this.context, R.raw.sfx_hit, 1);
		this.missSound = soundPool.load(this.context, R.raw.sfx_miss, 1);
		this.monsterHitSound = soundPool.load(this.context, R.raw.sfx_monster_hit, 1);
		this.monsterMissSound = soundPool.load(this.context, R.raw.sfx_monster_miss, 1);
		this.magicSound = soundPool.load(this.context, R.raw.sfx_magic, 1);
		this.ravenMarkSound = soundPool.load(this.context, R.raw.sfx_ravenmark, 1);
		this.crowFormSound = soundPool.load(this.context, R.raw.sfx_crowform, 1);
		this.murderOfCrowsSound = soundPool.load(this.context, R.raw.sfx_murder_of_crows, 1);
		this.musicVolume = readVolume(MUSIC_VOLUME_KEY, DEFAULT_MUSIC_VOLUME);
		this.effectsVolume = readVolume(EFFECTS_VOLUME_KEY, DEFAULT_EFFECTS_VOLUME);
		this.audioFocusRequest = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
				? new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
						.setAudioAttributes(new AudioAttributes.Builder()
								.setUsage(AudioAttributes.USAGE_GAME)
								.setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
								.build())
						.setOnAudioFocusChangeListener(audioFocusChangeListener)
						.build()
				: null;
		this.preferences.registerOnSharedPreferenceChangeListener(this);
	}

	public void resumeMusic(boolean inCombat) {
		this.foreground = true;
		this.inCombat = inCombat;
		updateMusic();
		soundPool.autoResume();
	}

	public void pauseMusic() {
		foreground = false;
		if (musicPlayer != null && musicPlayer.isPlaying()) musicPlayer.pause();
		soundPool.autoPause();
		abandonAudioFocus();
	}

	public void setCombatMusic(boolean inCombat) {
		this.inCombat = inCombat;
		updateMusic();
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
		if (MUSIC_VOLUME_KEY.equals(key)) {
			musicVolume = readVolume(MUSIC_VOLUME_KEY, DEFAULT_MUSIC_VOLUME);
			updateMusic();
		} else if (EFFECTS_VOLUME_KEY.equals(key)) {
			effectsVolume = readVolume(EFFECTS_VOLUME_KEY, DEFAULT_EFFECTS_VOLUME);
		}
	}

	private int readVolume(String key, int defaultValue) {
		String storedValue = preferences.getString(key, Integer.toString(defaultValue));
		try {
			int volume = Integer.parseInt(storedValue);
			return Math.max(0, Math.min(100, volume));
		} catch (NumberFormatException e) {
			Log.e(TAG, "Invalid volume preference for " + key + ": " + storedValue, e);
			return defaultValue;
		}
	}

	private void playEffect(int soundId) {
		if (foreground && effectsVolume > 0) {
			float volume = effectsVolume / 100f;
			soundPool.play(soundId, volume, volume, 1, 0, 1f);
		}
	}

	private void updateMusic() {
		if (!foreground || musicVolume == 0) {
			if (musicPlayer != null && musicPlayer.isPlaying()) musicPlayer.pause();
			if (musicVolume == 0) abandonAudioFocus();
			return;
		}
		if (!requestAudioFocus()) return;

		int requestedMusic = inCombat ? R.raw.music_combat : R.raw.music_exploration;
		if (musicPlayer == null || currentMusicResource != requestedMusic) {
			releaseMusicPlayer();
			musicPlayer = MediaPlayer.create(context, requestedMusic);
			if (musicPlayer == null) {
				Log.e(TAG, "Unable to create music player for resource " + requestedMusic);
				currentMusicResource = 0;
				return;
			}
			currentMusicResource = requestedMusic;
			musicPlayer.setLooping(true);
			musicPlayer.setOnErrorListener((player, what, extra) -> {
				Log.e(TAG, "Music playback failed (what " + what + ", extra " + extra + ")");
				releaseMusicPlayer();
				return true;
			});
		}
		float volume = musicVolume / 100f;
		musicPlayer.setVolume(volume, volume);
		if (!musicPlayer.isPlaying()) musicPlayer.start();
	}

	private boolean requestAudioFocus() {
		if (hasAudioFocus) return true;
		int result;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			result = audioManager.requestAudioFocus(audioFocusRequest);
		} else {
			result = audioManager.requestAudioFocus(audioFocusChangeListener,
					AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
		}
		hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
		return hasAudioFocus;
	}

	private void abandonAudioFocus() {
		if (!hasAudioFocus) return;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			audioManager.abandonAudioFocusRequest(audioFocusRequest);
		} else {
			audioManager.abandonAudioFocus(audioFocusChangeListener);
		}
		hasAudioFocus = false;
	}

	private void onAudioFocusChange(int focusChange) {
		if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
			hasAudioFocus = true;
			updateMusic();
		} else {
			hasAudioFocus = false;
			if (musicPlayer != null && musicPlayer.isPlaying()) musicPlayer.pause();
		}
	}

	private void releaseMusicPlayer() {
		if (musicPlayer != null) {
			musicPlayer.release();
			musicPlayer = null;
		}
		currentMusicResource = 0;
	}
}
