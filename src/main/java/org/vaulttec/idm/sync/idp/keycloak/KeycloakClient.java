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

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.vaulttec.idm.sync.app.AbstractRestClient;
import org.vaulttec.idm.sync.idp.IdpGroup;
import org.vaulttec.idm.sync.idp.IdpUser;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class KeycloakClient extends AbstractRestClient {

  private static final Logger LOG = LoggerFactory.getLogger(KeycloakClient.class);

  protected static final ParameterizedTypeReference<List<IdpGroup>> RESPONSE_TYPE_GROUPS = new ParameterizedTypeReference<List<IdpGroup>>() {
  };
  protected static final ParameterizedTypeReference<List<IdpUser>> RESPONSE_TYPE_USERS = new ParameterizedTypeReference<List<IdpUser>>() {
  };

  private final String realm;
  private final HttpEntity<String> loginEntity;

  KeycloakClient(String serverUrl, int perPage, String realm, String clientId, String clientSecret, String proxyHost,
      int proxyPort) {
    super(serverUrl, perPage, proxyHost, proxyPort);
    LOG.debug("Init: serverUrl={}, perPage={}, realm={}, clientId={}, proxyHost={}, proxyPort={}", serverUrl, perPage,
        realm, clientId, proxyHost, proxyPort);
    this.realm = realm;
    this.loginEntity = createLoginEntity(clientId, clientSecret);
  }

  /**
   * Authenticate with Keycloak via <code>confidential</code> <a href=
   * "https://www.keycloak.org/docs/latest/server_admin/index.html#oidc-clients">OpenID
   * Connect client</a> with <a href=
   * "https://www.keycloak.org/docs/latest/server_admin/index.html#_service_accounts">service
   * account</a>.
   */
  public boolean authenticate() {
    LOG.info("Authenticating");
    String loginUrl = serverUrl + "/realms/{realm}/protocol/openid-connect/token";
    Map<String, String> uriVariables = createUriVariables("realm", realm);
    try {
      ObjectNode response = restTemplate.postForObject(loginUrl, loginEntity, ObjectNode.class, uriVariables);
      LOG.debug("response={}", response);
      if (response.has("access_token")) {
        String authToken = response.get("access_token").asText();
        prepareAuthenticationEntity("Authorization", "Bearer " + authToken);
        return true;
      } else {
        LOG.error("No 'access_token' property in JSON response");
      }
    } catch (RestClientException e) {
      LOG.error("Authentication failed", e);
    }
    return false;
  }

  public List<IdpGroup> getGroupsWithMembers(String search) {
    LOG.debug("Retrieving groups with members: search={}", search);
    List<IdpGroup> groups = getGroups(search);
    if (groups != null) {
      for (IdpGroup group : groups) {
        List<IdpUser> members = getGroupMembers(group);
        if (members != null) {
          for (IdpUser member : members) {
            member.addGroup(group);
            group.addMember(member);
          }
        }
      }
      return groups;
    }
    return null;
  }

  public List<IdpGroup> getGroups(String search) {
    if (authenticationEntity == null) {
      throw new IllegalStateException("Authentication required");
    }
    LOG.debug("Retrieving groups: search={}", search);
    int first = 0;
    String groupsUrl = serverUrl + "/admin/realms/{realm}/groups?first={first}&max={perPage}";
    Map<String, String> uriVariables = createUriVariables("realm", realm, "first", Integer.toString(first), "perPage",
        perPageAsString());
    if (StringUtils.hasText(search)) {
      groupsUrl += "&search={search}";
      uriVariables.put("search", search);
    }
    try {
      ResponseEntity<List<IdpGroup>> response = restTemplate.exchange(groupsUrl, HttpMethod.GET, authenticationEntity,
          RESPONSE_TYPE_GROUPS, uriVariables);
      if (response.getBody().size() < perPage) {
        return response.getBody();
      } else {
        List<IdpGroup> groups = new ArrayList<>(response.getBody());
        do {
          first += perPage;
          uriVariables.put("first", Integer.toString(first));
          response = restTemplate.exchange(groupsUrl, HttpMethod.GET, authenticationEntity, RESPONSE_TYPE_GROUPS,
              uriVariables);
          groups.addAll(response.getBody());
        } while (response.getBody().size() == perPage);
        return groups;
      }
    } catch (RestClientException e) {
      LOG.error("Retrieving groups failed", e);
    }
    return null;
  }

  public List<IdpUser> getGroupMembers(IdpGroup group) {
    if (authenticationEntity == null) {
      throw new IllegalStateException("Authentication required");
    }
    if (group == null) {
      throw new IllegalStateException("Group required");
    }
    LOG.debug("Retrieving group members from group '{}", group.getPath());
    int first = 0;
    String groupMembersUrl = serverUrl + "/admin/realms/{realm}/groups/{groupId}/members?first={first}&max={perPage}";
    Map<String, String> uriVariables = createUriVariables("realm", realm, "groupId", group.getId(), "first",
        Integer.toString(first), "perPage", perPageAsString());
    try {
      ResponseEntity<List<IdpUser>> response = restTemplate.exchange(groupMembersUrl, HttpMethod.GET,
          authenticationEntity, RESPONSE_TYPE_USERS, uriVariables);
      if (response.getBody().size() < perPage) {
        return response.getBody();
      } else {
        List<IdpUser> members = new ArrayList<>(response.getBody());
        do {
          first += perPage;
          uriVariables.put("first", Integer.toString(first));
          response = restTemplate.exchange(groupMembersUrl, HttpMethod.GET, authenticationEntity, RESPONSE_TYPE_USERS,
              uriVariables);
          members.addAll(response.getBody());
        } while (response.getBody().size() == perPage);
        return members;
      }
    } catch (RestClientException e) {
      LOG.error("Retrieving group members failed", e);
    }
    return null;
  }

  private HttpEntity<String> createLoginEntity(String clientId, String clientSecret) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    String auth = clientId + ":" + clientSecret;
    String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(Charset.forName("US-ASCII")));
    headers.set("Authorization", "Basic " + encodedAuth);
    return new HttpEntity<String>("grant_type=client_credentials", headers);
  }
}
