package ch.bergturbenthal.home.touch.domain.settings;

import lombok.Data;

import java.net.InetAddress;

@Data
public class MqttEndpoint {
  private InetAddress address;
  private int port = 1883;
  private String clientId = "TouchMenuProcessor";
}
