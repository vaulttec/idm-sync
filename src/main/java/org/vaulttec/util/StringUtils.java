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
package org.vaulttec.util;

import java.util.LinkedHashSet;
import java.util.Set;

public abstract class StringUtils extends org.springframework.util.StringUtils {

  public static Set<String> commaDelimitedListToTrimmedSet(String str) {
    Set<String> set = new LinkedHashSet<>();
    String[] tokens = str.split(",");
    for (String token : tokens) {
      String trimmedToken = token.trim();
      if (trimmedToken.length() > 0) {
        set.add(trimmedToken);
      }
    }
    return set;
  }
}
