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

    displayRenderer.fillShape(
        Shapes.UP_ARROW, DisplayRenderer.ZoneAddress.builder().row(0).column(1).build());
    ;
    displayRenderer.setFontStyle(Font.BOLD);
    displayRenderer.drawText(
        "22.0",
        DisplayRenderer.ZoneAddress.builder().row(2).column(0).colSpan(2).build(),
        DisplayRenderer.VerticalAlignment.MIDDLE,
        DisplayRenderer.HorizontalAlignment.CENTER);
    displayRenderer.setFontStyle(Font.PLAIN);
    displayRenderer.drawText(
        "21.7",
        DisplayRenderer.ZoneAddress.builder().row(1).column(0).colSpan(2).build(),
        DisplayRenderer.VerticalAlignment.MIDDLE,
        DisplayRenderer.HorizontalAlignment.CENTER);

    displayRenderer.drawText(
        "°C",
        DisplayRenderer.ZoneAddress.builder().row(3).column(0).colSpan(2).build(),
        DisplayRenderer.VerticalAlignment.MIDDLE,
        DisplayRenderer.HorizontalAlignment.CENTER);

    displayRenderer.fillShape(
        Shapes.DOWN_ARROW, DisplayRenderer.ZoneAddress.builder().row(4).column(1).build());
    displayRenderer.drawShape(
        Shapes.LEFT_ARROW, DisplayRenderer.ZoneAddress.builder().row(4).column(0).build());
  }
}
