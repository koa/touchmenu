package ch.bergturbenthal.home.touch.domain.menu;

import java.awt.geom.Point2D;

public interface DisplayEntry {
  void draw();

  TouchResult handleTouch(Point2D position);

  enum TouchResult {
    IGNORED,
    DIRTY,
    NOOP
  }
}
