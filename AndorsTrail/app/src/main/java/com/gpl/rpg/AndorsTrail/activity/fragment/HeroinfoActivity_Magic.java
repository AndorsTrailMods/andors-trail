package com.gpl.rpg.AndorsTrail.activity.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gpl.rpg.AndorsTrail.AndorsTrailApplication;
import com.gpl.rpg.AndorsTrail.Dialogs;
import com.gpl.rpg.AndorsTrail.R;
import com.gpl.rpg.AndorsTrail.context.ControllerContext;
import com.gpl.rpg.AndorsTrail.model.ability.SkillCollection;
import com.gpl.rpg.AndorsTrail.model.ability.SkillInfo;
import com.gpl.rpg.AndorsTrail.model.actor.Player;
import com.gpl.rpg.AndorsTrail.view.MagicAbilityTreeView;

public final class HeroinfoActivity_Magic extends Fragment {
	private static final int INTENTREQUEST_SKILLINFO = 27;
	private Player player;
	private ControllerContext controllers;
	private SkillCollection skills;
	private MagicAbilityTreeView abilityTree;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		AndorsTrailApplication app = AndorsTrailApplication.getApplicationFromActivity(getActivity());
		if (!app.isInitialized()) return;
		player = app.getWorld().model.player;
		controllers = app.getControllerContext();
		skills = app.getWorld().skills;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.heroinfo_magic, container, false);
		abilityTree = (MagicAbilityTreeView) view.findViewById(R.id.magic_ability_tree);
		abilityTree.setPlayer(player);
		abilityTree.setOnSkillClickListener(skillID -> {
			Intent intent = Dialogs.getIntentForSkillInfo(getActivity(), skillID);
			startActivityForResult(intent, INTENTREQUEST_SKILLINFO);
		});
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		updateMagicTree();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == INTENTREQUEST_SKILLINFO && resultCode == Activity.RESULT_OK && data != null) {
			SkillCollection.SkillID skillID = SkillCollection.SkillID.valueOf(data.getStringExtra("skillID"));
			SkillInfo skill = skills.getSkill(skillID);
			controllers.skillController.levelUpSkillManually(player, skill);
		}
		updateMagicTree();
	}

	private void updateMagicTree() {
		if (player == null) return;
		if (abilityTree != null) abilityTree.setPlayer(player);
	}
}
