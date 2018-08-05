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
package org.vaulttec.idm.sync.app;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties
public class ApplicationConfig {
  private final Environment env;
  private List<App> apps = new ArrayList<>();

  ApplicationConfig(Environment env) {
    this.env = env;
  }

  public List<App> getApps() {
    return apps;
  }

  @Bean
  public List<Application> applications() throws InstantiationException, IllegalAccessException {
    List<Application> applications = new ArrayList<>(apps.size());
    for (App app : apps) {
      app.getFactory().newInstance().createApplication(app.getConfig(), env);
    }
    return applications;
  }

  protected static class App {
    private Class<ApplicationFactory> factory;
    private Map<String, String> config = new HashMap<>();

    public Class<ApplicationFactory> getFactory() {
      return factory;
    }

    public void setFactory(Class<ApplicationFactory> factory) {
      this.factory = factory;
    }

    public Map<String, String> getConfig() {
      return config;
    }
  }
}
