package com.gpl.rpg.AndorsTrail.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.gpl.rpg.AndorsTrail.R;
import com.gpl.rpg.AndorsTrail.model.ability.SkillCollection;
import com.gpl.rpg.AndorsTrail.model.actor.Player;

public final class MagicAbilityTreeView extends View {
	private static final float ROOT_Y = 42f;
	private static final float BRANCH_Y = 130f;
	private static final float COUNTER_Y = 218f;
	private static final float NODE_HEIGHT = 48f;
	private Player player;
	private OnSkillClickListener skillClickListener;
	private SkillCollection.SkillID touchStartedSkill;
	private SkillCollection.SkillID clickedSkill;

	private final Paint connectorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Paint nodePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
	private final float density;

	public MagicAbilityTreeView(Context context, AttributeSet attrs) {
		super(context, attrs);
		density = getResources().getDisplayMetrics().density;
		connectorPaint.setColor(0xff8aa9d6);
		connectorPaint.setStrokeWidth(2f * density);
		nodePaint.setColor(0xff252b39);
		borderPaint.setStyle(Paint.Style.STROKE);
		borderPaint.setStrokeWidth(2f * density);
		textPaint.setTextSize(14f * density);
		textPaint.setFakeBoldText(true);
		setContentDescription(getResources().getString(R.string.magic_tree_content_description));
	}

	public void setPlayer(Player player) {
		this.player = player;
		invalidate();
	}

	public void setOnSkillClickListener(OnSkillClickListener listener) {
		skillClickListener = listener;
		setClickable(listener != null);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		float width = getWidth();
		float centerX = width / 2f;
		float branchWidth = Math.min(142f * density, width * 0.44f);
		float rootWidth = Math.min(210f * density, width - 32f * density);
		float crowX = width * 0.26f;
		float ravenX = width * 0.74f;
		boolean manaRegenerationLearned = manaRegenerationLevel() > 0;
		boolean crowFormLearned = crowFormLevel() > 0;
		boolean ravenMarkLearned = ravenMarkLevel() > 0;
		boolean counterLearned = blackwingCounterLevel() > 0;
		boolean murderLearned = murderOfCrowsLevel() > 0;
		int prerequisiteColor = manaRegenerationLearned ? 0xff75bd91 : 0xff81b5ff;
		int crowColor = crowFormLearned ? 0xff75bd91
				: ravenMarkLearned ? 0xff778397
						: hasMageSkill() && manaRegenerationLearned ? 0xff81b5ff : 0xff778397;
		int ravenColor = ravenMarkLearned ? 0xff75bd91
				: crowFormLearned ? 0xff778397
						: hasMageSkill() && manaRegenerationLearned ? 0xff81b5ff : 0xff778397;
		int counterColor = counterLearned ? 0xff75bd91
				: murderLearned ? 0xff778397 : crowFormLearned ? 0xff81b5ff : 0xff778397;
		int murderColor = murderLearned ? 0xff75bd91
				: counterLearned ? 0xff778397 : ravenMarkLearned ? 0xff81b5ff : 0xff778397;

		drawNode(canvas, centerX, ROOT_Y * density, rootWidth, NODE_HEIGHT * density,
				0xff293c58, prerequisiteColor);
		drawText(canvas, getResources().getString(R.string.magic_tree_mana_regeneration),
				centerX, (ROOT_Y - 5f) * density, 0xffffffff);
		textPaint.setTextSize(11f * density);
		drawText(canvas, getResources().getString(R.string.magic_tree_skill_level,
				manaRegenerationLevel(), SkillCollection.MAX_LEVEL_MANA_REGENERATION),
				centerX, (ROOT_Y + 15f) * density, 0xffd2dced);
		textPaint.setTextSize(14f * density);

		float rootBottom = (ROOT_Y + NODE_HEIGHT / 2f) * density;
		float branchTop = (BRANCH_Y - NODE_HEIGHT / 2f) * density;
		canvas.drawLine(centerX, rootBottom, crowX, branchTop, connectorPaint);
		canvas.drawLine(centerX, rootBottom, ravenX, branchTop, connectorPaint);
		drawBranchNode(canvas, crowX, BRANCH_Y * density, branchWidth,
				R.string.magic_crowform_title, R.string.magic_tree_defensive_path,
				crowFormLevel(), SkillCollection.MAX_LEVEL_CROW_FORM, crowColor);
		drawBranchNode(canvas, ravenX, BRANCH_Y * density, branchWidth,
				R.string.magic_ravenmark_title, R.string.magic_tree_offensive_path,
				ravenMarkLevel(), SkillCollection.MAX_LEVEL_RAVEN_MARK, ravenColor);

		float branchBottom = (BRANCH_Y + NODE_HEIGHT / 2f) * density;
		float pathTop = (COUNTER_Y - NODE_HEIGHT / 2f) * density;
		canvas.drawLine(crowX, branchBottom, crowX, pathTop, connectorPaint);
		canvas.drawLine(ravenX, branchBottom, ravenX, pathTop, connectorPaint);
		drawSkillNode(canvas, crowX, COUNTER_Y * density, branchWidth,
				R.string.magic_blackwing_counter_title, blackwingCounterLevel(),
				SkillCollection.MAX_LEVEL_BLACKWING_COUNTER, counterColor);
		drawSkillNode(canvas, ravenX, COUNTER_Y * density, branchWidth,
				R.string.magic_murder_of_crows_title, murderOfCrowsLevel(),
				SkillCollection.MAX_LEVEL_MURDER_OF_CROWS, murderColor);
	}

