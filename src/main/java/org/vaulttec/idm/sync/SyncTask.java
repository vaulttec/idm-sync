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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.vaulttec.idm.sync.app.Application;
import org.vaulttec.idm.sync.idp.IdentityProvider;
import org.vaulttec.idm.sync.idp.IdpGroup;
import org.vaulttec.idm.sync.idp.IdpUser;

@Component
public class SyncTask {

  private static final Logger LOG = LoggerFactory.getLogger(SyncTask.class);

  private final IdentityProvider idp;
  private final List<Application> apps;
  private final SyncConfig syncConfig;
  private final AuditEventRepository eventRepository;

  SyncTask(IdentityProvider idp, List<Application> apps, SyncConfig syncConfig, AuditEventRepository eventRepository) {
    this.idp = idp;
    this.apps = apps;
    this.syncConfig = syncConfig;
    this.eventRepository = eventRepository;
  }

  @Scheduled(fixedRateString = "${sync.rate}")
  public void sync() {
    LOG.info("Start syncing...");
    if (idp.authenticate()) {
      for (Application app : apps) {
        if (syncConfig.getEnabledApps().contains("*") || syncConfig.getEnabledApps().contains(app.getId())) {
          LOG.info("Syncing '{}'", app.getName());
          List<IdpGroup> groups = idp.getGroups(app.getGroupSearch());
          if (groups != null) {
            Map<String, IdpUser> users = retrieveMembersForGroups(groups);

            // Sync groups
            Instant start = Instant.now();
            app.sync(groups);

            // Update user attributes for newly created application users
            updateUserAttributes(users, start);
          }
        }
      }
    }
    LOG.info("Finished syncing...");
  }

  private Map<String, IdpUser> retrieveMembersForGroups(List<IdpGroup> groups) {
    Map<String, IdpUser> users = new HashMap<>();
    for (IdpGroup group : groups) {
      List<IdpUser> members = idp.getGroupMembers(group);
      if (members != null) {
        for (IdpUser member : members) {
          member.addGroup(group);
          group.addMember(member);
          users.put(member.getId(), member);
        }
      }
    }
    return users;
  }

  private void updateUserAttributes(Map<String, IdpUser> users, Instant start) {
    List<AuditEvent> events = eventRepository.find(SyncEvents.PRINCIPAL, start, SyncEvents.USER_CREATED);
    for (AuditEvent event : events) {
      Map<String, Object> data = event.getData();
      if (data.containsKey("application") && data.containsKey("idpUserId") && data.containsKey("userId")) {
        String idpUserId = (String) data.get("idpUserId");
        IdpUser user = users.get(idpUserId);
        if (user != null) {
          String appUserId = (String) data.get("userId");
          String appId = (String) data.get("application");
          String attributeName = appId.toUpperCase() + "_USER_ID";
          Map<String, List<String>> attributes = new HashMap<>();
          attributes.put(attributeName, Arrays.asList(appUserId));
          idp.updateUserAttributes(user, attributes);
        }
      }
    }
  }
}
