package ch.bergturbenthal.home.touch.domain.renderer;

import ch.bergturbenthal.home.touch.domain.settings.DisplaySettings;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;

public class DisplayRenderer {
  private final DisplaySettings settings;
  private final BufferedImage bufferedImage;
  private final Graphics2D graphics;
  private final AffineTransform transformation;

  public DisplayRenderer(final DisplaySettings settings) {
    this.settings = settings;
    final int imageWidth;
    final int imageHeight;
    imageWidth = settings.getWidth();
    imageHeight = settings.getHeight();
    bufferedImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_BYTE_BINARY);
    graphics = bufferedImage.createGraphics();
    graphics.setColor(Color.WHITE);
    graphics.fill(new Rectangle2D.Float(0, 0, imageWidth, imageHeight));
    graphics.setColor(Color.BLACK);
    graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, settings.getBigFontSize()));
    transformation = new AffineTransform();
    switch (settings.getOrientation()) {
      case DEFAULT:
        break;
      case ROTATE_LEFT:
        transformation.quadrantRotate(1, settings.getHeight() / 2, settings.getHeight() / 2);
        break;
      case ROTATE_RIGHT:
        transformation.quadrantRotate(3, settings.getWidth() / 2, settings.getWidth() / 2);
        break;
      case UPSIDE_DOWN:
        transformation.quadrantRotate(2, settings.getWidth() / 2, settings.getHeight() / 2);
        break;
    }
  }

  public Rectangle2D getTouchZone(int column, int row, int colSpan, int rowSpan, double margin) {
    final int touchColumnCount = settings.getTouchColumnCount();
    if (column >= touchColumnCount)
      throw new IndexOutOfBoundsException(
          "Try to access column "
              + column
              + ", while screen has only "
              + touchColumnCount
              + " columns");
    final int width = settings.getWidth();
    double touchFieldWidth = width * 1.0 / touchColumnCount;
    final int touchRowCount = settings.getTouchRowCount();
    if (row >= touchRowCount)
      throw new IndexOutOfBoundsException(
          "Try to access row " + row + ", while screen has only " + touchRowCount + " rows");
    final int height = settings.getHeight();
    double touchFieldHeight = height * 1.0 / touchRowCount;
    return new Rectangle2D.Double(
        touchFieldWidth * column + margin,
        touchFieldHeight * row + margin,
        touchFieldWidth * colSpan - 2 * margin,
        touchFieldHeight * rowSpan - 2 * margin);
  }

  public void setFontStyle(int style) {
    final Font font = graphics.getFont();
    graphics.setFont(font.deriveFont(style));
  }

  public void setFont(Font font) {
    graphics.setFont(font);
  }

  public void drawText(
      String text,
      int touchZoneColumn,
      int touchZoneRow,
      int colSpan,
      int rowSpan,
      VerticalAlignment verticalAlignment,
      HorizontalAlignment horizontalAlignment) {
    final Rectangle2D zone = getTouchZone(touchZoneColumn, touchZoneRow, colSpan, rowSpan, 1);
    final double y;
    switch (verticalAlignment) {
      case TOP:
        y = zone.getMinY();
        break;
      case MIDDLE:
        y = zone.getCenterY();
        break;
      case BOTTOM:
        y = zone.getMaxY();
        break;
      default:
        throw new IllegalArgumentException(
            "Vertical alignment " + verticalAlignment + " not supported");
    }
    final double x;
    switch (horizontalAlignment) {
      case LEFT:
        x = zone.getMinX();
        break;
      case CENTER:
        x = zone.getCenterX();
        break;
      case RIGHT:
        x = zone.getMaxX();
        break;
      default:
        throw new IllegalArgumentException(
            "Horizontal alignment " + horizontalAlignment + " not supported");
    }
    doDrawText(text, x, y, verticalAlignment, horizontalAlignment);
  }

  public void fillShape(
      Shape shape, int touchZoneColumn, int touchZoneRow, int colSpan, int rowSpan) {
    graphics.setTransform(
        createMoveIntoTransform(
            getTouchZone(touchZoneColumn, touchZoneRow, colSpan, rowSpan, 2), shape.getBounds2D()));
    graphics.fill(shape);
    graphics.setTransform(new AffineTransform());
  }

  private AffineTransform createMoveIntoTransform(
      final Rectangle2D targetZone, final Rectangle2D shapeZone) {
    double widthScale = targetZone.getWidth() / shapeZone.getWidth();
    double heightScale = targetZone.getHeight() / shapeZone.getHeight();
    double scale = Math.min(widthScale, heightScale);
    final double srcX = shapeZone.getCenterX() * scale;
    final double srcY = shapeZone.getCenterY() * scale;
    final double centerX = targetZone.getCenterX();
    final double centerY = targetZone.getCenterY();
    final AffineTransform transform = AffineTransform.getScaleInstance(scale, scale);
    final AffineTransform translateInstance =
        AffineTransform.getTranslateInstance(centerX - srcX, centerY - srcY);
    translateInstance.concatenate(transform);
    return translateInstance;
  }

  private void doDrawText(
      String text,
      double x,
      double y,
      VerticalAlignment verticalAlignment,
      HorizontalAlignment horizontalAlignment) {
    final Font currentFont = graphics.getFont();
    final FontRenderContext fontRenderContext = graphics.getFontRenderContext();
    final Rectangle2D stringBounds = currentFont.getStringBounds(text, fontRenderContext);
    final float drawX;
    final float drawY;
    switch (verticalAlignment) {
      case TOP:
        drawY = (float) (y - stringBounds.getMinY());
        break;
      case MIDDLE:
        drawY = (float) (y - stringBounds.getCenterY());
        break;
      case BASELINE:
        drawY = (float) y;
        break;
      case BOTTOM:
        drawY = (float) (y - stringBounds.getMaxY());
        break;
      default:
        throw new IllegalArgumentException("Unsupported vertical alignment: " + verticalAlignment);
    }
    switch (horizontalAlignment) {
      case LEFT:
        drawX = (float) (x - stringBounds.getMinX());
        break;
      case CENTER:
        drawX = (float) (x - stringBounds.getCenterX());
        break;
      case RIGHT:
        drawX = (float) (x - stringBounds.getMaxX());
        break;
      default:
        throw new IllegalArgumentException(
            "Unsupported horizontal alignment: " + horizontalAlignment);
    }
    graphics.drawString(text, drawX, drawY);
  }

  public void render(OutputStream out) throws IOException {

    final AffineTransformOp affineTransformOp =
        new AffineTransformOp(transformation, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);

    ImageIO.write(affineTransformOp.filter(bufferedImage, null), "png", out);
  }

  public enum VerticalAlignment {
    TOP,
    MIDDLE,
    BASELINE,
    BOTTOM
  }

  public enum HorizontalAlignment {
    LEFT,
    CENTER,
    RIGHT
  }
}
