package com.gpl.rpg.AndorsTrail.model.ability;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

public final class SkillCollectionTest {

	@Test
	public void crowAbilitiesRequireTheirMatchingBranchAbility() {
		SkillCollection skills = new SkillCollection();
		skills.initialize();

		assertEquals(SkillCollection.SkillID.crowForm.name(),
				skills.getSkill(SkillCollection.SkillID.blackwingCounter).levelupRequirements[0].skillOrStatID);
		assertEquals(SkillCollection.SkillID.ravenMark.name(),
				skills.getSkill(SkillCollection.SkillID.murderOfCrows).levelupRequirements[0].skillOrStatID);
	}

	@Test
	public void magicSkillsFollowTheirPurchaseProgression() {
		assertNull(SkillCollection.getPreviousMagicSkill(SkillCollection.SkillID.manaRegeneration));
		assertSame(SkillCollection.SkillID.manaRegeneration,
				SkillCollection.getPreviousMagicSkill(SkillCollection.SkillID.crowForm));
		assertNull(SkillCollection.getPreviousMagicSkill(SkillCollection.SkillID.ravenMark));
		assertNull(SkillCollection.getPreviousMagicSkill(SkillCollection.SkillID.blackwingCounter));
		assertNull(SkillCollection.getPreviousMagicSkill(SkillCollection.SkillID.murderOfCrows));
	}

	@Test
	public void ravenMarkBonusDamageScalesAndCapsAtMaximumLevel() {
		assertEquals(0, SkillCollection.getRavenMarkBonusDamage(0));
		assertEquals(2, SkillCollection.getRavenMarkBonusDamage(1));
		assertEquals(10, SkillCollection.getRavenMarkBonusDamage(SkillCollection.MAX_LEVEL_RAVEN_MARK));
		assertEquals(10, SkillCollection.getRavenMarkBonusDamage(SkillCollection.MAX_LEVEL_RAVEN_MARK + 1));
	}

	@Test
	public void blackwingCounterReflectsIncreasingShareOfPreventedDamage() {
		assertEquals(0, SkillCollection.getBlackwingCounterDamage(100, 0));
		assertEquals(5, SkillCollection.getBlackwingCounterDamage(100, 1));
		assertEquals(25, SkillCollection.getBlackwingCounterDamage(100, SkillCollection.MAX_LEVEL_BLACKWING_COUNTER));
		assertEquals(25, SkillCollection.getBlackwingCounterDamage(100, SkillCollection.MAX_LEVEL_BLACKWING_COUNTER + 1));
	}

	@Test
	public void crowBranchesAreMutuallyExclusive() {
		assertSame(SkillCollection.SkillID.ravenMark,
				SkillCollection.getExclusiveCrowBranchSkill(SkillCollection.SkillID.crowForm));
		assertSame(SkillCollection.SkillID.crowForm,
				SkillCollection.getExclusiveCrowBranchSkill(SkillCollection.SkillID.ravenMark));
		assertSame(SkillCollection.SkillID.murderOfCrows,
				SkillCollection.getExclusiveCrowBranchSkill(SkillCollection.SkillID.blackwingCounter));
		assertSame(SkillCollection.SkillID.blackwingCounter,
				SkillCollection.getExclusiveCrowBranchSkill(SkillCollection.SkillID.murderOfCrows));
		assertNull(SkillCollection.getExclusiveCrowBranchSkill(SkillCollection.SkillID.manaRegeneration));
	}

	@Test
	public void murderOfCrowsDamageScalesAndCapsAtMaximumLevel() {
		assertEquals(0, SkillCollection.getMurderOfCrowsDamage(0));
		assertEquals(2, SkillCollection.getMurderOfCrowsDamage(1));
		assertEquals(10, SkillCollection.getMurderOfCrowsDamage(SkillCollection.MAX_LEVEL_MURDER_OF_CROWS));
		assertEquals(10, SkillCollection.getMurderOfCrowsDamage(SkillCollection.MAX_LEVEL_MURDER_OF_CROWS + 1));
	}

	@Test
	public void crowFormManaCostAndCooldownIncreaseWithSkillLevel() {
		assertEquals(5, SkillCollection.getCrowFormManaCost(1));
		assertEquals(5, SkillCollection.getCrowFormManaCost(2));
		assertEquals(4, SkillCollection.getCrowFormManaCost(3));
		assertEquals(3, SkillCollection.getCrowFormManaCost(4));
		assertEquals(2, SkillCollection.getCrowFormManaCost(5));
		assertEquals(10, SkillCollection.getCrowFormCooldownTurns(1));
		assertEquals(2, SkillCollection.getCrowFormCooldownTurns(5));
		assertEquals(2, SkillCollection.getCrowFormUsesBeforeCooldown(5));
	}
}
