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

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StringUtils;

@ActiveProfiles("test")
@IfProfileValue(name = "run.integration.tests", value = "true")
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
public class MattermostClientIntegrationTest {

  private static final Logger LOG = LoggerFactory.getLogger(MattermostClientIntegrationTest.class);

  @Autowired
  private Environment env;

  private MattermostClient client;

  @Before
  public void setup() {
    MattermostClientBuilder builder = new MattermostClientBuilder(env.getProperty("apps[1].config.serverUrl"))
        .perPage(Integer.parseInt(env.getProperty("apps[1].config.perPage")))
        .personalAccessToken(env.getProperty("apps[1].config.personalAccessToken"));
    if (StringUtils.hasText(env.getProperty("proxy.host"))) {
      builder = builder.proxyHost(env.getProperty("proxy.host"))
          .proxyPort(Integer.parseInt(env.getProperty("proxy.port")));
    }
    client = builder.build();
  }

  @Test
  public void testGetGroupsWithMembers() {
    List<MMTeam> teams = client.getTeamsWithMembers();
    assertThat(teams).isNotNull().isNotEmpty();
    for (MMTeam team : teams) {
      LOG.info("{} ({}):", team.getName(), team.getMembers().size());
      for (MMUser member : team.getMembers()) {
        LOG.info("   {}", member);
      }
    }
  }

  @Test
  public void testGetUsers() {
    List<MMUser> users = client.getUsers();
    assertThat(users).isNotNull().isNotEmpty();
    for (MMUser user : users) {
      LOG.info("{}", user);
    }
  }

  @Test
  @Ignore
  public void testCreateUser() {
    MMUser user = client.createUser("x000042", "John", "Doo", "john.doo@acme.com", "gitlab", "x000042");
    assertThat(user).isNotNull().hasFieldOrPropertyWithValue("username", "x000042");
  }

  @Test
  @Ignore
  public void testCreateTeam() {
    MMTeam team = client.createTeam("test", "test Team");
    assertThat(team).isNotNull().hasFieldOrPropertyWithValue("name", "test");
  }

  @Test
  @Ignore
  public void testAddMemberToTeam() {
    MMTeam team = client.createTeam("test2", "test2 Team");
    assertThat(team).isNotNull().hasFieldOrPropertyWithValue("name", "test2");
    MMUser user = client.createUser("x000043", "John", "Doo3", "john.doo3@acme.com", "gitlab", "x000043");
    assertThat(user).isNotNull().hasFieldOrPropertyWithValue("username", "x000043");
    assertTrue(client.addMemberToTeam(team, user));
  }

  @Test
  @Ignore
  public void testUpdateTeamMemberRoles() {
    MMTeam team = client.createTeam("test3", "test3 Team");
    assertThat(team).isNotNull().hasFieldOrPropertyWithValue("name", "test3");
    MMUser user = client.createUser("x000044", "John", "Doo4", "john.doo2@acme.com", "gitlab", "x000044");
    assertThat(user).isNotNull().hasFieldOrPropertyWithValue("username", "x000044");
    assertTrue(client.addMemberToTeam(team, user));
    assertTrue(client.updateTeamMemberRoles(team, user, Arrays.asList(MMRole.TEAM_ADMIN)));
  }

  @Test
  @Ignore
  public void testRemoveMemberFromTeam() {
    MMTeam team = client.createTeam("test4", "test4 Team");
    assertThat(team).isNotNull().hasFieldOrPropertyWithValue("name", "test4");
    MMUser user = client.createUser("x000045", "John", "Doo5", "john.doo5@acme.com", "gitlab", "x000045");
    assertThat(user).isNotNull().hasFieldOrPropertyWithValue("username", "x000045");
    assertTrue(client.addMemberToTeam(team, user));
    assertTrue(client.removeMemberFromTeam(team, user));
  }

  @Test
  @Ignore
  public void testUpdateUserActiveStatus() {
    MMUser user = client.createUser("x000046", "John", "Doo6", "john.doo6@acme.com", "gitlab", "x000046");
    assertThat(user).isNotNull().hasFieldOrPropertyWithValue("username", "x000046");
    assertTrue(client.updateUserActiveStatus(user, false));
    assertTrue(client.updateUserActiveStatus(user, true));
  }

  @Test
  public void testTeamChannels() {
    List<MMTeam> teams = client.getTeamsWithMembers();
    assertThat(teams).isNotNull().isNotEmpty();
    MMTeam team = teams.get(0);
    List<MMTeamChannel> channels = client.getTeamChannels(team);
    assertThat(channels).isNotNull().isNotEmpty();
  }
}
