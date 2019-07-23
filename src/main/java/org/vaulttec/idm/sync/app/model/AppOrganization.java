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

import java.util.ArrayList;
import java.util.List;

public class AppOrganization {

  private final String name;
  private final List<String> roles;

  public AppOrganization(String name) {
    this.name = name;
    this.roles = new ArrayList<String>();
  }

  public AppOrganization(String name, String role) {
    this(name);
    addRole(role);
  }

  public String getName() {
    return name;
  }

  public List<String> getRoles() {
    return roles;
  }

  public void addRole(String role) {
    if (!roles.contains(role)) {
      roles.add(role);
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
    AppOrganization other = (AppOrganization) obj;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return name + " " + roles;
  }
}
