package com.gpl.rpg.AndorsTrail.controller;

public final class AttackResult {
	public final boolean isHit;
	public final boolean isCriticalHit;
	public final int damage;
	public final boolean targetDied;
	public final boolean isCrowForm;
	public final boolean counterTargetDied;
	public AttackResult(boolean isHit, boolean isCriticalHit, int damage, boolean targetDied) {
		this(isHit, isCriticalHit, damage, targetDied, false, false);
	}
	public AttackResult(boolean isHit, boolean isCriticalHit, int damage, boolean targetDied, boolean isCrowForm) {
		this(isHit, isCriticalHit, damage, targetDied, isCrowForm, false);
	}
	public AttackResult(boolean isHit, boolean isCriticalHit, int damage, boolean targetDied,
			boolean isCrowForm, boolean counterTargetDied) {
		this.isHit = isHit;
		this.isCriticalHit = isCriticalHit;
		this.damage = damage;
		this.targetDied = targetDied;
		this.isCrowForm = isCrowForm;
		this.counterTargetDied = counterTargetDied;
	}
	public static final AttackResult MISS = new AttackResult(false, false, 0, false);
	public static final AttackResult CROW_FORM = new AttackResult(false, false, 0, false, true);
}
