package ch.bergturbenthal.home.touch.domain.mqtt;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

public interface MqttClient {
  Flux<MqttWireMessage> publish(String topic, MqttMessage message);

  Flux<ReceivedMqttMessage> listenTopic(String topic);

  void registerTopic(
      final String topic,
      final Consumer<ReceivedMqttMessage> mqttMessageConsumer,
      final Consumer<Disposable> disposableConsumer);

  interface ReceivedMqttMessage {
    String getTopic();

    MqttMessage getMessage();
  }
}
