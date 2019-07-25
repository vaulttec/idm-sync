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
package org.vaulttec.idm.sync.idp.keycloak;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.vaulttec.idm.sync.idp.IdentityProvider;
import org.vaulttec.idm.sync.idp.model.IdpGroup;
import org.vaulttec.idm.sync.idp.model.IdpUser;

public class Keycloak implements IdentityProvider {

  private final KeycloakClient client;

  Keycloak(KeycloakClient client) {
    this.client = client;
  }

  @Override
  public String getId() {
    return "keycloak";
  }

  @Override
  public String getName() {
    return "Keycloak";
  }

  @Override
  public boolean authenticate() {
    return client.authenticate();
  }

  @Override
  public List<IdpUser> getUsers(String search) {
    return client.getUsers(search);
  }

  @Override
  public boolean updateUserAttributes(IdpUser user, Map<String, List<String>> attributes) {
    return client.updateUserAttributes(user, attributes);
  }

  @Override
  public boolean removeRequiredActions(IdpUser user) {
    return client.updateRequiredActions(user, Collections.emptyList());
  }

  @Override
  public List<IdpGroup> getGroups(String search) {
    return client.getGroups(search);
  }

  @Override
  public boolean updateGroupAttributes(IdpGroup group, Map<String, List<String>> attributes) {
    return client.updateGroupAttributes(group, attributes);
  }

  @Override
  public List<IdpUser> getGroupMembers(IdpGroup group) {
    return client.getGroupMembers(group);
  }
}
