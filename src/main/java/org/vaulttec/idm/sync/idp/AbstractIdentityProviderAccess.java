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
package org.vaulttec.idm.sync.idp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.vaulttec.idm.sync.app.Application;

public abstract class AbstractIdentityProviderAccess {

  protected final IdentityProvider idp;
  protected final List<Application> applications;

  public AbstractIdentityProviderAccess(IdentityProvider idp, List<Application> apps) {
    this.idp = idp;
    this.applications = apps;
  }

  /**
   * Returns a map with all users which are members of the given list of groups.
   * <p>
   * These groups and users or linked with each other by the attributes
   * <code>IdpGroup.members</code> and <code>IdpUser.groups</code>.
   */
  protected Map<String, IdpUser> retrieveMembersForGroups(List<IdpGroup> groups) {
    Map<String, IdpUser> users = new HashMap<>();
    for (IdpGroup group : groups) {
      List<IdpUser> members = idp.getGroupMembers(group);
      if (members == null) {
        return null;
      } else {
        for (IdpUser member : members) {
          if (!users.containsKey(member.getId())) {
            users.put(member.getId(), member);
          }
          IdpUser user = users.get(member.getId());
          user.addGroup(group);
          group.addMember(user);
        }
      }
    }
    return users;
  }
}
