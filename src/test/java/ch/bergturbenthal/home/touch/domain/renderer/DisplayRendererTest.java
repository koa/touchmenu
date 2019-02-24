package ch.bergturbenthal.home.touch.domain.renderer;

import ch.bergturbenthal.home.touch.domain.settings.DisplaySettings;
import org.junit.Test;

import java.awt.*;
import java.io.FileOutputStream;
import java.io.IOException;

public class DisplayRendererTest {
  @Test
  public void testRenderDefault() throws IOException {
    render(DisplaySettings.DisplayOrientation.DEFAULT, "target/default.png");
    render(DisplaySettings.DisplayOrientation.ROTATE_LEFT, "target/left.png");
    render(DisplaySettings.DisplayOrientation.ROTATE_RIGHT, "target/right.png");
    render(DisplaySettings.DisplayOrientation.UPSIDE_DOWN, "target/upside_down.png");
  }

  private void render(final DisplaySettings.DisplayOrientation orientation, final String filename)
      throws IOException {
    final DisplaySettings settings = new DisplaySettings();
    settings.setOrientation(orientation);
    final DisplayRenderer displayRenderer = new DisplayRenderer(settings);
    renderExamples(displayRenderer);
    displayRenderer.render(new FileOutputStream(filename));
  }

  private void renderExamples(final DisplayRenderer displayRenderer) {
    displayRenderer.fillShape(Shapes.UP_ARROW, 1, 0, 1, 1);
    displayRenderer.setFontStyle(Font.BOLD);
    displayRenderer.drawText(
        "22.0",
        0,
        2,
        2,
        1,
        DisplayRenderer.VerticalAlignment.MIDDLE,
        DisplayRenderer.HorizontalAlignment.CENTER);
    displayRenderer.setFontStyle(Font.PLAIN);
    displayRenderer.drawText(
        "21.7",
        0,
        1,
        2,
        1,
        DisplayRenderer.VerticalAlignment.MIDDLE,
        DisplayRenderer.HorizontalAlignment.CENTER);
    displayRenderer.drawText(
        "°C",
        0,
        3,
        2,
        1,
        DisplayRenderer.VerticalAlignment.MIDDLE,
        DisplayRenderer.HorizontalAlignment.CENTER);
    displayRenderer.fillShape(Shapes.DOWN_ARROW, 1, 4, 1, 1);
    displayRenderer.fillShape(Shapes.LEFT_ARROW, 0, 4, 1, 1);
  }
}
