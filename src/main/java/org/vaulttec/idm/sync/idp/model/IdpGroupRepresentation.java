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
package org.vaulttec.idm.sync.idp.model;

/**
 * Representation of the application-specific information encoded in an IdP
 * group name:
 * <ul>
 * <li><code>organizationName</code>: name of GitLab group or Mattermost team
 * <li><code>role</code>: permission in GitLab group or role in Mattermost team
 */
public class IdpGroupRepresentation {

  private String organizationName;
  private String role;

  public IdpGroupRepresentation(String name, String role) {
    this.organizationName = name;
    this.role = role;
  }

  public String getOrganizationName() {
    return organizationName;
  }

  public String getRole() {
    return role;
  }

  @Override
  public String toString() {
    return organizationName + ": " + role;
  }
}
