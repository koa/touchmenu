package ch.bergturbenthal.home.touch;

import ch.bergturbenthal.home.touch.domain.menu.MenuProcessor;
import ch.bergturbenthal.home.touch.domain.mqtt.impl.PahoMqttClient;
import ch.bergturbenthal.home.touch.domain.settings.MenuProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@SpringBootApplication
@ComponentScan(
    basePackageClasses = {PahoMqttClient.class, MenuProperties.class, MenuProcessor.class})
public class TouchmenuApplication {

  public static void main(String[] args) {
    SpringApplication.run(TouchmenuApplication.class, args);
  }

  @Bean
  public ScheduledExecutorService executorService() {
    return Executors.newScheduledThreadPool(2);
  }
}
