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
package org.vaulttec.idm.sync;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.actuate.audit.InMemoryAuditEventRepository;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "sync")
public class SyncConfig {
  private String emailDomain;
  private int auditEventRepositoryCapacity;
  private List<String> enabledApps = new ArrayList<>();

  public String getEmailDomain() {
    return emailDomain;
  }

  public void setEmailDomain(String emailDomain) {
    this.emailDomain = emailDomain;
  }

  public int getAuditEventRepositoryCapacity() {
    return auditEventRepositoryCapacity;
  }

  public void setAuditEventRepositoryCapacity(int auditEventRepositoryCapacity) {
    this.auditEventRepositoryCapacity = auditEventRepositoryCapacity;
  }

  public List<String> getEnabledApps() {
    return enabledApps;
  }

  @Bean
  public InMemoryAuditEventRepository auditEventRepository() throws Exception {
    return new InMemoryAuditEventRepository(getAuditEventRepositoryCapacity());
  }
}