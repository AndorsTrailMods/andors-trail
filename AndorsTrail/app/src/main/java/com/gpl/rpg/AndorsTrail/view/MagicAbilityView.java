package com.gpl.rpg.AndorsTrail.view;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.gpl.rpg.AndorsTrail.AndorsTrailApplication;
import com.gpl.rpg.AndorsTrail.AndorsTrailPreferences;
import com.gpl.rpg.AndorsTrail.R;
import com.gpl.rpg.AndorsTrail.activity.MainActivity;
import com.gpl.rpg.AndorsTrail.context.ControllerContext;
import com.gpl.rpg.AndorsTrail.context.WorldContext;
import com.gpl.rpg.AndorsTrail.controller.listeners.ActorStatsListener;
import com.gpl.rpg.AndorsTrail.controller.listeners.CombatSelectionListener;
import com.gpl.rpg.AndorsTrail.controller.listeners.CombatTurnListener;
import com.gpl.rpg.AndorsTrail.model.ability.SkillCollection;
import com.gpl.rpg.AndorsTrail.model.actor.Actor;
import com.gpl.rpg.AndorsTrail.model.actor.Monster;
import com.gpl.rpg.AndorsTrail.model.actor.Player;
import com.gpl.rpg.AndorsTrail.util.Coord;

