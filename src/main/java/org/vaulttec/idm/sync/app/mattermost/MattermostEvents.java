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

import org.springframework.boot.actuate.audit.AuditEvent;
import org.vaulttec.idm.sync.SyncEvents;
import org.vaulttec.idm.sync.idp.IdpUser;

public abstract class MattermostEvents extends SyncEvents {

  public static AuditEvent userCreated(MMUser user, IdpUser idpUser) {
    return createEvent(USER_CREATED, Mattermost.APPLICATION_ID, "idpUserId=" + idpUser.getId(),
        "userId=" + user.getId(), "username=" + user.getUsername());
  }

  public static AuditEvent userDeactivated(MMUser user) {
    return createEvent(USER_BLOCKED, Mattermost.APPLICATION_ID, "username=" + user.getUsername());
  }

  public static AuditEvent userActivated(MMUser user) {
    return createEvent(USER_UNBLOCKED, Mattermost.APPLICATION_ID, "username=" + user.getUsername());
  }

  public static AuditEvent userAddedToTeam(MMUser user, MMTeam team) {
    return createEvent(USER_ADDED, Mattermost.APPLICATION_ID, "username=" + user.getUsername(), "compositeType=team",
        "compositeId=" + team.getId(), "compositeName=" + team.getName());
  }

  public static AuditEvent userRemovedFromTeam(MMUser user, MMTeam team) {
    return createEvent(USER_REMOVED, Mattermost.APPLICATION_ID, "username=" + user.getUsername(), "compositeType=team",
        "compositeName=" + team.getName());
  }

  public static AuditEvent teamCreated(MMTeam team) {
    return createEvent(COMPOSITE_CREATED, Mattermost.APPLICATION_ID, "compositeType=team",
        "compositeId=" + team.getId(), "compositeName=" + team.getName());
  }
}
