package com.gpl.rpg.AndorsTrail.audio;

import java.util.Locale;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;

import com.gpl.rpg.AndorsTrail.R;
import com.gpl.rpg.AndorsTrail.model.actor.Actor;
import com.gpl.rpg.AndorsTrail.model.actor.Monster;

public final class MikhailVoicePlayer {
	private static final String TAG = "MikhailVoicePlayer";
	private static final float VOICE_VOLUME = 0.85f;

	private final Context context;
	private MediaPlayer mediaPlayer;

	public MikhailVoicePlayer(Context context) {
		this.context = context.getApplicationContext();
	}

	public void play(Actor speaker, String phraseID) {
		stop();
		if (!isMikhail(speaker) || !isEnglishDialogue() || phraseID == null) return;

		String resourceName = "voice_mikhail_" + phraseID.toLowerCase(Locale.US);
		int resourceID = context.getResources().getIdentifier(resourceName, "raw", context.getPackageName());
		if (resourceID == 0) {
			Log.w(TAG, "No voice resource for Mikhail phrase " + phraseID);
			return;
		}

		MediaPlayer nextPlayer = MediaPlayer.create(context, resourceID);
		if (nextPlayer == null) {
			Log.e(TAG, "Unable to create voice player for Mikhail phrase " + phraseID);
			return;
		}

		nextPlayer.setVolume(VOICE_VOLUME, VOICE_VOLUME);
		nextPlayer.setOnCompletionListener(player -> releasePlayer(player));
		nextPlayer.setOnErrorListener((player, what, extra) -> {
			Log.e(TAG, "Voice playback failed for Mikhail phrase " + phraseID + " (what " + what + ", extra " + extra + ")");
			releasePlayer(player);
			return true;
		});
		mediaPlayer = nextPlayer;
		try {
			nextPlayer.start();
		} catch (IllegalStateException e) {
			Log.e(TAG, "Unable to start voice playback for Mikhail phrase " + phraseID, e);
			releasePlayer(nextPlayer);
		}
	}

	public void stop() {
		if (mediaPlayer == null) return;
		MediaPlayer player = mediaPlayer;
		mediaPlayer = null;
		player.setOnCompletionListener(null);
		player.setOnErrorListener(null);
		player.release();
	}

	private void releasePlayer(MediaPlayer player) {
		if (mediaPlayer == player) mediaPlayer = null;
		player.release();
	}

	private boolean isEnglishDialogue() {
		return context.getString(R.string.localize_resources_from_mo_filename).trim().isEmpty();
	}

	private static boolean isMikhail(Actor actor) {
		if (!(actor instanceof Monster)) return false;
		Monster monster = (Monster) actor;
		String monsterTypeID = monster.getMonsterTypeID();
		return "mikhail".equals(monsterTypeID)
				|| "stn_colonel_mons3".equals(monsterTypeID)
				|| "stn_colonel_mons3b".equals(monsterTypeID)
				|| "Mikhail".equalsIgnoreCase(monster.getName());
	}
}
