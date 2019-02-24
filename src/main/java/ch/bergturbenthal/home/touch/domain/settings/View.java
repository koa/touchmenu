package ch.bergturbenthal.home.touch.domain.settings;

import lombok.Data;

import java.util.List;

@Data
public class View {
  private List<MenuEntry> menu;
  private List<DisplayValue> displayValue;
  private ValueEdit valueEdit;
}
