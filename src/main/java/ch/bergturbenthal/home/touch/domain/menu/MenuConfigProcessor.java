package ch.bergturbenthal.home.touch.domain.menu;

import ch.bergturbenthal.home.touch.domain.mqtt.MqttClient;
import ch.bergturbenthal.home.touch.domain.menu.settings.Screen;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public class MenuConfigProcessor {
  private final MqttClient mqttClient;
  private final ObjectReader screenObjectReader;
  private final MenuProcessor menuProcessor;

  public MenuConfigProcessor(final MqttClient mqttClient, MenuProcessor menuProcessor) {
    this.mqttClient = mqttClient;
    this.menuProcessor = menuProcessor;
    final ObjectMapper objectMapper =
        Jackson2ObjectMapperBuilder.json().factory(new YAMLFactory()).build();
    screenObjectReader = objectMapper.readerFor(Screen.class);
  }

  @PostConstruct
  public void initConfigListeners() {
    AtomicReference<Disposable> currentRunningDisposable = new AtomicReference<>();
    ConcurrentHashMap<String, Disposable> currentRunningMenu = new ConcurrentHashMap<>();
    final String configPrefix = "settings/menu/";
    mqttClient.registerTopic(
        configPrefix + "#",
        msg -> {
          final String topic = msg.getTopic();
          try {
            final String screenTopic = topic.substring(configPrefix.length());
            final MqttMessage message = msg.getMessage();
            Screen screen = screenObjectReader.readValue(message.getPayload());

            synchronized (currentRunningMenu) {
              final Disposable oldDisposable = currentRunningMenu.remove(screenTopic);
              if (oldDisposable != null) oldDisposable.dispose();
              final Disposable screenDisposable = menuProcessor.startTopic(screenTopic, screen);
              currentRunningMenu.put(screenTopic, screenDisposable);
            }
          } catch (IOException e) {
            log.warn("Cannot load config from " + topic, e);
          }
        },
        disposable -> {
          final Disposable oldDisposable = currentRunningDisposable.getAndSet(disposable);
          if (oldDisposable != null) oldDisposable.dispose();
        });
  }
}
