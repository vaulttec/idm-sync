/*
 * IDM Syncronizer
 * Copyright (c) 2019 Torsten Juergeleit
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
package org.vaulttec.idm.sync.app.model;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

public class AppStatistics {

  private final String organizationName;
  private final Map<String, String> statistics;

  public AppStatistics(String organisationName) {
    this.organizationName = organisationName;
    this.statistics = new LinkedHashMap<String, String>();
  }

  public String getOrganizationName() {
    return organizationName;
  }

  @JsonAnyGetter
  public Map<String, String> getStatistics() {
    return statistics;
  }

  @JsonAnySetter
  public void addStatistic(String key, String value) {
    statistics.put(key, value);
  }

  public void addStatistics(Map<String, String> statistics) {
    this.statistics.putAll(statistics);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((organizationName == null) ? 0 : organizationName.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    AppStatistics other = (AppStatistics) obj;
    if (organizationName == null) {
      if (other.organizationName != null)
        return false;
    } else if (!organizationName.equals(other.organizationName))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return organizationName + " " + statistics;
  }
}
