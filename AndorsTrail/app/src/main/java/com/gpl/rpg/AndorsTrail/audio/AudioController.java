package com.gpl.rpg.AndorsTrail.audio;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Build;
import android.os.Process;
import android.preference.PreferenceManager;
import android.util.Log;

import com.gpl.rpg.AndorsTrail.R;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class AudioController implements SharedPreferences.OnSharedPreferenceChangeListener {
	private static final String TAG = "AudioController";
	private static final String MUSIC_VOLUME_KEY = "music_volume";
	private static final String EFFECTS_VOLUME_KEY = "effects_volume";
	private static final int DEFAULT_MUSIC_VOLUME = 70;
	private static final int DEFAULT_EFFECTS_VOLUME = 85;
	private static final int MUSIC_SAMPLE_RATE = 22050;
	private static final int MUSIC_CHANNEL_COUNT = 2;
	private static final int MUSIC_BYTES_PER_FRAME = MUSIC_CHANNEL_COUNT * 2;
	private static final int EXPLORATION_LOOP_START_MILLIS = 32756;
	private static final int EXPLORATION_WRITE_CHUNK_BYTES = 8192;

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
	private AudioTrack explorationTrack;
	private Thread explorationWriterThread;
	private volatile boolean explorationWriterRunning;
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
		pauseCurrentMusic();
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
			pauseCurrentMusic();
			if (musicVolume == 0) abandonAudioFocus();
			return;
		}
		if (!requestAudioFocus()) return;

		int requestedMusic = inCombat ? R.raw.music_combat : R.raw.music_exploration;
		if (requestedMusic == R.raw.music_exploration) {
			boolean sameExplorationTrack = currentMusicResource == requestedMusic;
			boolean writerStopped = explorationWriterThread == null || !explorationWriterThread.isAlive();
			if (explorationTrack == null || !sameExplorationTrack || writerStopped) {
				int resumeFrame = sameExplorationTrack && explorationTrack != null
						? getExplorationResumeFrame(explorationTrack)
						: 0;
				releaseMusicPlayer();
				if (!createExplorationTrack(resumeFrame)) return;
				currentMusicResource = requestedMusic;
			}
			float volume = musicVolume / 100f;
			explorationTrack.setStereoVolume(volume, volume);
			if (explorationTrack.getPlayState() != AudioTrack.PLAYSTATE_PLAYING) {
				explorationTrack.play();
			}
			return;
		}

		if (musicPlayer == null || currentMusicResource != requestedMusic) {
			releaseMusicPlayer();
			musicPlayer = MediaPlayer.create(context, requestedMusic);
			if (musicPlayer == null) {
				Log.e(TAG, "Unable to create music player for resource " + requestedMusic);
				currentMusicResource = 0;
				return;
			}
			musicPlayer.setOnErrorListener(this::onMusicPlayerError);
			musicPlayer.setLooping(true);
			currentMusicResource = requestedMusic;
		}
		float volume = musicVolume / 100f;
		musicPlayer.setVolume(volume, volume);
		if (!musicPlayer.isPlaying()) musicPlayer.start();
	}

	private int getExplorationResumeFrame(AudioTrack track) {
		try {
			PcmWavData wav = readExplorationWav();
			if (wav == null || wav.blockAlign <= 0) return 0;
			int loopStartFrame = EXPLORATION_LOOP_START_MILLIS * wav.sampleRate / 1000;
			int frameCount = wav.dataLength / wav.blockAlign;
			long playedFrames = Integer.toUnsignedLong(track.getPlaybackHeadPosition());
			long sourceFrame = playedFrames < loopStartFrame
					? playedFrames
					: loopStartFrame + (playedFrames - loopStartFrame) % (frameCount - loopStartFrame);
			return (int) Math.min(sourceFrame, frameCount - 1L);
		} catch (IllegalStateException e) {
			Log.e(TAG, "Unable to read exploration playback position; restarting from the beginning", e);
			return 0;
		}
	}

	private boolean createExplorationTrack(int startFrame) {
		PcmWavData wav = readExplorationWav();
		if (wav == null) return false;
		if (wav.sampleRate != MUSIC_SAMPLE_RATE
				|| wav.channels != MUSIC_CHANNEL_COUNT
				|| wav.bitsPerSample != 16
				|| wav.blockAlign != MUSIC_BYTES_PER_FRAME
				|| wav.dataLength % MUSIC_BYTES_PER_FRAME != 0) {
			Log.e(TAG, "Exploration music must be 22050 Hz, stereo, 16-bit PCM");
			return false;
		}

		int loopStartFrame = EXPLORATION_LOOP_START_MILLIS * wav.sampleRate / 1000;
		int frameCount = wav.dataLength / wav.blockAlign;
		if (loopStartFrame <= 0 || loopStartFrame >= frameCount) {
			Log.e(TAG, "Exploration loop start is outside the PCM data");
			return false;
		}
		if (startFrame < 0 || startFrame >= frameCount) {
			Log.e(TAG, "Exploration resume position is outside the PCM data: " + startFrame);
			return false;
		}
		int startByte = wav.dataOffset + startFrame * wav.blockAlign;
		int loopStartByte = wav.dataOffset + loopStartFrame * wav.blockAlign;
		int dataEndByte = wav.dataOffset + wav.dataLength;
		int minBufferSize = AudioTrack.getMinBufferSize(
				wav.sampleRate,
				AudioFormat.CHANNEL_OUT_STEREO,
				AudioFormat.ENCODING_PCM_16BIT);
		if (minBufferSize <= 0) {
			Log.e(TAG, "Unable to determine exploration AudioTrack buffer size: " + minBufferSize);
			return false;
		}

		AudioTrack track = new AudioTrack(
				AudioManager.STREAM_MUSIC,
				wav.sampleRate,
				AudioFormat.CHANNEL_OUT_STEREO,
				AudioFormat.ENCODING_PCM_16BIT,
				Math.max(minBufferSize * 4, 32768),
				AudioTrack.MODE_STREAM);
		if (track.getState() != AudioTrack.STATE_INITIALIZED) {
			Log.e(TAG, "Unable to initialize exploration AudioTrack");
			track.release();
			return false;
		}

		explorationTrack = track;
		explorationWriterRunning = true;
		// Keep one PCM stream running so Android never has to hand off between players at the loop.
		explorationWriterThread = new Thread(
				() -> writeExplorationLoop(track, wav.data, startByte, loopStartByte, dataEndByte),
				"AndorsTrailExplorationAudio");
		explorationWriterThread.start();
		return true;
	}

	private PcmWavData readExplorationWav() {
		try (InputStream input = context.getResources().openRawResource(R.raw.music_exploration);
			 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			byte[] buffer = new byte[16384];
			int count;
			while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
			return parsePcmWav(output.toByteArray());
		} catch (IOException e) {
			Log.e(TAG, "Unable to read exploration music", e);
			return null;
		}
	}

	private PcmWavData parsePcmWav(byte[] wav) {
		if (wav.length < 12 || !hasFourCc(wav, 0, "RIFF") || !hasFourCc(wav, 8, "WAVE")) {
			Log.e(TAG, "Exploration music is not a valid RIFF/WAVE file");
			return null;
		}

		int sampleRate = 0;
		int channels = 0;
		int bitsPerSample = 0;
		int blockAlign = 0;
		int dataOffset = -1;
		int dataLength = 0;
		int offset = 12;
		while (offset <= wav.length - 8) {
			long chunkLength = readUnsignedLittleEndianInt(wav, offset + 4);
			long chunkEnd = offset + 8L + chunkLength;
			if (chunkEnd > wav.length) {
				Log.e(TAG, "Exploration WAV contains an invalid chunk length");
				return null;
			}
			int chunkData = offset + 8;
			if (hasFourCc(wav, offset, "fmt ")) {
				if (chunkLength < 16) {
					Log.e(TAG, "Exploration WAV has an incomplete format chunk");
					return null;
				}
				int format = readUnsignedLittleEndianShort(wav, chunkData);
				channels = readUnsignedLittleEndianShort(wav, chunkData + 2);
				sampleRate = (int) readUnsignedLittleEndianInt(wav, chunkData + 4);
				blockAlign = readUnsignedLittleEndianShort(wav, chunkData + 12);
				bitsPerSample = readUnsignedLittleEndianShort(wav, chunkData + 14);
				if (format != 1) {
					Log.e(TAG, "Exploration WAV is not uncompressed PCM");
					return null;
				}
			} else if (hasFourCc(wav, offset, "data")) {
				if (chunkLength > Integer.MAX_VALUE) {
					Log.e(TAG, "Exploration WAV data chunk is too large");
					return null;
				}
				dataOffset = chunkData;
				dataLength = (int) chunkLength;
			}
			offset = (int) (chunkEnd + (chunkLength & 1));
		}

		if (sampleRate == 0 || channels == 0 || bitsPerSample == 0 || blockAlign == 0
				|| dataOffset < 0 || dataLength == 0) {
			Log.e(TAG, "Exploration WAV is missing a format or data chunk");
			return null;
		}
		return new PcmWavData(wav, sampleRate, channels, bitsPerSample, blockAlign, dataOffset, dataLength);
	}

	private void writeExplorationLoop(
			AudioTrack track, byte[] data, int dataStart, int loopStart, int dataEnd) {
		Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
		int position = dataStart;
		while (explorationWriterRunning && explorationTrack == track) {
			if (position >= dataEnd) position = loopStart;
			int length = Math.min(EXPLORATION_WRITE_CHUNK_BYTES, dataEnd - position);
			length -= length % MUSIC_BYTES_PER_FRAME;
			int written = track.write(data, position, length);
			if (written <= 0) {
				if (explorationWriterRunning && explorationTrack == track) {
					Log.e(TAG, "Exploration audio stream write failed: " + written);
				}
				return;
			}
			position += written;
		}
	}

	private void pauseCurrentMusic() {
		if (musicPlayer != null && musicPlayer.isPlaying()) musicPlayer.pause();
		if (explorationTrack != null
				&& explorationTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
			explorationTrack.pause();
		}
	}

	private static boolean hasFourCc(byte[] data, int offset, String fourCc) {
		if (offset < 0 || offset > data.length - 4) return false;
		for (int i = 0; i < 4; i++) {
			if (data[offset + i] != (byte) fourCc.charAt(i)) return false;
		}
		return true;
	}

	private static int readUnsignedLittleEndianShort(byte[] data, int offset) {
		return (data[offset] & 0xff) | ((data[offset + 1] & 0xff) << 8);
	}

	private static long readUnsignedLittleEndianInt(byte[] data, int offset) {
		return (data[offset] & 0xffL)
				| ((data[offset + 1] & 0xffL) << 8)
				| ((data[offset + 2] & 0xffL) << 16)
				| ((data[offset + 3] & 0xffL) << 24);
	}

	private boolean onMusicPlayerError(MediaPlayer player, int what, int extra) {
		Log.e(TAG, "Music playback failed (what " + what + ", extra " + extra + ")");
		releaseMusicPlayer();
		return true;
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
			pauseCurrentMusic();
		}
	}

	private void releaseMusicPlayer() {
		explorationWriterRunning = false;
		if (explorationWriterThread != null) {
			explorationWriterThread.interrupt();
			explorationWriterThread = null;
		}
		if (explorationTrack != null) {
			explorationTrack.pause();
			explorationTrack.flush();
			explorationTrack.release();
			explorationTrack = null;
		}
		if (musicPlayer != null) {
			musicPlayer.release();
			musicPlayer = null;
		}
		currentMusicResource = 0;
	}

	private static final class PcmWavData {
		private final byte[] data;
		private final int sampleRate;
		private final int channels;
		private final int bitsPerSample;
		private final int blockAlign;
		private final int dataOffset;
		private final int dataLength;

		private PcmWavData(
				byte[] data, int sampleRate, int channels, int bitsPerSample,
				int blockAlign, int dataOffset, int dataLength) {
			this.data = data;
			this.sampleRate = sampleRate;
			this.channels = channels;
			this.bitsPerSample = bitsPerSample;
			this.blockAlign = blockAlign;
			this.dataOffset = dataOffset;
			this.dataLength = dataLength;
		}
	}
}
