package ch.bergturbenthal.home.touch;

import ch.bergturbenthal.home.touch.domain.settings.Screen;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.type.MapLikeType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class GenerateMenuYaml {
  public static void main(String[] args) throws IOException {
    final ObjectMapper objectMapper =
        Jackson2ObjectMapperBuilder.json().factory(new YAMLFactory()).build();

    final ObjectReader reader = objectMapper.readerFor(Screen.class);

    final Screen screens = reader.readValue(new ClassPathResource("menu.yaml").getURL());
    System.out.println("------------------------");
    System.out.println(screens);
    System.out.println("------------------------");
    objectMapper.writerFor(Screen.class).writeValue(System.out, screens);
  }
}
