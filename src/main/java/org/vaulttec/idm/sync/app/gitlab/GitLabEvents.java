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
import org.vaulttec.idm.sync.idp.IdpUser;

public abstract class GitLabEvents extends SyncEvents {

  public static AuditEvent userCreated(GLUser user, IdpUser idpUser) {
    return createEvent(USER_CREATED, GitLab.APPLICATION_ID, "idpUserId=" + idpUser.getId(),
        "userId=" + user.getId(), "username=" + user.getUsername());
  }

  public static AuditEvent userBlocked(GLUser user) {
    return createEvent(USER_BLOCKED, GitLab.APPLICATION_ID, "username=" + user.getUsername());
  }

  public static AuditEvent userUnblocked(GLUser user) {
    return createEvent(USER_UNBLOCKED, GitLab.APPLICATION_ID, "username=" + user.getUsername());
  }

  public static AuditEvent userAddedToGroup(GLUser user, GLGroup group) {
    return createEvent(USER_ADDED, GitLab.APPLICATION_ID, "username=" + user.getUsername(), "compositeType=group",
        "compositeId=" + group.getId(), "compositeName=" + group.getPath());
  }

  public static AuditEvent userRemovedFromGroup(GLUser user, GLGroup group) {
    return createEvent(USER_REMOVED, GitLab.APPLICATION_ID, "username=" + user.getUsername(), "compositeType=group",
        "compositeName=" + group.getPath());
  }

  public static AuditEvent userRemovedFromProject(GLUser user, GLProject project) {
    return createEvent(USER_REMOVED, GitLab.APPLICATION_ID, "username=" + user.getUsername(),
        "compositeType=project", "compositeName=" + project.getPath());
  }

  public static AuditEvent groupCreated(GLGroup group) {
    return createEvent(COMPOSITE_CREATED, GitLab.APPLICATION_ID, "compositeType=group",
        "compositeId=" + group.getId(), "compositeName=" + group.getPath());
  }
}
