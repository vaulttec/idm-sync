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
package org.vaulttec.idm.sync.app.gitlab.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.vaulttec.idm.sync.idp.model.IdpUser;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GLUser {

  private static final Pattern PROJECT_BOT = Pattern.compile("^project_\\d+_bot\\d*$");
  private static final Pattern GROUP_BOT = Pattern.compile("^group_\\d+_bot\\d*$");

  private final IdpUser idpUser;
  private String id;
  private String username;
  private String name;
  private String email;
  @JsonAlias("is_admin")
  private boolean admin;
  private Boolean bot;
  private GLState state;
  @JsonAlias("access_level")
  private GLPermission permission;
  private String provider;
  private String externUid;
  private List<GLIdentity> identities;
  private Map<String, GLGroup> groups = new HashMap<>();

  public GLUser() {
    this.idpUser = null;
  }

  public GLUser(IdpUser idpUser) {
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

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public boolean isAdmin() {
    return admin;
  }

  public void setAdmin(boolean admin) {
    this.admin = admin;
  }

  public Boolean getBot() {
    return bot;
  }

  public void setBot(Boolean bot) {
    this.bot = bot;
  }

  public boolean isBot() {
    if (bot == null) {
      return PROJECT_BOT.matcher(username).matches() || GROUP_BOT.matcher(username).matches();
    }
    return bot.booleanValue();
  }

  public GLState getState() {
    return state;
  }

  public void setState(GLState state) {
    this.state = state;
  }

  public GLPermission getPermission() {
    return permission;
  }

  public void setPermission(GLPermission permission) {
    this.permission = permission;
  }

  public String getProvider() {
    return provider;
  }

  public void setProvider(String provider) {
    this.provider = provider;
  }

  public String getExternUid() {
    return externUid;
  }

  public void setExternUid(String externUid) {
    this.externUid = externUid;
  }

  public List<GLIdentity> getIdentities() {
    return identities;
  }

  @JsonSetter("identities")
  public void setIdentities(List<GLIdentity> identities) {
    this.identities = identities;
  }

  public void addIdentity(String provider, String externUid) {
    if (identities == null) {
      identities = new ArrayList<>();
    }
    GLIdentity identity = new GLIdentity();
    identity.setProvider(provider);
    identity.setExternUid(externUid);
    identities.add(identity);
  }

  public Map<String, GLGroup> getGroups() {
    return groups;
  }

  public GLGroup getGroup(String path) {
    return groups.get(path);
  }

  public void addGroup(GLGroup group) {
    this.groups.put(group.getPath(), group);
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
    GLUser other = (GLUser) obj;
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
    return username + (state != null ? "(" + state + ")" : "");
  }
}
