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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StringUtils;
import org.vaulttec.idm.sync.idp.IdpGroup;
import org.vaulttec.idm.sync.idp.IdpUser;
import org.vaulttec.idm.sync.idp.keycloak.KeycloakClient;
import org.vaulttec.idm.sync.idp.keycloak.KeycloakClientBuilder;

@ActiveProfiles("test")
@IfProfileValue(name = "run.integration.tests", value = "true")
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
public class MattermostIntegrationTest {

  @Autowired
  private Environment env;

  @Test
  public void testSync() {
    KeycloakClientBuilder kcBuilder = new KeycloakClientBuilder(env.getProperty("idp.config.serverUrl"))
        .realm(env.getProperty("idp.config.realm")).clientId(env.getProperty("idp.config.client.id"))
        .clientSecret(env.getProperty("idp.config.client.secret"));
    if (StringUtils.hasText(env.getProperty("proxy.host"))) {
      kcBuilder = kcBuilder.proxyHost(env.getProperty("proxy.host"))
          .proxyPort(Integer.parseInt(env.getProperty("proxy.port")));
    }
    KeycloakClient kcClient = kcBuilder.build();

    assertTrue(kcClient.authenticate());
    List<IdpGroup> groups = kcClient.getGroups(env.getProperty("apps[1].config.group.search"));
    assertThat(groups).isNotNull().isNotEmpty();
    for (IdpGroup group : groups) {
      List<IdpUser> members = kcClient.getGroupMembers(group);
      assertThat(members).isNotNull();
      for (IdpUser member : members) {
        member.addGroup(group);
        group.addMember(member);
      }
    }

    MattermostClientBuilder mmcBuilder = new MattermostClientBuilder(env.getProperty("apps[1].config.serverUrl"))
        .perPage(Integer.parseInt(env.getProperty("apps[1].config.perPage")))
        .personalAccessToken(env.getProperty("apps[1].config.personalAccessToken"));
    if (StringUtils.hasText(env.getProperty("proxy.host"))) {
      mmcBuilder = mmcBuilder.proxyHost(env.getProperty("proxy.host"))
          .proxyPort(Integer.parseInt(env.getProperty("proxy.port")));
    }
    MattermostClient glClient = mmcBuilder.build();

    MattermostBuilder mmBuilder = new MattermostBuilder(glClient, null)
        .groupRegExp(env.getProperty("apps[1].config.group.regExp"))
        .excludedUsers(env.getProperty("apps[1].config.sync.excludedUsers"))
        .authService(env.getProperty("apps[1].config.auth.serviceName"))
        .authUidAttribute(env.getProperty("apps[1].config.auth.uidAttribute"));
    Mattermost mattermost = mmBuilder.build();

    assertTrue(mattermost.sync(groups));
  }
}
