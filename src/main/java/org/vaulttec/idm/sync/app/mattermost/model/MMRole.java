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
package org.vaulttec.idm.sync.app.mattermost.model;

import java.util.ArrayList;
import java.util.List;

public enum MMRole {
  SYSTEM_ADMIN, SYSTEM_USER, TEAM_ADMIN, TEAM_USER;

  public static List<MMRole> fromJson(String jsonRoles) {
    List<MMRole> roles = new ArrayList<>();
    String[] splittedJsonRoles = jsonRoles.split(" ");
    for (String jsonRole : splittedJsonRoles) {
      roles.add(valueOf(jsonRole.toUpperCase()));
    }
    return roles;
  }
}
