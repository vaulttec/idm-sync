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
package org.vaulttec.idm.sync.app.mattermost;

import org.springframework.boot.actuate.audit.AuditEventRepository;

public final class MattermostBuilder {

  private final MattermostClient client;
  private final AuditEventRepository eventRepository;
  private String groupSearch;
  private String groupRegExp;
  private String excludedUsers;
  private String globalTeam;
  private String authService;
  private String authUidAttribute;

  public MattermostBuilder(MattermostClient client, AuditEventRepository eventRepository) {
    this.client = client;
    this.eventRepository = eventRepository;
  }

  public MattermostBuilder groupSearch(String groupSearch) {
    this.groupSearch = groupSearch;
    return this;
  }

  public MattermostBuilder groupRegExp(String groupRegExp) {
    this.groupRegExp = groupRegExp;
    return this;
  }

  public MattermostBuilder excludedUsers(String excludedUsers) {
    this.excludedUsers = excludedUsers;
    return this;
  }

  public MattermostBuilder globalTeam(String globalTeam) {
    this.globalTeam = globalTeam;
    return this;
  }

  public MattermostBuilder authService(String authService) {
    this.authService = authService;
    return this;
  }

  public MattermostBuilder authUidAttribute(String authUidAttribute) {
    this.authUidAttribute = authUidAttribute;
    return this;
  }

  public Mattermost build() {
    if (client == null) {
      throw new IllegalStateException("client required");
    }
    if (groupSearch == null) {
      groupSearch = "";
    }
    if (excludedUsers == null) {
      excludedUsers = "root,ghost";
    }
    return new Mattermost(client, eventRepository, groupSearch, groupRegExp, excludedUsers, globalTeam, authService,
        authUidAttribute);
  }
}