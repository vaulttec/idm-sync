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

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.vaulttec.idm.sync.app.Application;
import org.vaulttec.idm.sync.idp.IdentityProvider;
import org.vaulttec.idm.sync.idp.model.IdpGroup;
import org.vaulttec.idm.sync.idp.model.IdpUser;
import org.vaulttec.util.StringUtils;

@Component
public class SyncTask {

  private static final Logger LOG = LoggerFactory.getLogger(SyncTask.class);

  private final IdentityProvider idp;
  private final List<Application> applications;
  private final SyncConfig syncConfig;
  private Instant lastSyncTime;

  SyncTask(IdentityProvider idp, List<Application> applications, SyncConfig syncConfig) {
    this.idp = idp;
    this.applications = applications;
    this.syncConfig = syncConfig;
  }

  public Instant getLastSyncTime() {
    return lastSyncTime;
  }

  @Scheduled(fixedRateString = "${sync.rate}")
  public void sync() {
    LOG.info("Start syncing...");
    if (idp.authenticate()) {
      for (Application app : applications) {
        if (syncConfig.getEnabledApps().contains("*") || syncConfig.getEnabledApps().contains(app.getId())) {
          LOG.info("Syncing '{}'", app.getName());
          List<IdpGroup> groups = idp.getGroups(app.getGroupSearch());
          if (groups != null && !groups.isEmpty()) {
            Map<String, IdpUser> users = retrieveMembersForGroups(groups);
            if (users != null) {
              addMissingEmail(users);
              removeRequiredActions(users);
              app.sync(groups);
              updateModifiedUserAttributes(users);
            }
          }
        }
      }
      lastSyncTime = Instant.now();
    }
    LOG.info("Finished syncing...");
  }

  /**
   * Returns a map with all users which are members of the given list of groups.
   * <p>
   * These groups and users or linked with each other by the attributes
   * <code>IdpGroup.members</code> and <code>IdpUser.groups</code>.
   */
  private Map<String, IdpUser> retrieveMembersForGroups(List<IdpGroup> groups) {
    Map<String, IdpUser> users = new HashMap<>();
    for (IdpGroup group : groups) {
      List<IdpUser> members = idp.getGroupMembers(group);
      if (members == null) {
        return null;
      } else {
        for (IdpUser member : members) {
          if (!users.containsKey(member.getId())) {
            users.put(member.getId(), member);
          }
          IdpUser user = users.get(member.getId());
          user.addGroup(group);
          group.addMember(user);
        }
      }
    }
    return users;
  }

  private void addMissingEmail(Map<String, IdpUser> users) {
    for (IdpUser user : users.values()) {
      if (!StringUtils.hasText(user.getEmail()) && StringUtils.hasText(syncConfig.getEmailDomain())) {
        user.setEmail(user.getUsername() + "@" + syncConfig.getEmailDomain());
      }
    }
  }

  private void removeRequiredActions(Map<String, IdpUser> users) {
    for (IdpUser user : users.values()) {
      if (!user.getRequiredActions().isEmpty() && syncConfig.isRemoveRequiredActions()) {
        idp.removeRequiredActions(user);
      }
    }
  }

  private void updateModifiedUserAttributes(Map<String, IdpUser> users) {
    for (IdpUser user : users.values()) {
      if (user.isAttributesModified()) {
        idp.updateUserAttributes(user, user.getAttributes());
      }
    }
  }
}
