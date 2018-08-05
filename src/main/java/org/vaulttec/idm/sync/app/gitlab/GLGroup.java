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
package org.vaulttec.idm.sync.app.gitlab;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GLGroup {

  private String id;
  private String path;
  private String name;
  private Map<String, GLUser> members = new HashMap<>();
  private MultiValueMap<GLPermission, GLUser> permissionedMembers = new LinkedMultiValueMap<>();

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public boolean isMember(GLUser user) {
    return this.members.containsKey(user.getUsername());
  }

  public GLUser getMember(String username) {
    return members.get(username);
  }

  public Collection<GLUser> getMembers() {
    return members.values();
  }

  public void addMember(GLUser user, GLPermission permission) {
    GLUser member = members.get(user.getUsername());
    if (member == null) {
      member = user;
      this.members.put(member.getUsername(), member);
    }
    this.permissionedMembers.add(permission, member);
  }

  public GLPermission getPermission(GLUser user) {
    GLUser member = members.get(user.getUsername());
    if (member != null) {
      GLPermission found = null;
      for (GLPermission permission : getPermissions()) {
        if (getMembersByPermission(permission).contains(member)
            && (found == null || permission.compareAccessLevel(found) > 0)) {
          found = permission;
        }
      }
      return found;
    }
    return null;
  }

  public Set<GLPermission> getPermissions() {
    return permissionedMembers.keySet();
  }

  public List<GLUser> getMembersByPermission(GLPermission permission) {
    return permissionedMembers.get(permission);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((path == null) ? 0 : path.hashCode());
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
    GLGroup other = (GLGroup) obj;
    if (path == null) {
      if (other.path != null) {
        return false;
      }
    } else if (!path.equals(other.path)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return path + "(" + members.size() + ")";
  }
}
