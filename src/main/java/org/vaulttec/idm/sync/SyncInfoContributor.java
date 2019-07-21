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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.boot.actuate.info.Info.Builder;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;
import org.vaulttec.idm.sync.app.Application;

@Component
public class SyncInfoContributor implements InfoContributor {

  private final SyncTask syncTask;
  private final List<String> appNames;

  SyncInfoContributor(SyncTask syncTask, List<Application> apps) {
    this.syncTask = syncTask;
    this.appNames = apps.stream().map(a -> a.getName()).collect(Collectors.toList());
  }

  @Override
  public void contribute(Builder builder) {
    Map<String, Object> syncDetails = new HashMap<>();
    syncDetails.put("apps", appNames);
    syncDetails.put("lastSync", syncTask.getLastSyncTime());
    builder.withDetail("sync", syncDetails);
  }
}
