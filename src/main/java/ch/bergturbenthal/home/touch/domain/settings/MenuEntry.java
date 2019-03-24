package ch.bergturbenthal.home.touch.domain.settings;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = MenuEntry.MenuEntryBuilder.class)
public class MenuEntry {
  private String label;
  private String icon;
  @NonNull private View content;

  @JsonPOJOBuilder(withPrefix = "")
  public static class MenuEntryBuilder {}
}
