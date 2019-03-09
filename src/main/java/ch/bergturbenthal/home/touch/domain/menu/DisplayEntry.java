package ch.bergturbenthal.home.touch.domain.menu;

import java.awt.geom.Point2D;

public interface DisplayEntry {
  void draw();

  boolean handleTouch(Point2D position);
}
