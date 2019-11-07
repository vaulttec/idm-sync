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

import java.util.List;

import org.junit.Before;
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
import org.vaulttec.idm.sync.app.gitlab.model.GLGroup;
import org.vaulttec.idm.sync.app.gitlab.model.GLIdentity;
import org.vaulttec.idm.sync.app.gitlab.model.GLPermission;
import org.vaulttec.idm.sync.app.gitlab.model.GLProject;
import org.vaulttec.idm.sync.app.gitlab.model.GLState;
import org.vaulttec.idm.sync.app.gitlab.model.GLUser;

@ActiveProfiles("test")
@IfProfileValue(name = "run.integration.tests", value = "true")
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
public class GitLabClientIntegrationTest {

  private static final Logger LOG = LoggerFactory.getLogger(GitLabClientIntegrationTest.class);

  @Autowired
  private Environment env;

  private GitLabClient client;

  @Before
  public void setup() {
    GitLabClientBuilder builder = new GitLabClientBuilder(env.getProperty("apps[0].config.serverUrl"))
        .perPage(Integer.parseInt(env.getProperty("apps[0].config.perPage")))
        .personalAccessToken(env.getProperty("apps[0].config.personalAccessToken"));
    if (StringUtils.hasText(env.getProperty("proxy.host"))) {
      builder = builder.proxyHost(env.getProperty("proxy.host"))
          .proxyPort(Integer.parseInt(env.getProperty("proxy.port")));
    }
    client = builder.build();
  }

  @Test
  public void testGetGroupsWithMembers() {
    List<GLGroup> groups = client.getGroupsWithMembers(null, false);
    assertThat(groups).isNotNull().isNotEmpty();
    for (GLGroup group : groups) {
      LOG.info("{} ({}):", group.getName(), group.getMembers().size());
      for (GLUser member : group.getMembers()) {
        LOG.info("   {} ({}) - {}", member.getUsername(), member.getPermission(), member.getState());
      }
      for (GLPermission permission : group.getPermissions()) {
        LOG.info("   {} {}", permission, group.getMembersByPermission(permission));
      }
    }
  }

  @Test
  public void testGetUsers() {
    List<GLUser> users = client.getUsers(null);
    assertThat(users).isNotNull().isNotEmpty();
    for (GLUser user : users) {
      LOG.info("{} ({}) - {}", user.getUsername(), user.getName(), user.getState());
    }
  }

  @Test
  public void testGetProjectsFromGroup() {
    List<GLGroup> groups = client.getGroups(null, false);
    assertThat(groups).isNotNull().isNotEmpty();
    for (GLGroup group : groups) {
      LOG.info("{}:", group.getName());
      List<GLProject> projects = client.getProjectsFromGroup(group, null);
      assertThat(projects).isNotNull();
      for (GLProject project : projects) {
        LOG.info("   {}", project.getName());
      }
    }
  }

  @Test
  public void testGetUsersFromProject() {
    List<GLGroup> groups = client.getGroups(null, false);
    assertThat(groups).isNotNull().isNotEmpty();
    for (GLGroup group : groups) {
      LOG.info("{}:", group.getName());
      List<GLProject> projects = client.getProjectsFromGroup(group, null);
      assertThat(projects).isNotNull();
      for (GLProject project : projects) {
        List<GLUser> users = client.getProjectUsers(project);
        assertThat(users).isNotNull();
        LOG.info("   {} {}", project.getName(), users);
      }
    }
  }

  @Test
  public void testCreateAndDeleteUser() {
    GLUser user = client.createUser("x000042", "John Doo", "john.doo@acme.com", null, null);
    assertThat(user).isNotNull();
    assertThat(client.getUsers("x000042")).isNotNull();
    assertThat(client.deleteUser(user, true)).isTrue();
    List<GLUser> users = client.getUsers("x000042");
    assertThat(users).isNotNull().hasSize(1);
    assertThat(users.get(0).getState()).isEqualTo(GLState.BLOCKED);
  }

  @Test
  public void testAddIdentityToUser() {
    GLUser user = client.createUser("x000042", "John Doo", "john.doo@acme.com", "foo1", "bar1");
    assertThat(user).isNotNull();
    assertThat(client.addIdentityToUser(user, "foo2", "bar2")).isTrue();
    List<GLUser> users = client.getUsers("x000042");
    assertThat(users).isNotNull().hasSize(1);
    List<GLIdentity> identities = users.get(0).getIdentities();
    assertThat(identities).isNotNull().hasSize(2);
    assertThat(identities.get(1).getProvider()).startsWith("foo");
    assertThat(identities.get(1).getExternUid()).startsWith("bar");
    assertThat(client.deleteUser(user, true)).isTrue();
  }

  @Test
  public void testGroupStatistics() {
    List<GLGroup> groups = client.getGroups(null, true);
    assertThat(groups).isNotNull().isNotEmpty();
    GLGroup group = groups.get(0);
    assertThat(group.getStatistics()).isNotEmpty().containsKey("repository_size");
  }
}
