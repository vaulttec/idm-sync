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

import java.util.Map;

import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;
import org.vaulttec.idm.sync.app.Application;
import org.vaulttec.idm.sync.app.ApplicationFactory;

public class MattermostFactory implements ApplicationFactory {

  @Override
  public Application createApplication(Map<String, String> config, Environment env,
      AuditEventRepository eventRepository) {
    MattermostClientBuilder mmcBuilder = new MattermostClientBuilder(config.get("serverUrl"))
        .perPage(Integer.parseInt(config.get("perPage"))).personalAccessToken(config.get("personalAccessToken"));
    if (StringUtils.hasText(env.getProperty("proxy.host"))) {
      mmcBuilder = mmcBuilder.proxyHost(env.getProperty("proxy.host"))
          .proxyPort(Integer.parseInt(env.getProperty("proxy.port")));
    }
    MattermostClient mmClient = mmcBuilder.build();

    MattermostBuilder mmBuilder = new MattermostBuilder(mmClient, eventRepository)
        .groupSearch(config.get("group.search")).groupRegExp(config.get("group.regExp"))
        .excludedUsers(config.get("sync.excludedUsers")).authService(config.get("auth.serviceName"))
        .authUidAttribute(config.get("auth.uidAttribute"));
    return mmBuilder.build();
  }
}
