package com.gpl.rpg.AndorsTrail.mod;

import com.gpl.rpg.AndorsTrail.context.WorldContext;
import com.gpl.rpg.AndorsTrail.model.actor.MonsterType;
import com.gpl.rpg.AndorsTrail.model.actor.Player;
import com.gpl.rpg.AndorsTrail.model.item.Inventory;
import com.gpl.rpg.AndorsTrail.model.item.ItemContainer;
import com.gpl.rpg.AndorsTrail.model.item.ItemType;

public final class SoulEaterEvolution {
	public static final String QUEST_ID = "soul_eater_bounties";

	private static final String[] FORM_IDS = {
		"soul_eater_dormant",
		"soul_eater_base",
		"soul_eater_honed",
		"soul_eater_awakened",
		"soul_eater_godly",
		"soul_eater_evolved_base",
		"soul_eater_prime_honed",
		"soul_eater_prime_awakened",
		"soul_eater_prime_ascendant",
		"soul_eater_prime_eternal"
	};
	private static final int[] KILLS_PER_FAMILY = { 25, 25, 25, 50 };
	public static final int POWER_CAP_MILESTONE = FORM_IDS.length - 1;

	private SoulEaterEvolution() {}

	public static boolean isSoulEater(ItemType item) {
		if (item == null) return false;
		for (String id : FORM_IDS) {
			if (id.equals(item.id)) return true;
		}
		return false;
	}

	public static int getUnlockedMilestones(WorldContext world, Player player) {
		if (world == null || world.model == null || player == null) return 0;
		int milestone = 1;
		while (player.level >= requiredLevel(milestone)) {
			int familyIndex = (milestone - 1) % KILLS_PER_FAMILY.length;
			int familyCycle = (milestone - 1) / KILLS_PER_FAMILY.length + 1;
			long requiredKills = (long) KILLS_PER_FAMILY[familyIndex] * familyCycle;
			if (getFamilyKillCount(world, familyIndex) < requiredKills) break;
			if (milestone == Integer.MAX_VALUE) break;
			++milestone;
		}
		return milestone - 1;
	}

	public static boolean isCurrentBountyTarget(WorldContext world, Player player, MonsterType monster) {
		if (world == null || world.model == null || player == null || monster == null) return false;
		if (!isSoulEater(player.inventory.getItemTypeInWearSlot(Inventory.WearSlot.weapon))) return false;
		int milestone = getUnlockedMilestones(world, player) + 1;
		int familyIndex = (milestone - 1) % KILLS_PER_FAMILY.length;
		long requiredKills = getRequiredBountyKills(milestone);
		return matchesFamily(monster, familyIndex) && getFamilyKillCount(world, familyIndex) < requiredKills;
	}

	public static String getCurrentBountyName(WorldContext world, Player player) {
		int familyIndex = (getUnlockedMilestones(world, player)) % KILLS_PER_FAMILY.length;
		switch (familyIndex) {
		case 0: return "rats and insects";
		case 1: return "Izthiels";
		case 2: return "wyrms";
		case 3: return "demons and cultists";
		default: throw new IllegalStateException("Unknown Soul-Eater bounty family: " + familyIndex);
		}
	}

	public static int getCurrentBountyKills(WorldContext world, Player player) {
		int familyIndex = getUnlockedMilestones(world, player) % KILLS_PER_FAMILY.length;
		return (int) Math.min(Integer.MAX_VALUE, getFamilyKillCount(world, familyIndex));
	}

	public static int getCurrentBountyRequiredKills(WorldContext world, Player player) {
		return (int) Math.min(Integer.MAX_VALUE, getRequiredBountyKills(getUnlockedMilestones(world, player) + 1));
	}

	public static int getLevelRequiredForNextMilestone(WorldContext world, Player player) {
		return (int) Math.min(Integer.MAX_VALUE, requiredLevel(getUnlockedMilestones(world, player) + 1));
	}

	public static boolean isCombatPowerCapped(WorldContext world, Player player) {
		return getUnlockedMilestones(world, player) >= POWER_CAP_MILESTONE;
	}

	/**
	 * Evolutions use lifetime monster statistics, so no new save-game fields are needed.
	 * The family and level requirements repeat forever; combat bonuses stop increasing
	 * after the four tiers following the Prime evolution.
	 */
	private static long requiredLevel(int milestone) {
		return milestone == 1 ? 1 : (long) milestone * 10;
	}

	private static long getFamilyKillCount(WorldContext world, int familyIndex) {
		long kills = 0;
		for (MonsterType monster : world.monsterTypes.getAllMonsterTypes()) {
			if (matchesFamily(monster, familyIndex)) {
				kills += world.model.statistics.getNumberOfKillsForMonsterType(monster.id);
			}
		}
		return kills;
	}

	private static long getRequiredBountyKills(int milestone) {
		int familyCycle = (milestone - 1) / KILLS_PER_FAMILY.length + 1;
		int familyIndex = (milestone - 1) % KILLS_PER_FAMILY.length;
		return (long) KILLS_PER_FAMILY[familyIndex] * familyCycle;
	}

	private static boolean matchesFamily(MonsterType monster, int familyIndex) {
		String id = monster.id.toLowerCase(java.util.Locale.ROOT);
		switch (familyIndex) {
		case 0:
			boolean ratID = id.contains("rat") && (id.startsWith("rat") || id.contains("_rat")
					|| id.endsWith("rat") || id.contains("caverat"));
			return ratID || monster.monsterClass == MonsterType.MonsterClass.insect;
		case 1:
			return id.contains("izthiel");
		case 2:
			return id.contains("wyrm");
		case 3:
			return id.contains("cult") || monster.monsterClass == MonsterType.MonsterClass.demon;
		default:
			throw new IllegalArgumentException("Unknown Soul-Eater bounty family: " + familyIndex);
		}
	}

	public static int checkAndEvolve(WorldContext world, Player player) {
		if (world == null || player == null) return -1;
		ItemType equipped = player.inventory.getItemTypeInWearSlot(Inventory.WearSlot.weapon);
		if (!isSoulEater(equipped)) return -1;

		int milestones = getUnlockedMilestones(world, player);
		int formIndex = Math.min(milestones, FORM_IDS.length - 1);
		if (equipped.id.equals(FORM_IDS[formIndex])) return milestones;

		ItemType evolvedForm = world.itemTypes.getItemType(FORM_IDS[formIndex]);
		if (evolvedForm == null) {
			throw new IllegalStateException("Missing Soul-Eater form: " + FORM_IDS[formIndex]);
		}

		player.inventory.setItemTypeInWearSlot(Inventory.WearSlot.weapon, evolvedForm);
		for (int i = 0; i < player.inventory.items.size(); ++i) {
			ItemContainer.ItemEntry entry = player.inventory.items.get(i);
			if (entry.itemType.id.equals(equipped.id)) {
				player.inventory.items.set(i, new ItemContainer.ItemEntry(evolvedForm, entry.quantity));
				break;
			}
		}
		return milestones;
	}

	public static boolean hasGodlyKillEffect(Player player) {
		ItemType equipped = player.inventory.getItemTypeInWearSlot(Inventory.WearSlot.weapon);
		if (equipped == null) return false;
		for (int i = 4; i < FORM_IDS.length; ++i) {
			if (FORM_IDS[i].equals(equipped.id)) return true;
		}
		return false;
	}
}
