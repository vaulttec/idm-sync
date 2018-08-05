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

import org.springframework.boot.actuate.audit.AuditEvent;
import org.vaulttec.idm.sync.SyncEvents;

public abstract class GitLabEvents extends SyncEvents {

  public static AuditEvent userCreated(String username) {
    return createEvent(USER_CREATED, GitLab.GITLAB_APPLICATION_ID, "username=" + username);
  }

  public static AuditEvent userBlocked(String username) {
    return createEvent(USER_BLOCKED, GitLab.GITLAB_APPLICATION_ID, "username=" + username);
  }

  public static AuditEvent userUnblocked(String username) {
    return createEvent(USER_UNBLOCKED, GitLab.GITLAB_APPLICATION_ID, "username=" + username);
  }

  public static AuditEvent userAddedToGroup(String username, String groupPath) {
    return createEvent(USER_ADDED, GitLab.GITLAB_APPLICATION_ID, "username=" + username, "targetType=group",
        "groupPath=" + groupPath);
  }

  public static AuditEvent userRemovedFromGroup(String username, String groupPath) {
    return createEvent(USER_REMOVED, GitLab.GITLAB_APPLICATION_ID, "username=" + username, "targetType=group",
        "groupPath=" + groupPath);
  }

  public static AuditEvent userRemovedFromProject(String username, String projectPath) {
    return createEvent(USER_REMOVED, GitLab.GITLAB_APPLICATION_ID, "username=" + username, "targetType=project",
        "projectPath=" + projectPath);
  }

  public static AuditEvent groupCreated(String groupPath) {
    return createEvent(GROUP_CREATED, GitLab.GITLAB_APPLICATION_ID, "groupPath=" + groupPath);
  }
}
