package com.group_finity.mascot.mac;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.util.ArrayList;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import com.group_finity.mascot.environment.Area;
import com.group_finity.mascot.environment.Environment;

/**
 * Java �ł͎擾�����������AppleScript���g�p���Ď擾����.
 */
class MacEnvironment extends Environment {

  /**
    In mac environment, I think getting the frontmost window is easier
    than specific applications' window (such as Chrome).

    So, In this class, I implement getting the frontmost window, and I
    use "frontmostWindow" for alias of "activeIE".
   */
	private static Area activeIE = new Area();
  private static Area frontmostWindow = activeIE;

	private static final long screenWidth =
		Math.round(Toolkit.getDefaultToolkit().getScreenSize().getWidth());
	private static final long screenHeight =
		Math.round(Toolkit.getDefaultToolkit().getScreenSize().getHeight());

  private static ScriptEngine engine = new ScriptEngineManager().getEngineByName("AppleScript");

  private static Rectangle getFrontmostAppRect() {
    ArrayList<Long> bounds = null;

    try {
      bounds = (ArrayList<Long>) engine.eval(getFrontmostAppRectScript());
    } catch (ScriptException e) {}

    if (bounds != null && bounds.size() == 4) {
      return rectangleFromBounds(bounds);
    } else {
      return null;
    }
  }

	private static void moveFrontmostWindow(final Point point) {
		try {
			engine.eval(moveFrontmostWindowScript(point));
		} catch (ScriptException e) {}
	}

  private static String getFrontmostAppRectScript() {
    return "tell application \"System Events\"\n" +
           "  set appName to name of first item of (processes whose frontmost is true)\n" +
           "end tell\n" +
           "return bounds of first window of application appName";
  }

	private static String moveFrontmostWindowScript(final Point point) {
    return
			"tell application \"System Events\"\n" +
			"  set appName to name of first item of (processes whose frontmost is true)\n" +
			"end tell\n" +
			"set w to first window of application appName\n" +
			"set x to " + Double.toString(point.getX()) + "\n" +
			"set y to " + Double.toString(point.getY()) + "\n" +
			"set {x1, y1, x2, y2} to bounds of w\n" +
			"set bounds of w to {x, y, x+(x2-x1), y+(y2-y1)}";
	}

	private static String getWindowVisibleBoundsScript() {
		/**
			�X�N���[���̑傫���̎擾�� Finder ���g�����@�����邪�A
			�Z���ԂɌJ��Ԃ��Ăяo���� Finder �� CPU �g�p�����オ���āA
			���̂܂܉�����Ȃ��Ȃ�̂ŁA
			Java ���x���Ŏ擾���������g��
		 */
		return
			"set x1 to 0\n" +
			"set y1 to 0\n" +
			"set x2 to " + Long.toString(getScreenWidth()) + "\n" +
			"set y2 to " + Long.toString(getScreenHeight()) + "\n" +
			"tell application \"System Events\"\n" +
			"  tell process \"Dock\"\n" +
			"    set {dw, dh} to size in list 1\n" +
			"  end tell\n" +
			"  tell dock preferences\n" +
			"    set edge to screen edge as string\n" +
			"  end tell\n" +
			"end tell\n" +
			"if edge = \"bottom\" then\n" +
			"  set y2 to y2 - dh\n" +
			"else if edge = \"right\" then\n" +
			"  set x2 to x2 - dw\n" +
			"else if edge = \"left\" then\n" +
			"  set x1 to x1 + dw\n" +
			"end if\n" +
			"{x1+1, y1+22, x2-1, y2-22}";
	}

  private static Rectangle rectangleFromBounds(ArrayList<Long> bounds) {
    int
      leftTopX     = bounds.get(0).intValue(), leftTopY     = bounds.get(1).intValue(),
      rightBottomX = bounds.get(2).intValue(), rightBottomY = bounds.get(3).intValue();
    return new Rectangle(
      leftTopX,
      leftTopY,
      rightBottomX - leftTopX,
      rightBottomY - leftTopY);
  }

	private static long getScreenWidth() {
		return screenWidth;
	}

	private static long getScreenHeight() {
		return screenHeight;
	}

	/**
		��ʓ��ŃE�B���h�E���ړ����Ă������Ԃ���Ȃ��͈͂� Rectangle �ŕԂ��B
		Mac �ł́A�E�B���h�E�����S�ɉ�ʊO�Ɉړ������悤�Ƃ���ƁA
		�E�B���h�E����ʓ��ɉ����Ԃ���Ă��܂��B
	 */
	private static Rectangle getWindowVisibleArea() {
		ArrayList<Long> bounds = null;

		try {
			bounds = (ArrayList<Long>) engine.eval(getWindowVisibleBoundsScript());
		} catch (ScriptException e) {}

		if (bounds != null && bounds.size() == 4) {
			return rectangleFromBounds(bounds);
		} else {
			return null;
		}
	}

  private void updateFrontmostWindow() {
    final Rectangle frontmostWindowRect = getFrontmostAppRect();

    frontmostWindow.setVisible(
      (frontmostWindowRect != null)
      && frontmostWindowRect.intersects(getWindowVisibleArea()));
    frontmostWindow.set(
      frontmostWindowRect == null ? new Rectangle(-1, -1, 0, 0) : frontmostWindowRect);
  }

	@Override
	public void tick() {
		super.tick();
    this.updateFrontmostWindow();
	}

	@Override
	public void moveActiveIE(final Point point) {
		/**
			�O�q�̂Ƃ���A���S�ɉ�ʊO�ֈړ����悤�Ƃ���Ɖ����Ԃ���邽�߁A
			���̂悤�Ȉʒu�̎w��ɑ΂��Ă͉\�Ȃ�����̈ړ��ɐ؂�ւ���B
		 */
		final Rectangle
			visibleRect = getWindowVisibleArea(),
			windowRect  = getFrontmostAppRect();

		final double
			minX = visibleRect.getMinX() - windowRect.getWidth(), // �������̐܂�Ԃ����W
			maxX = visibleRect.getMaxX(),													// �E�����̐܂�Ԃ����W
			minY = visibleRect.getMinY(),													// ������̐܂�Ԃ����W
																														// (���j���[�o�[����ւ͈ړ��ł��Ȃ�)
			maxY = visibleRect.getMaxY();													// �������̐܂�Ԃ����W

		double
			pX   = point.getX(),
			pY   = point.getY();

		// X�����̐܂�Ԃ�
		if (pX < minX) {
			pX = minX;
		} else if (pX > maxX) {
			pX = maxX;
		}

		// Y�����̐܂�Ԃ�
		if (pY < minY) {
			pY = minY;
		} else if (pY > maxY) {
			pY = maxY;
		}

		point.setLocation(pX, pY);
		moveFrontmostWindow(point);
	}

	@Override
	public void restoreIE() {
	}

	@Override
	public Area getWorkArea() {
		return getScreen();
	}

	@Override
	public Area getActiveIE() {
		return this.activeIE;
	}

}
