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
package org.vaulttec.idm.sync;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.actuate.audit.AuditEvent;

public abstract class SyncEvents {

  public static final String PRINCIPAL = "idm-sync";

  public static final String USER_CREATED = "USER_CREATED";
  public static final String USER_BLOCKED = "USER_BLOCKED";
  public static final String USER_UNBLOCKED = "USER_UNBLOCKED";
  public static final String USER_ADDED = "USER_ADDED";
  public static final String USER_UPDATED = "USER_UPDATED";
  public static final String USER_REMOVED = "USER_REMOVED";
  /** Generic term for the application-specific terms GitLab Group, Mattermost Team, JIRA Project, ... */
  public static final String COMPOSITE_CREATED = "COMPOSITE_CREATED";

  public static AuditEvent createEvent(String type, String application, String... data) {
    Map<String, Object> dataMap = convertToMap(data);
    dataMap.put("application", application);
    return new AuditEvent(PRINCIPAL, type, dataMap);
  }

  private static Map<String, Object> convertToMap(String[] data) {
    Map<String, Object> map = new HashMap<>();
    for (String entry : data) {
      int index = entry.indexOf('=');
      if (index != -1) {
        map.put(entry.substring(0, index), entry.substring(index + 1));
      } else {
        map.put(entry, null);
      }
    }
    return map;
  }
}
