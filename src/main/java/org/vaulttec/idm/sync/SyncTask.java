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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.vaulttec.idm.sync.app.Application;
import org.vaulttec.idm.sync.idp.IdentityProvider;
import org.vaulttec.idm.sync.idp.IdpGroup;
import org.vaulttec.idm.sync.idp.IdpUser;

@Component
public class SyncTask extends AbstractSyncEventPublisher {

  private static final Logger LOG = LoggerFactory.getLogger(SyncTask.class);

  private final IdentityProvider idp;
  private final List<Application> apps;
  private final SyncConfig syncConfig;

  SyncTask(IdentityProvider idp, List<Application> apps, SyncConfig syncConfig) {
    this.idp = idp;
    this.apps = apps;
    this.syncConfig = syncConfig;
  }

  @Scheduled(fixedRateString = "${sync.rate}")
  public void sync() {
    LOG.info("Start syncing...");
    if (idp.authenticate()) {
      for (Application app : apps) {
        if (syncConfig.getEnabledApps().contains("*") || syncConfig.getEnabledApps().contains(app.getId())) {
          LOG.info("Syncing '{}'", app.getName());
          publishSyncEvent(SyncEvents.createEvent(SyncEvents.SYNC_STARTED, app.getId()));
          List<IdpGroup> groups = getGroupsWithMembers(app.getGroupSearch());
          if (groups != null) {
            app.sync(groups);
          }
          publishSyncEvent(SyncEvents.createEvent(SyncEvents.SYNC_FINISHED, app.getId()));
        }
      }
    }
    LOG.info("Finished syncing...");
  }

  protected List<IdpGroup> getGroupsWithMembers(String search) {
    LOG.debug("Retrieving groups with members: search={}", search);
    List<IdpGroup> groups = idp.getGroups(search);
    if (groups != null) {
      for (IdpGroup group : groups) {
        List<IdpUser> members = idp.getGroupMembers(group);
        if (members != null) {
          for (IdpUser member : members) {
            member.addGroup(group);
            group.addMember(member);
          }
        }
      }
      return groups;
    }
    return null;
  }

}
