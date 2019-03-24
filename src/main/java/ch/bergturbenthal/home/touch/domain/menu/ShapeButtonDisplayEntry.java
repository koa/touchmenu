package ch.bergturbenthal.home.touch.domain.menu;

import ch.bergturbenthal.home.touch.domain.renderer.DisplayRenderer;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public class ShapeButtonDisplayEntry implements DisplayEntry {
  private final Shape                       shape;
  private final DisplayRenderer             renderer;
  private final DisplayRenderer.ZoneAddress position;
  private final Runnable                    actionCallback;

  ShapeButtonDisplayEntry(
          final Shape shape,
          final DisplayRenderer renderer,
          final DisplayRenderer.ZoneAddress position,
          final Runnable actionCallback) {
    this.shape = shape;
    this.renderer = renderer;
    this.position = position;
    this.actionCallback = actionCallback;
  }

  @Override
  public void draw() {
    renderer.fillShape(shape, position);
  }

  @Override
  public TouchResult handleTouch(final Point2D p) {
    final Rectangle2D touchZone = renderer.getTouchZone(position, 0);
    if (touchZone.contains(p)) {
      actionCallback.run();
      return TouchResult.NOOP;
    }
    return TouchResult.IGNORED;
  }
}
