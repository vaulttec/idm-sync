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

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "idp")
public class IdentityProviderConfig {
  private final Environment env;
  private Class<IdentityProviderFactory> factory;
  private Map<String, String> config = new HashMap<>();

  IdentityProviderConfig(Environment env) {
    this.env = env;
  }

  public Class<IdentityProviderFactory> getFactory() {
    return factory;
  }

  public void setFactory(Class<IdentityProviderFactory> factory) {
    this.factory = factory;
  }

  public Map<String, String> getConfig() {
    return config;
  }

  @Bean
  public IdentityProvider identityProvider() throws InstantiationException, IllegalAccessException {
    if (factory == null) {
      throw new IllegalStateException("No identity provider factory defined in configuration");
    }
    return factory.newInstance().createIdentityProvider(config, env);
  }
}