public final class MagicAbilityView extends HorizontalScrollView
		implements ActorStatsListener, CombatSelectionListener, CombatTurnListener {
	private final WorldContext world;
	private final ControllerContext controllers;
	private final LinearLayout abilityPanel;
	private final Button crowFormButton;
	private final Button ravenMarkButton;
	private final Button murderOfCrowsButton;

	public MagicAbilityView(Context context, AttributeSet attrs) {
		super(context, attrs);
		AndorsTrailApplication app = AndorsTrailApplication.getApplicationFromActivityContext(context);
		world = app.getWorld();
		controllers = app.getControllerContext();
		setHorizontalScrollBarEnabled(false);
		setFillViewport(false);

		Resources resources = getResources();
		abilityPanel = new LinearLayout(context);
		abilityPanel.setOrientation(LinearLayout.HORIZONTAL);
		abilityPanel.setBackgroundColor(resources.getColor(android.R.color.transparent));
		addView(abilityPanel, new HorizontalScrollView.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		crowFormButton = createAbilityButton(context, v -> {
			controllers.combatController.primeCrowForm();
			refreshAbilities();
			requestMainViewFocus();
		});
		ravenMarkButton = createAbilityButton(context, v -> {
			controllers.combatController.castRavenMark(world.model.uiSelections.selectedMonster);
			refreshAbilities();
			requestMainViewFocus();
		});
		murderOfCrowsButton = createAbilityButton(context, v -> {
			controllers.combatController.castMurderOfCrows();
			refreshAbilities();
			requestMainViewFocus();
		});
		refreshAbilities();
	}

	private Button createAbilityButton(Context context, OnClickListener listener) {
		Button button = new Button(context, null, R.attr.buttonQuickslotsButtonStyle);
		button.setSingleLine(true);
		button.setEllipsize(TextUtils.TruncateAt.END);
		button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
		button.setMinWidth((int) (176f * getResources().getDisplayMetrics().density));
		button.setMinHeight((int) (56f * getResources().getDisplayMetrics().density));
		button.setPadding((int) (12f * getResources().getDisplayMetrics().density),
				button.getPaddingTop(), (int) (12f * getResources().getDisplayMetrics().density),
				button.getPaddingBottom());
		button.setOnClickListener(listener);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		params.setMargins((int) (3f * getResources().getDisplayMetrics().density), 0,
				(int) (3f * getResources().getDisplayMetrics().density), 0);
		abilityPanel.addView(button, params);
		return button;
	}

	private void requestMainViewFocus() {
		if (getContext() instanceof MainActivity) {
			MainView mainView = ((MainActivity) getContext()).getMainView();
			mainView.post(mainView::requestFocus);
		}
	}

	public void refreshAbilities() {
		Player player = world.model.player;
		int crowFormLevel = player.getSkillLevel(SkillCollection.SkillID.crowForm);
		int ravenMarkLevel = player.getSkillLevel(SkillCollection.SkillID.ravenMark);
		int murderLevel = player.getSkillLevel(SkillCollection.SkillID.murderOfCrows);
		crowFormButton.setVisibility(crowFormLevel > 0 ? View.VISIBLE : View.GONE);
		ravenMarkButton.setVisibility(ravenMarkLevel > 0 ? View.VISIBLE : View.GONE);
		murderOfCrowsButton.setVisibility(murderLevel > 0 ? View.VISIBLE : View.GONE);

		boolean playerTurn = world.model.uiSelections.isInCombat
				&& world.model.uiSelections.isPlayersCombatTurn;
		int crowFormCost = SkillCollection.getCrowFormManaCost(crowFormLevel);
		if (player.crowFormPrimed) {
			crowFormButton.setText(R.string.combat_crowform_primed);
		} else if (player.crowFormCooldownTurns > 0) {
			crowFormButton.setText(getResources().getString(
					R.string.combat_crowform_cooldown, player.crowFormCooldownTurns));
		} else {
			crowFormButton.setText(getResources().getString(
					R.string.combat_crowform, crowFormCost));
		}
		crowFormButton.setEnabled(playerTurn && !player.crowFormPrimed
				&& player.crowFormCooldownTurns == 0 && player.mana.current >= crowFormCost);

		int ravenMarkCost = SkillCollection.RAVEN_MARK_MANA_COST;
		int ravenCooldown = controllers.combatController.getRavenMarkCooldownTurns();
		Monster markedTarget = controllers.combatController.getRavenMarkedTarget();
		if (ravenCooldown > 0) {
			ravenMarkButton.setText(getResources().getString(
					R.string.combat_ravenmark_cooldown, ravenCooldown));
		} else if (markedTarget != null) {
			ravenMarkButton.setText(getResources().getString(R.string.combat_ravenmark_active,
					ravenMarkCost, markedTarget.monsterType.name));
		} else {
			ravenMarkButton.setText(getResources().getString(
					R.string.combat_ravenmark, ravenMarkCost));
		}
		ravenMarkButton.setEnabled(playerTurn && markedTarget == null && ravenCooldown == 0
				&& world.model.uiSelections.selectedMonster != null
				&& player.mana.current >= ravenMarkCost);

		int murderCooldown = controllers.combatController.getMurderOfCrowsCooldownTurns();
		if (murderCooldown > 0) {
			murderOfCrowsButton.setText(getResources().getString(
					R.string.combat_murderofcrows_cooldown, murderCooldown));
		} else {
			murderOfCrowsButton.setText(getResources().getString(
					R.string.combat_murderofcrows, SkillCollection.MURDER_OF_CROWS_MANA_COST));
		}
		murderOfCrowsButton.setEnabled(playerTurn && murderCooldown == 0
				&& controllers.combatController.hasAdjacentAggressiveMonsters()
				&& player.mana.current >= SkillCollection.MURDER_OF_CROWS_MANA_COST);
	}

	public void setPosition() {
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		switch (controllers.preferences.quickslotsPosition) {
			case AndorsTrailPreferences.QUICKSLOTS_POSITION_HORIZONTAL_BOTTOM_RIGHT:
			case AndorsTrailPreferences.QUICKSLOTS_POSITION_VERTICAL_BOTTOM_RIGHT:
				params.addRule(RelativeLayout.ALIGN_RIGHT, R.id.main_mainview);
				params.addRule(RelativeLayout.ALIGN_BOTTOM, R.id.main_mainview);
				break;
			case AndorsTrailPreferences.QUICKSLOTS_POSITION_HORIZONTAL_CENTER_BOTTOM:
				params.addRule(RelativeLayout.CENTER_HORIZONTAL);
				params.addRule(RelativeLayout.ALIGN_BOTTOM, R.id.main_mainview);
				break;
			case AndorsTrailPreferences.QUICKSLOTS_POSITION_VERTICAL_CENTER_LEFT:
				params.addRule(RelativeLayout.ALIGN_LEFT, R.id.main_mainview);
				params.addRule(RelativeLayout.CENTER_VERTICAL);
				break;
			case AndorsTrailPreferences.QUICKSLOTS_POSITION_VERTICAL_CENTER_RIGHT:
				params.addRule(RelativeLayout.ALIGN_RIGHT, R.id.main_mainview);
				params.addRule(RelativeLayout.CENTER_VERTICAL);
				break;
			default:
				params.addRule(RelativeLayout.ALIGN_LEFT, R.id.main_mainview);
				params.addRule(RelativeLayout.ALIGN_BOTTOM, R.id.main_mainview);
				break;
		}
		setLayoutParams(params);
	}

	public void subscribe() {
		controllers.actorStatsController.actorStatsListeners.add(this);
		controllers.combatController.combatSelectionListeners.add(this);
		controllers.combatController.combatTurnListeners.add(this);
	}

	public void unsubscribe() {
		controllers.actorStatsController.actorStatsListeners.remove(this);
		controllers.combatController.combatSelectionListeners.remove(this);
		controllers.combatController.combatTurnListeners.remove(this);
	}

	@Override public void onActorHealthChanged(Actor actor) { refreshAbilities(); }
	@Override public void onActorAPChanged(Actor actor) { }
	@Override public void onActorMPChanged(Player player) { refreshAbilities(); }
	@Override public void onActorAttackCostChanged(Actor actor, int newAttackCost) { }
	@Override public void onActorMoveCostChanged(Actor actor, int newMoveCost) { }
	@Override public void onPlayerReequipCostChanged(Player actor, int newAttackCost) { }
	@Override public void onPlayerUseCostChanged(Player actor, int newMoveCost) { }
	@Override public void onMonsterSelected(Monster monster, Coord selectedPosition, Coord previousSelection) { refreshAbilities(); }
	@Override public void onMovementDestinationSelected(Coord selectedPosition, Coord previousSelection) { refreshAbilities(); }
	@Override public void onCombatSelectionCleared(Coord previousSelection) { refreshAbilities(); }
	@Override public void onCombatStarted() { refreshAbilities(); }
	@Override public void onCombatEnded() { refreshAbilities(); }
	@Override public void onNewPlayerTurn() { refreshAbilities(); }
	@Override public void onMonsterIsAttacking(Monster monster) { refreshAbilities(); }
}
