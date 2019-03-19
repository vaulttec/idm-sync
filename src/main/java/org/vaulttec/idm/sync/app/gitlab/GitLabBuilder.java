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

import org.springframework.boot.actuate.audit.AuditEventRepository;

public final class GitLabBuilder {

  private final GitLabClient client;
  private final AuditEventRepository eventRepository;
  private String groupSearch;
  private String groupRegExp;
  private String excludedUsers;
  private boolean removeProjectMembers;
  private String providerName;
  private String providerUidAttribute;

  public GitLabBuilder(GitLabClient client, AuditEventRepository eventRepository) {
    this.client = client;
    this.eventRepository = eventRepository;
  }

  public GitLabBuilder groupSearch(String groupSearch) {
    this.groupSearch = groupSearch;
    return this;
  }

  public GitLabBuilder groupRegExp(String groupRegExp) {
    this.groupRegExp = groupRegExp;
    return this;
  }

  public GitLabBuilder excludedUsers(String excludedUsers) {
    this.excludedUsers = excludedUsers;
    return this;
  }

  public GitLabBuilder removeProjectMembers(boolean removeProjectMembers) {
    this.removeProjectMembers = removeProjectMembers;
    return this;
  }

  public GitLabBuilder providerName(String providerName) {
    this.providerName = providerName;
    return this;
  }

  public GitLabBuilder providerUidAttribute(String providerUidAttribute) {
    this.providerUidAttribute = providerUidAttribute;
    return this;
  }

  public GitLab build() {
    if (client == null) {
      throw new IllegalStateException("client required");
    }
    if (groupSearch == null) {
      groupSearch = "";
    }
    if (excludedUsers == null) {
      excludedUsers = "root,ghost";
    }
    return new GitLab(client, eventRepository, groupSearch, groupRegExp, excludedUsers, removeProjectMembers,
        providerName, providerUidAttribute);
  }
}