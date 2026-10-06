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
	private static final int[] BRANCH_COUNTS = {1, 2, 3, 4};
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
		float rootY = 36f * density;
		float rootHeight = 46f * density;
		float firstRowY = 112f * density;
		float rowSpacing = 72f * density;
		float nodeHeight = 34f * density;
		float nodeWidth = Math.min(40f * density, width / 9f);
		float crowFormWidth = Math.min(122f * density, width * 0.42f);

		boolean manaRegenerationLearned = manaRegenerationLevel() > 0;
		drawNode(canvas, centerX, rootY, Math.min(190f * density, width - 32f * density),
				rootHeight, 0xff293c58, manaRegenerationLearned ? 0xff75bd91 : 0xff81b5ff);
		drawText(canvas, getResources().getString(R.string.magic_tree_mana_regeneration),
				centerX, rootY - 2f * density, 0xffffffff);
		textPaint.setTextSize(11f * density);
		drawText(canvas, getResources().getString(R.string.magic_tree_skill_level,
				manaRegenerationLevel(), SkillCollection.MAX_LEVEL_MANA_REGENERATION),
				centerX, rootY + 15f * density, 0xffd2dced);
		textPaint.setTextSize(14f * density);

		for (int row = 0; row < BRANCH_COUNTS.length; row++) {
			int countPerSide = BRANCH_COUNTS[row];
			float rowY = firstRowY + row * rowSpacing;
			for (int side = 0; side < 2; side++) {
				boolean leftSide = side == 0;
				for (int node = 0; node < countPerSide; node++) {
					float x = getNodeX(width, leftSide, countPerSide, node);
					if (row == 0) {
						canvas.drawLine(centerX, rootY + rootHeight / 2f,
								x, rowY - nodeHeight / 2f, connectorPaint);
					} else {
						int parentCount = BRANCH_COUNTS[row - 1];
						int parent = node * parentCount / countPerSide;
						float parentX = getNodeX(width, leftSide, parentCount, parent);
						float parentY = firstRowY + (row - 1) * rowSpacing + nodeHeight / 2f;
						canvas.drawLine(parentX, parentY, x, rowY - nodeHeight / 2f, connectorPaint);
					}

					boolean isCrowForm = row == 0 && leftSide;
					boolean crowFormLearned = crowFormLevel() > 0;
					int crowFormBorderColor = crowFormLearned ? 0xff75bd91
							: hasMageSkill() ? 0xff81b5ff : 0xff778397;
					drawNode(canvas, x, rowY, isCrowForm ? crowFormWidth : nodeWidth,
							nodeHeight, 0xff252b39, isCrowForm ? crowFormBorderColor : 0xff778397);
					if (isCrowForm) {
						drawText(canvas, getResources().getString(R.string.magic_crowform_title),
								x, rowY + textPaint.getTextSize() / 3f, 0xffffffff);
					}
				}
			}
		}
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
		return null;
	}

	private boolean isInsideManaRegenerationNode(float x, float y) {
		float rootWidth = Math.min(190f * density, getWidth() - 32f * density);
		return Math.abs(x - getWidth() / 2f) <= rootWidth / 2f
				&& Math.abs(y - 36f * density) <= 23f * density;
	}

	private boolean isInsideCrowFormNode(float x, float y) {
		float nodeWidth = Math.min(122f * density, getWidth() * 0.42f);
		float centerX = getNodeX(getWidth(), true, 1, 0);
		return Math.abs(x - centerX) <= nodeWidth / 2f
				&& Math.abs(y - 112f * density) <= 17f * density;
	}

	private int manaRegenerationLevel() {
		return player == null ? 0 : player.getSkillLevel(SkillCollection.SkillID.manaRegeneration);
	}

	private int crowFormLevel() {
		return player == null ? 0 : player.getSkillLevel(SkillCollection.SkillID.crowForm);
	}

	private boolean hasMageSkill() {
		return player != null && player.getSkillLevel(SkillCollection.SkillID.fightstyleMage) > 0;
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

	private static float getNodeX(float width, boolean leftSide, int count, int node) {
		float min = leftSide ? 0.06f : 0.56f;
		float max = leftSide ? 0.44f : 0.94f;
		if (count == 1) return width * (min + max) / 2f;
		return width * (min + (max - min) * node / (count - 1f));
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
		float treeHeight = 112f + (BRANCH_COUNTS.length - 1) * 72f + 34f / 2f + 16f;
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
