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
package org.vaulttec.idm.sync.idp.keycloak;

import java.util.Map;

import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;
import org.vaulttec.idm.sync.idp.IdentityProvider;
import org.vaulttec.idm.sync.idp.IdentityProviderFactory;

public class KeycloakFactory implements IdentityProviderFactory {

  @Override
  public IdentityProvider createIdentityProvider(Map<String, String> config, Environment env) {
    KeycloakClientBuilder builder = new KeycloakClientBuilder(config.get("serverUrl"))
        .perPage(Integer.valueOf(config.get("perPage"))).realm(config.get("realm")).clientId(config.get("client.id"))
        .clientSecret(config.get("client.secret"));
    if (StringUtils.hasText(env.getProperty("proxy.host"))) {
      builder = builder.proxyHost(env.getProperty("proxy.host"))
          .proxyPort(Integer.parseInt(env.getProperty("proxy.port")));
    }
    KeycloakClient client = builder.build();

    return new Keycloak(client);
  }
}
