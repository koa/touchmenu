package ch.bergturbenthal.home.touch.domain.menu.settings;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.time.Duration;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Screen.ScreenBuilder.class)
public class Screen {

  private String name;
  private String displaySettings;

  @JsonFormat(shape = JsonFormat.Shape.STRING)
  private Duration screenTimeout;

  @NonNull private View   defaultView;
  @NonNull private View   rootMenu;
  private          String startEntry;

  @JsonPOJOBuilder(withPrefix = "")
  public static class ScreenBuilder {
    {
      displaySettings = "default";
      startEntry = "";
      screenTimeout = Duration.ofMinutes(1);
    }
  }
}
