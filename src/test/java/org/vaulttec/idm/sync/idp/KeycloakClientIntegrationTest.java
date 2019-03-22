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
package org.vaulttec.idm.sync.idp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.vaulttec.idm.sync.idp.keycloak.KeycloakClient;
import org.vaulttec.idm.sync.idp.keycloak.KeycloakClientBuilder;

@ActiveProfiles("test")
@IfProfileValue(name = "run.integration.tests", value = "true")
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
public class KeycloakClientIntegrationTest {

  private static final Logger LOG = LoggerFactory.getLogger(KeycloakClientIntegrationTest.class);

  private static final String USER_SEARCH = "b0123";

  @Autowired
  private Environment env;

  private KeycloakClient client;

  @Before
  public void setup() {
    KeycloakClientBuilder builder = new KeycloakClientBuilder(env.getProperty("idp.config.serverUrl"))
        .perPage(Integer.valueOf(env.getProperty("idp.config.perPage"))).realm(env.getProperty("idp.config.realm"))
        .clientId(env.getProperty("idp.config.client.id")).clientSecret(env.getProperty("idp.config.client.secret"));
    if (StringUtils.hasText(env.getProperty("proxy.host"))) {
      builder = builder.proxyHost(env.getProperty("proxy.host"))
          .proxyPort(Integer.parseInt(env.getProperty("proxy.port")));
    }
    client = builder.build();
    assertTrue(client.authenticate());
  }

  @Test
  @Ignore
  public void testGetUsers() {
    List<IdpUser> users = client.getUsers(null);
    assertThat(users).isNotNull().isNotEmpty();
    for (IdpUser user : users) {
      LOG.info("   {} ({}) - {}", user.getUsername(), user.getName(), user.getEmail());
    }
  }

  @Test
  @Ignore
  public void testUpdateUserAttributes() {
    List<IdpUser> users = client.getUsers(USER_SEARCH);
    assertThat(users).isNotNull().isNotEmpty();

    Map<String, List<String>> attributes = new HashMap<>();
    attributes.put("testAttribute", Arrays.asList("testValue"));
    assertTrue(client.updateUserAttributes(users.get(0), attributes));

    users = client.getUsers(USER_SEARCH);
    assertThat(users).isNotNull().isNotEmpty();
    assertThat(users.get(0).getAttribute("testAttribute")).isEqualTo("testValue");

    attributes.put("testAttribute", Arrays.asList("testValue2"));
    assertTrue(client.updateUserAttributes(users.get(0), attributes));

    users = client.getUsers(USER_SEARCH);
    assertThat(users).isNotNull().isNotEmpty();
    assertThat(users.get(0).getAttribute("testAttribute")).isEqualTo("testValue2");

    attributes.put("testAttribute", null);
    assertTrue(client.updateUserAttributes(users.get(0), attributes));

    users = client.getUsers(USER_SEARCH);
    assertThat(users).isNotNull().isNotEmpty();
    assertThat(users.get(0).getAttribute("testAttribute")).isNull();
  }

  @Test
  @Ignore
  public void testUpdateRequiredActions() {
    List<IdpUser> users = client.getUsers(USER_SEARCH);
    assertThat(users).isNotNull().isNotEmpty();
    List<String> savedRequiredActions = users.get(0).getRequiredActions();

    List<String> requiredActions = Arrays.asList("CONFIGURE_TOTP");
    assertTrue(client.updateRequiredActions(users.get(0), requiredActions));

    users = client.getUsers(USER_SEARCH);
    assertThat(users).isNotNull().isNotEmpty();
    assertThat(users.get(0).getRequiredActions()).isEqualTo(requiredActions);

    assertTrue(client.updateRequiredActions(users.get(0), Collections.emptyList()));

    users = client.getUsers(USER_SEARCH);
    assertThat(users).isNotNull().isNotEmpty();
    assertThat(users.get(0).getRequiredActions()).isEqualTo(Collections.emptyList());

    assertTrue(client.updateRequiredActions(users.get(0), savedRequiredActions));

    users = client.getUsers(USER_SEARCH);
    assertThat(users).isNotNull().isNotEmpty();
    assertThat(users.get(0).getRequiredActions()).isEqualTo(savedRequiredActions);
  }

  @Test
  public void testGetGroups() {
    List<IdpGroup> groups = client.getGroups(env.getProperty("apps[0].config.group.search"));
    assertThat(groups).isNotNull().isNotEmpty();
    for (IdpGroup group : groups) {
      LOG.info("{}", group.getPath());
    }
  }

  @Test
  @Ignore
  public void testUpdateGroupAttributes() {
    List<IdpGroup> groups = client.getGroups(null);
    assertThat(groups).isNotNull().isNotEmpty();
    Map<String, List<String>> attributes = new HashMap<>();
    attributes.put("testAttribute", Arrays.asList("testValue"));
    assertTrue(client.updateGroupAttributes(groups.get(0), attributes));
    attributes.put("testAttribute", null);
    assertTrue(client.updateGroupAttributes(groups.get(0), attributes));
  }

  @Test
  public void testGetGroupMembers() {
    List<IdpGroup> groups = client.getGroups(env.getProperty("apps[0].config.group.search"));
    assertThat(groups).isNotNull().isNotEmpty();
    for (IdpGroup group : groups) {
      LOG.info("{}", group.getPath());
      List<IdpUser> users = client.getGroupMembers(group);
      for (IdpUser user : users) {
        LOG.info("   {} ({}) - {}:", user.getUsername(), user.getName(), user.getEmail());
      }
    }
  }
}
