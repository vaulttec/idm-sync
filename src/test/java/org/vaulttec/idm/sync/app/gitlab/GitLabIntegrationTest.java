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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Comparator;
import java.util.List;

import org.junit.Before;
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
import org.vaulttec.idm.sync.app.model.AppStatistics;
import org.vaulttec.idm.sync.idp.IdpGroup;
import org.vaulttec.idm.sync.idp.IdpUser;
import org.vaulttec.idm.sync.idp.keycloak.KeycloakClient;
import org.vaulttec.idm.sync.idp.keycloak.KeycloakClientBuilder;

@ActiveProfiles("test")
@IfProfileValue(name = "run.integration.tests", value = "true")
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
public class GitLabIntegrationTest {

  private static final Comparator<String> INTEGER_STRING_COMPARATOR = new Comparator<String>() {
    public int compare(String s1, String s2) {
      return Integer.parseInt(s1) - Integer.parseInt(s2);
    }
  };

  @Autowired
  private Environment env;
  private KeycloakClient kcClient;
  private GitLab gitlab;

  @Before
  public void setup() {
    KeycloakClientBuilder kcBuilder = new KeycloakClientBuilder(env.getProperty("idp.config.serverUrl"))
        .realm(env.getProperty("idp.config.realm")).clientId(env.getProperty("idp.config.client.id"))
        .clientSecret(env.getProperty("idp.config.client.secret"));
    if (StringUtils.hasText(env.getProperty("proxy.host"))) {
      kcBuilder = kcBuilder.proxyHost(env.getProperty("proxy.host"))
          .proxyPort(Integer.parseInt(env.getProperty("proxy.port")));
    }
    kcClient = kcBuilder.build();
    assertTrue(kcClient.authenticate());

    GitLabClientBuilder glcBuilder = new GitLabClientBuilder(env.getProperty("apps[0].config.serverUrl"))
        .perPage(Integer.parseInt(env.getProperty("apps[0].config.perPage")))
        .personalAccessToken(env.getProperty("apps[0].config.personalAccessToken"));
    if (StringUtils.hasText(env.getProperty("proxy.host"))) {
      glcBuilder = glcBuilder.proxyHost(env.getProperty("proxy.host"))
          .proxyPort(Integer.parseInt(env.getProperty("proxy.port")));
    }
    GitLabClient glClient = glcBuilder.build();

    GitLabBuilder glBuilder = new GitLabBuilder(glClient, null)
        .groupRegExp(env.getProperty("apps[0].config.group.regExp"))
        .excludedUsers(env.getProperty("apps[0].config.sync.excludedUsers"))
        .removeProjectMembers(Boolean.parseBoolean(env.getProperty("apps[0].config.sync.removeProjectMembers")))
        .providerName(env.getProperty("apps[0].config.provider.name"))
        .providerUidAttribute(env.getProperty("apps[0].config.provider.uidAttribute"));
    gitlab = glBuilder.build();
  }

  @Test
  public void testSync() {
    List<IdpGroup> groups = kcClient.getGroups(env.getProperty("apps[0].config.group.search"));
    assertThat(groups).isNotNull();
    for (IdpGroup group : groups) {
      List<IdpUser> members = kcClient.getGroupMembers(group);
      assertThat(members).isNotNull();
      for (IdpUser member : members) {
        member.addGroup(group);
        group.addMember(member);
      }
    }
    assertTrue(gitlab.sync(groups));
  }

  @Test
  public void testStatistics() {
    List<AppStatistics> statisticsList = gitlab.getStatistics();
    assertThat(statisticsList).isNotNull().isNotEmpty();
    AppStatistics statistics = statisticsList.get(0);
    assertThat(statistics.getStatistics()).isNotEmpty().containsKey("members");
    assertThat(statistics.getStatistics().get("members")).isNotEmpty().usingComparator(INTEGER_STRING_COMPARATOR)
        .isGreaterThan("0");
    assertThat(statistics.getStatistics()).isNotEmpty().containsKey("repository_size");
  }
}
