package ch.bergturbenthal.home.touch.domain.settings;

import lombok.Data;

@Data
public class MqttEndpoint {
  private String service = "mqtt";
  private String clientId = "TouchMenuProcessor";
}
