package ch.bergturbenthal.home.touch.domain.util;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttMessage;

@Slf4j
public class MqttMessageUtil {

  public static MqttMessage createMessage(final String content, final boolean retained) {
    final MqttMessage message = new MqttMessage();
    message.setRetained(retained);
    message.setQos(1);
    message.setPayload(content.getBytes());
    return message;
  }
}
