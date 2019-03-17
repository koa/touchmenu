package ch.bergturbenthal.home.touch.domain.settings;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties("menu")
@Data
public class MenuProperties {
  private MqttEndpoint mqtt = new MqttEndpoint();
  private Map<String, DisplaySettings> displaySettings;
  private List<Screen> screens;
}
