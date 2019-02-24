package ch.bergturbenthal.home.touch.domain.renderer;

import java.awt.geom.Path2D;

public class Shapes {

  public static final Path2D.Double RIGHT_ARROW;
  public static final Path2D.Double LEFT_ARROW;
  public static final Path2D.Double UP_ARROW;
  public static final Path2D.Double DOWN_ARROW;

  static {
    RIGHT_ARROW = new Path2D.Double();
    RIGHT_ARROW.moveTo(1, 0);
    for (int i = 1; i < 3; i++) {
      final double x = Math.cos(i * 2 * Math.PI / 3);
      final double y = Math.sin(i * 2 * Math.PI / 3);
      RIGHT_ARROW.lineTo(x, y);
    }
    RIGHT_ARROW.closePath();

    LEFT_ARROW = new Path2D.Double();
    LEFT_ARROW.moveTo(-1, 0);
    for (int i = 1; i < 3; i++) {
      final double x = -Math.cos(i * 2 * Math.PI / 3);
      final double y = Math.sin(i * 2 * Math.PI / 3);
      LEFT_ARROW.lineTo(x, y);
    }
    LEFT_ARROW.closePath();

    UP_ARROW = new Path2D.Double();
    UP_ARROW.moveTo(0, -1);
    for (int i = 1; i < 3; i++) {
      final double x = Math.sin(i * 2 * Math.PI / 3);
      final double y = -Math.cos(i * 2 * Math.PI / 3);
      UP_ARROW.lineTo(x, y);
    }
    UP_ARROW.closePath();

    DOWN_ARROW = new Path2D.Double();
    DOWN_ARROW.moveTo(0, 1);
    for (int i = 1; i < 3; i++) {
      final double x = Math.sin(i * 2 * Math.PI / 3);
      final double y = Math.cos(i * 2 * Math.PI / 3);
      DOWN_ARROW.lineTo(x, y);
    }
    DOWN_ARROW.closePath();

  }
}