	private void drawSkillNode(Canvas canvas, float x, float y, float width,
			int titleResource, int level, int maxLevel, int color) {
		drawNode(canvas, x, y, width, NODE_HEIGHT * density, 0xff252b39, color);
		drawText(canvas, getResources().getString(titleResource),
				x, y - 3f * density, 0xffffffff);
		textPaint.setTextSize(11f * density);
		drawText(canvas, getResources().getString(R.string.magic_tree_skill_level, level, maxLevel),
				x, y + 14f * density, 0xffd2dced);
		textPaint.setTextSize(14f * density);
	}

	private void drawBranchNode(Canvas canvas, float x, float y, float width,
			int titleResource, int pathResource, int level, int maxLevel, int color) {
		drawNode(canvas, x, y, width, NODE_HEIGHT * density, 0xff252b39, color);
		textPaint.setTextSize(9f * density);
		drawText(canvas, getResources().getString(pathResource), x, y - 10f * density, 0xffd2dced);
		textPaint.setTextSize(13f * density);
		drawText(canvas, getResources().getString(titleResource), x, y + 3f * density, 0xffffffff);
		textPaint.setTextSize(10f * density);
		drawText(canvas, getResources().getString(R.string.magic_tree_skill_level, level, maxLevel),
				x, y + 17f * density, 0xffd2dced);
		textPaint.setTextSize(14f * density);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (!isClickable()) return false;
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			touchStartedSkill = skillAtPosition(event.getX(), event.getY());
			return touchStartedSkill != null;
		case MotionEvent.ACTION_UP:
			SkillCollection.SkillID touchedSkill = touchStartedSkill;
			touchStartedSkill = null;
			if (touchedSkill == null) return false;
			if (touchedSkill == skillAtPosition(event.getX(), event.getY())) {
				clickedSkill = touchedSkill;
				performClick();
				clickedSkill = null;
			}
			return true;
		case MotionEvent.ACTION_CANCEL:
			touchStartedSkill = null;
			return false;
		default:
			return touchStartedSkill != null;
		}
	}

	private SkillCollection.SkillID skillAtPosition(float x, float y) {
		if (isInsideManaRegenerationNode(x, y)) return SkillCollection.SkillID.manaRegeneration;
		if (isInsideCrowFormNode(x, y)) return SkillCollection.SkillID.crowForm;
		if (isInsideRavenMarkNode(x, y)) return SkillCollection.SkillID.ravenMark;
		if (isInsideBlackwingCounterNode(x, y)) return SkillCollection.SkillID.blackwingCounter;
		if (isInsideMurderOfCrowsNode(x, y)) return SkillCollection.SkillID.murderOfCrows;
		return null;
	}

	private boolean isInsideManaRegenerationNode(float x, float y) {
		float rootWidth = Math.min(210f * density, getWidth() - 32f * density);
		return Math.abs(x - getWidth() / 2f) <= rootWidth / 2f
				&& Math.abs(y - ROOT_Y * density) <= NODE_HEIGHT * density / 2f;
	}

	private boolean isInsideCrowFormNode(float x, float y) {
		float nodeWidth = Math.min(142f * density, getWidth() * 0.44f);
		float centerX = getWidth() * 0.26f;
		return Math.abs(x - centerX) <= nodeWidth / 2f
				&& Math.abs(y - BRANCH_Y * density) <= NODE_HEIGHT * density / 2f;
	}

	private boolean isInsideRavenMarkNode(float x, float y) {
		float nodeWidth = Math.min(142f * density, getWidth() * 0.44f);
		float centerX = getWidth() * 0.74f;
		return Math.abs(x - centerX) <= nodeWidth / 2f
				&& Math.abs(y - BRANCH_Y * density) <= NODE_HEIGHT * density / 2f;
	}

	private boolean isInsideBlackwingCounterNode(float x, float y) {
		float nodeWidth = Math.min(142f * density, getWidth() * 0.44f);
		float centerX = getWidth() * 0.26f;
		return Math.abs(x - centerX) <= nodeWidth / 2f
				&& Math.abs(y - COUNTER_Y * density) <= NODE_HEIGHT * density / 2f;
	}

	private boolean isInsideMurderOfCrowsNode(float x, float y) {
		float nodeWidth = Math.min(142f * density, getWidth() * 0.44f);
		float centerX = getWidth() * 0.74f;
		return Math.abs(x - centerX) <= nodeWidth / 2f
				&& Math.abs(y - COUNTER_Y * density) <= NODE_HEIGHT * density / 2f;
	}

	private int manaRegenerationLevel() {
		return player == null ? 0 : player.getSkillLevel(SkillCollection.SkillID.manaRegeneration);
	}

	private boolean manaRegenerationLearned() {
		return manaRegenerationLevel() > 0;
	}

	private boolean hasMageSkill() {
		return player != null && player.getSkillLevel(SkillCollection.SkillID.fightstyleMage) > 0;
	}

	private int crowFormLevel() {
		return player == null ? 0 : player.getSkillLevel(SkillCollection.SkillID.crowForm);
	}

	private int ravenMarkLevel() {
		return player == null ? 0 : player.getSkillLevel(SkillCollection.SkillID.ravenMark);
	}

	private int blackwingCounterLevel() {
		return player == null ? 0 : player.getSkillLevel(SkillCollection.SkillID.blackwingCounter);
	}

	private int murderOfCrowsLevel() {
		return player == null ? 0 : player.getSkillLevel(SkillCollection.SkillID.murderOfCrows);
	}

	@Override
	public boolean performClick() {
		super.performClick();
		if (skillClickListener != null) {
			skillClickListener.onSkillClicked(
					clickedSkill == null ? SkillCollection.SkillID.manaRegeneration : clickedSkill);
		}
		return true;
	}

	private void drawNode(Canvas canvas, float centerX, float centerY, float width,
			float height, int fillColor, int borderColor) {
		RectF box = new RectF(centerX - width / 2f, centerY - height / 2f,
				centerX + width / 2f, centerY + height / 2f);
		nodePaint.setColor(fillColor);
		canvas.drawRoundRect(box, 7f * density, 7f * density, nodePaint);
		borderPaint.setColor(borderColor);
		canvas.drawRoundRect(box, 7f * density, 7f * density, borderPaint);
	}

	private void drawText(Canvas canvas, String text, float centerX, float baseline, int color) {
		textPaint.setColor(color);
		textPaint.setTextAlign(Paint.Align.CENTER);
		canvas.drawText(text, centerX, baseline, textPaint);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		float treeHeight = COUNTER_Y + NODE_HEIGHT / 2f + 16f;
		int desiredHeight = (int) (treeHeight * density);
		int height = resolveSize(desiredHeight, heightMeasureSpec);
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int width = widthMode == MeasureSpec.UNSPECIFIED
				? (int) (320f * density)
				: MeasureSpec.getSize(widthMeasureSpec);
		setMeasuredDimension(width, height);
	}

	public interface OnSkillClickListener {
		void onSkillClicked(SkillCollection.SkillID skillID);
	}
}
