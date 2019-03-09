package ch.bergturbenthal.home.touch.domain.renderer;

import ch.bergturbenthal.home.touch.domain.settings.DisplaySettings;
import lombok.Builder;
import lombok.Value;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
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
    bufferedImage =
        new BufferedImage(
            settings.getWidth(), settings.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
    graphics = bufferedImage.createGraphics();
    clear();
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

  public Point2D calcTouchPosition(int x, int y) {
    try {
      return transformation.inverseTransform(new Point2D.Float(x, y), new Point2D.Float());
    } catch (NoninvertibleTransformException e) {
      throw new IllegalStateException("Illegal transformation " + settings.getOrientation(), e);
    }
  }

  public void clear() {
    graphics.setColor(Color.WHITE);
    graphics.fill(new Rectangle2D.Float(0, 0, settings.getWidth(), settings.getHeight()));
    graphics.setColor(Color.BLACK);
    graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, settings.getBigFontSize()));
    graphics.setStroke(new BasicStroke(0.0f));
  }

  public Rectangle2D getTouchZone(ZoneAddress position, double margin) {
    final int touchColumnCount = settings.getTouchColumnCount();
    final int column = position.getColumn();
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
    final int row = position.getRow();
    if (row >= touchRowCount)
      throw new IndexOutOfBoundsException(
          "Try to access row " + row + ", while screen has only " + touchRowCount + " rows");
    final int height = settings.getHeight();
    double touchFieldHeight = height * 1.0 / touchRowCount;
    final int colSpan = position.getColSpan();
    final int rowSpan = position.getRowSpan();
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

  //  public void setFont(Font font) {
  //    graphics.setFont(font);
  //  }

  public void drawText(
      String text,
      ZoneAddress position,
      VerticalAlignment verticalAlignment,
      HorizontalAlignment horizontalAlignment) {
    final Rectangle2D zone = getTouchZone(position, 1);
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
    doDrawText(
        text, x, y, verticalAlignment, horizontalAlignment, zone.getWidth(), zone.getHeight());
  }

  public void fillShape(Shape shape, ZoneAddress position) {
    graphics.setTransform(createMoveIntoTransform(getTouchZone(position, 2), shape.getBounds2D()));
    graphics.fill(shape);
    graphics.setTransform(new AffineTransform());
  }

  public void drawShape(Shape shape, ZoneAddress position) {
    graphics.setTransform(createMoveIntoTransform(getTouchZone(position, 2), shape.getBounds2D()));
    graphics.draw(shape);
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
      HorizontalAlignment horizontalAlignment,
      final double maxWidth,
      final double maxHeight) {

    final Font fontBefore = graphics.getFont();
    final FontRenderContext fontRenderContext = graphics.getFontRenderContext();
    final Font currentFont = findFittingFont(text, maxWidth, maxHeight, fontBefore, fontRenderContext);
    graphics.setFont(currentFont);
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
    graphics.setFont(fontBefore);
  }

  private Font findFittingFont(
      final String text,
      final double maxWidth,
      final double maxHeight,
      final Font fontBefore,
      final FontRenderContext fontRenderContext) {
    for (int i = fontBefore.getSize(); i > 1; i--) {
      Font currentFont = fontBefore.deriveFont((float) i);
      final Rectangle2D stringBounds = currentFont.getStringBounds(text, fontRenderContext);
      if (!(stringBounds.getWidth() > maxWidth) && !(stringBounds.getHeight() > maxHeight))
        return currentFont;
    }
    return fontBefore;
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

  @Value
  @Builder
  public static class ZoneAddress {
    private int column;
    private int row;
    private int colSpan;
    private int rowSpan;

    public static class ZoneAddressBuilder {
      public ZoneAddressBuilder() {
        colSpan = 1;
        rowSpan = 1;
      }
    }
  }
}
