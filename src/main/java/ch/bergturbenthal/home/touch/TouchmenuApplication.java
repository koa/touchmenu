package ch.bergturbenthal.home.touch;

import ch.bergturbenthal.home.touch.domain.menu.MenuProcessor;
import ch.bergturbenthal.home.touch.domain.mqtt.impl.PahoMqttClient;
import ch.bergturbenthal.home.touch.domain.settings.MenuProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@SpringBootApplication
@ComponentScan(
    basePackageClasses = {PahoMqttClient.class, MenuProperties.class, MenuProcessor.class})
@Import(SimpleDiscoveryClientAutoConfiguration.class)
@EnableScheduling
public class TouchmenuApplication {

  public static void main(String[] args) {
    SpringApplication.run(TouchmenuApplication.class, args);
  }

  @Bean
  public ScheduledExecutorService executorService() {
    return Executors.newScheduledThreadPool(2);
  }
}
