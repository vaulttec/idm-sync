/*
 * IDM Syncronizer
 * Copyright (c) 2018 Torsten Juergeleit
 * mailto:torsten AT vaulttec DOT org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.vaulttec.idm.sync.app.gitlab;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum GLPermission {
  NO("0"), GUEST("10"), REPORTER("20"), DEVELOPER("30"), MAINTAINER("40"), OWNER("50"), ADMIN("60");

  private final static Map<String, GLPermission> ENUM_NAME_MAP;
  static {
    ENUM_NAME_MAP = Arrays.stream(GLPermission.values())
        .collect(Collectors.toMap(GLPermission::getAccessLevel, Function.identity()));
  }

  @JsonCreator
  public static GLPermission fromAccessLevel(String accessLevel) {
    return ENUM_NAME_MAP.get(accessLevel);
  }

  public static GLPermission fromName(String name) {
    return valueOf(name.toUpperCase());
  }

  String accessLevel;

  private GLPermission(String accessLevel) {
    this.accessLevel = accessLevel;
  }

  @JsonValue
  public String getAccessLevel() {
    return accessLevel;
  }

  public int compareAccessLevel(GLPermission other) {
    if (other == null) {
      throw new IllegalArgumentException("Null not allowed here");
    }
    if (this == other) {
      return 0;
    }
    return Integer.parseInt(this.getAccessLevel()) - Integer.parseInt(other.getAccessLevel());
  }
}
