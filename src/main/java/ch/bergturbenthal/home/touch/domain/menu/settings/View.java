package ch.bergturbenthal.home.touch.domain.menu.settings;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Value;

import java.util.LinkedHashMap;
import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = View.ViewBuilder.class)
public class View {
  private LinkedHashMap<String, MenuEntry> menu;
  private List<DisplayValue> displayValue;
  private ValueEdit valueEdit;

  @JsonPOJOBuilder(withPrefix = "")
  public static class ViewBuilder {}
}
