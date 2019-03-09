package ch.bergturbenthal.home.touch.domain.mqtt.impl;

import ch.bergturbenthal.home.touch.domain.mqtt.MqttClient;
import ch.bergturbenthal.home.touch.domain.settings.MenuProperties;
import ch.bergturbenthal.home.touch.domain.settings.MqttEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

@Slf4j
@Component
@RefreshScope
public class PahoMqttClient implements MqttClient {
  private final MenuProperties properties;
  private final Mono<IMqttAsyncClient> asyncClient;
  private final Map<String, Collection<FluxSink<MqttMessage>>> registeredSinks =
      new ConcurrentHashMap<>();
  private ExecutorService executorService;
  private Map<String, MqttMessage> retainedMessages = new ConcurrentHashMap<>();

  public PahoMqttClient(final MenuProperties properties, ExecutorService executorService)
      throws MqttException {
    this.properties = properties;
    this.executorService = executorService;

    final MqttEndpoint mqtt = properties.getMqtt();
    final String hostAddress = mqtt.getAddress().getHostAddress();
    final int port = mqtt.getPort();
    String brokerAddress = "tcp://" + hostAddress + ":" + port;
    final MqttAsyncClient client = new MqttAsyncClient(brokerAddress, mqtt.getClientId());
    client.setCallback(
        new MqttCallback() {
          @Override
          public void connectionLost(final Throwable throwable) {}

          @Override
          public void messageArrived(final String s, final MqttMessage mqttMessage) {
            if (mqttMessage.isRetained()) retainedMessages.put(s, mqttMessage);
            new ArrayList<>(registeredSinks.getOrDefault(s, Collections.emptyList()))
                .forEach(sink -> sink.next(mqttMessage));
          }

          @Override
          public void deliveryComplete(final IMqttDeliveryToken iMqttDeliveryToken) {}
        });
    asyncClient =
        Mono.create(
                (MonoSink<IMqttAsyncClient> sink) -> {
                  try {
                    client.connect(
                        null,
                        new IMqttActionListener() {
                          @Override
                          public void onSuccess(final IMqttToken asyncActionToken) {
                            log.info("Connected: " + client.isConnected());
                            sink.success(client);
                          }

                          @Override
                          public void onFailure(
                              final IMqttToken asyncActionToken, final Throwable exception) {
                            sink.error(exception);
                          }
                        });
                  } catch (MqttException e) {
                    sink.error(e);
                  }
                })
            .cache();
    log.info("Connected: " + client.isConnected());
  }

  @Override
  public Mono<MqttWireMessage> publish(String topic, MqttMessage message) {
    return asyncClient.flatMap(
        client ->
            Mono.create(
                (MonoSink<MqttWireMessage> sink) -> {
                  try {
                    client.publish(
                        topic,
                        message,
                        null,
                        new IMqttActionListener() {
                          @Override
                          public void onSuccess(final IMqttToken iMqttToken) {
                            sink.success(iMqttToken.getResponse());
                          }

                          @Override
                          public void onFailure(
                              final IMqttToken iMqttToken, final Throwable throwable) {
                            sink.error(throwable);
                          }
                        });
                  } catch (MqttException e) {
                    sink.error(e);
                  }
                }));
  }

  @Override
  public Flux<MqttMessage> listenTopic(String topic) {
    if (topic == null) throw new IllegalArgumentException("Missing topic");
    return Flux.concat(
        Mono.justOrEmpty(retainedMessages.get(topic)),
        asyncClient.flatMapMany(
            client ->
                Flux.create(
                    sink -> {
                      sink.onDispose(
                          () -> {
                            synchronized (registeredSinks) {
                              final Collection<FluxSink<MqttMessage>> existingSubscriptions =
                                  registeredSinks.get(topic);
                              if (existingSubscriptions != null) existingSubscriptions.remove(sink);
                              if (existingSubscriptions == null
                                  || existingSubscriptions.isEmpty()) {
                                registeredSinks.remove(topic);
                                try {
                                  client.unsubscribe(topic);
                                } catch (MqttException e) {
                                  log.error("Cannot unsubscribe from " + topic, e);
                                }
                              }
                            }
                          });
                      synchronized (registeredSinks) {
                        final Collection<FluxSink<MqttMessage>> existingSubscriptions =
                            registeredSinks.get(topic);
                        if (existingSubscriptions != null) {
                          existingSubscriptions.add(sink);
                        } else {
                          final Collection<FluxSink<MqttMessage>> newSubscriptions =
                              new ConcurrentLinkedDeque<>();
                          registeredSinks.put(topic, newSubscriptions);
                          newSubscriptions.add(sink);
                          try {
                            client.subscribe(topic, 1);
                          } catch (MqttException e) {
                            sink.error(e);
                          }
                        }
                      }
                    })));
  }

  @Override
  public void registerTopic(
      final String topic,
      final Consumer<MqttMessage> mqttMessageConsumer,
      final Consumer<Disposable> disposableConsumer) {
    disposableConsumer.accept(
        listenTopic(topic)
            .subscribe(
                mqttMessageConsumer,
                ex -> {
                  log.warn("Error processing " + topic, ex);
                  executorService.submit(
                      () -> registerTopic(topic, mqttMessageConsumer, disposableConsumer));
                }));
  }
}
