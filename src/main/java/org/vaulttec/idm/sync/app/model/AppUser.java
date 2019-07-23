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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class AppUser {

  private final String username;
  private final String name;
  private final Map<String, AppOrganization> organizations;

  public AppUser(String username, String name) {
    this.username = username;
    this.name = name;
    this.organizations = new HashMap<String, AppOrganization>();
  }

  public String getUsername() {
    return username;
  }

  public String getName() {
    return name;
  }

  public Collection<AppOrganization> getOrganizations() {
    return organizations.values();
  }

  public void addOrganization(AppOrganization newOrganization) {
    AppOrganization existingOrganization = organizations.get(newOrganization.getName());
    if (existingOrganization == null) {
      existingOrganization = new AppOrganization(newOrganization.getName());
      organizations.put(existingOrganization.getName(), existingOrganization);
    }
    for (String role : newOrganization.getRoles()) {
      existingOrganization.addRole(role);
    }
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((name == null) ? 0 : name.hashCode());
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
    AppUser other = (AppUser) obj;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return username  + "(" + name + ") " + organizations;
  }
}
