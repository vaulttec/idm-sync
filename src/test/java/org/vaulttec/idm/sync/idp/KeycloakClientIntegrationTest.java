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

import java.util.List;

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

  @Autowired
  private Environment env;

  @Test
  public void testGetGruoupsWithMembers() {
    KeycloakClientBuilder builder = new KeycloakClientBuilder(env.getProperty("idp.config.serverUrl"))
        .perPage(Integer.valueOf(env.getProperty("idp.config.perPage"))).realm(env.getProperty("idp.config.realm"))
        .clientId(env.getProperty("idp.config.client.id")).clientSecret(env.getProperty("idp.config.client.secret"));
    if (StringUtils.hasText(env.getProperty("proxy.host"))) {
      builder = builder.proxyHost(env.getProperty("proxy.host"))
          .proxyPort(Integer.parseInt(env.getProperty("proxy.port")));
    }
    KeycloakClient client = builder.build();

    assertTrue(client.authenticate());

    List<IdpGroup> groups = client.getGroupsWithMembers(env.getProperty("idp.config.group.search"));
    assertThat(groups).isNotNull().isNotEmpty();
    for (IdpGroup group : groups) {
      LOG.info("{} ({}):", group.getPath(), group.getMembers().size());
      for (IdpUser user : group.getMembers()) {
        LOG.info("   {} ({}) - {}:", user.getUsername(), user.getName(), user.getEmail());
      }
    }
  }
}
