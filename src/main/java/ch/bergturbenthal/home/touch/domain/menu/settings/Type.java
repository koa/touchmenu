package ch.bergturbenthal.home.touch.domain.menu.settings;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Type {
  @JsonProperty("integer")
  INTEGER,
  @JsonProperty("string")
  STRING,
  @JsonProperty("float")
  FLOAT
}
