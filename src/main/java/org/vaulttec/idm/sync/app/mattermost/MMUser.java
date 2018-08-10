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
package org.vaulttec.idm.sync.app.mattermost;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.vaulttec.idm.sync.idp.IdpUser;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MMUser {

  private final IdpUser idpUser;
  private String id;
  private String username;
  @JsonAlias("first_name")
  private String firstName;
  @JsonAlias("last_name")
  private String lastName;
  private String email;
  @JsonAlias("auth_service")
  private String authService;
  @JsonAlias("auth_data")
  private String authData;
  private List<MMRole> roles;
  @JsonAlias("delete_at")
  private String deleteAt;
  private Map<String, MMTeam> teams = new HashMap<>();

  MMUser() {
    this.idpUser = null;
  }
  
  MMUser(IdpUser idpUser) {
    this.idpUser = idpUser;
  }

  public IdpUser getIdpUser() {
    return idpUser;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getAuthService() {
    return authService;
  }

  public void setAuthService(String authService) {
    this.authService = authService;
  }

  public String getAuthData() {
    return authData;
  }

  public void setAuthData(String authData) {
    this.authData = authData;
  }

  public List<MMRole> getRoles() {
    return roles;
  }

  @JsonSetter("roles")
  public void setRoles(String roles) {
    this.roles = MMRole.fromJson(roles);
  }

  public boolean isActive() {
    return "0".equals(deleteAt) ? true : false;
  }

  public String getDeleteAt() {
    return deleteAt;
  }

  public void setDeleteAt(String deleteAt) {
    this.deleteAt = deleteAt;
  }

  public Map<String, MMTeam> getTeams() {
    return teams;
  }

  public MMTeam getTeam(String name) {
    return teams.get(name);
  }

  public void addTeam(MMTeam team) {
    this.teams.put(team.getName(), team);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((username == null) ? 0 : username.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    MMUser other = (MMUser) obj;
    if (username == null) {
      if (other.username != null) {
        return false;
      }
    } else if (!username.equals(other.username)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return username + (roles != null ? " " + roles : "");
  }
}
