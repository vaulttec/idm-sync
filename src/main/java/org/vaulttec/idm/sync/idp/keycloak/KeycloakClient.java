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

import java.io.IOException;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class KeycloakClient extends AbstractRestClient {

  private static final Logger LOG = LoggerFactory.getLogger(KeycloakClient.class);

  protected static final ParameterizedTypeReference<List<IdpGroup>> RESPONSE_TYPE_GROUPS = new ParameterizedTypeReference<List<IdpGroup>>() {
  };
  protected static final ParameterizedTypeReference<List<IdpUser>> RESPONSE_TYPE_USERS = new ParameterizedTypeReference<List<IdpUser>>() {
  };

  private final String realm;
  private final HttpEntity<String> loginEntity;
  private final ObjectMapper mapper;

  KeycloakClient(String serverUrl, int perPage, String realm, String clientId, String clientSecret, String proxyHost,
      int proxyPort) {
    super(serverUrl, perPage, proxyHost, proxyPort);
    LOG.debug("Init: serverUrl={}, perPage={}, realm={}, clientId={}, proxyHost={}, proxyPort={}", serverUrl, perPage,
        realm, clientId, proxyHost, proxyPort);
    this.realm = realm;
    this.loginEntity = createLoginEntity(clientId, clientSecret);
    this.mapper = new ObjectMapper();
  }

  @Override
  protected String getApiPath() {
    return "/auth";
  }

  @Override
  protected <T> List<T> makeReadListApiCall(String apiCall, ParameterizedTypeReference<List<T>> typeReference,
      Map<String, String> uriVariables) {
    int first = 0;
    String url = getApiUrl(apiCall + (apiCall.contains("?") ? "&" : "?") + "first={first}&max={perPage}");
    uriVariables.put("first", Integer.toString(first));
    uriVariables.put("perPage", perPageAsString());
    try {
      List<T> entities;
      ResponseEntity<List<T>> response = restTemplate.exchange(url, HttpMethod.GET, authenticationEntity, typeReference,
          uriVariables);
      if (response.getBody().size() < perPage) {
        entities = response.getBody();
      } else {
        entities = new ArrayList<>(response.getBody());
        do {
          first += perPage;
          uriVariables.put("first", Integer.toString(first));
          response = restTemplate.exchange(url, HttpMethod.GET, authenticationEntity, typeReference, uriVariables);
          entities.addAll(response.getBody());
        } while (response.getBody().size() == perPage);
        return entities;
      }
      return entities;
    } catch (RestClientException e) {
      LOG.error("API call {} '{}' {} failed", "GET", url, uriVariables, e);
    }
    return null;
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
    String apiCall = "/realms/{realm}/protocol/openid-connect/token";
    Map<String, String> uriVariables = createUriVariables("realm", realm);
    ObjectNode node = makeWriteApiCall(apiCall, loginEntity, ObjectNode.class, uriVariables);
    if (node != null) {
      LOG.debug("response={}", node);
      if (node.has("access_token")) {
        String authToken = node.get("access_token").asText();
        prepareAuthenticationEntity("Authorization", "Bearer " + authToken);
        return true;
      } else {
        LOG.error("No 'access_token' property in JSON response");
      }
    }
    return false;
  }

  public List<IdpUser> getUsers(String search) {
    if (authenticationEntity == null) {
      throw new IllegalStateException("Authentication required");
    }
    LOG.debug("Retrieving users: search={}", search);
    String apiCall = "/admin/realms/{realm}/users";
    Map<String, String> uriVariables = createUriVariables("realm", realm);
    if (StringUtils.hasText(search)) {
      apiCall += "?search={search}";
      uriVariables.put("search", search);
    }
    return makeReadApiCall(apiCall, RESPONSE_TYPE_USERS, uriVariables);
  }

  public boolean updateUserAttributes(IdpUser user, Map<String, List<String>> attributes) {
    if (authenticationEntity == null) {
      throw new IllegalStateException("Authentication required");
    }
    LOG.debug("Updating user ({}) attributes: attributes={}", user.getUsername(), attributes);
    String apiCall = "/admin/realms/{realm}/users/{userId}";
    Map<String, String> uriVariables = createUriVariables("realm", realm, "userId", user.getId());
    updateMultiValueMap(user.getAttributes(), attributes);
    try {
      JsonNode userAttributesNode = mapper.valueToTree(user.getAttributes());
      HttpEntity<String> entity = new HttpEntity<String>(
          "{\"attributes\": " + mapper.writeValueAsString(userAttributesNode) + "}", authenticationEntity.getHeaders());
      return makeWriteApiCall(apiCall, HttpMethod.PUT, entity, uriVariables);
    } catch (IOException e) {
      LOG.error("Invalid user attribues", e);
    }
    return false;
  }

  public boolean updateRequiredActions(IdpUser user, List<String> requiredActions) {
    if (authenticationEntity == null) {
      throw new IllegalStateException("Authentication required");
    }
    LOG.debug("Updating user ({}) required actions: requiredActions={}", user.getUsername(), requiredActions);
    String apiCall = "/admin/realms/{realm}/users/{userId}";
    Map<String, String> uriVariables = createUriVariables("realm", realm, "userId", user.getId());
    try {
      JsonNode requiredActionsNode = mapper.valueToTree(requiredActions);
      HttpEntity<String> entity = new HttpEntity<String>(
          "{\"requiredActions\": " + mapper.writeValueAsString(requiredActionsNode) + "}",
          authenticationEntity.getHeaders());
      return makeWriteApiCall(apiCall, HttpMethod.PUT, entity, uriVariables);
    } catch (IOException e) {
      LOG.error("Invalid required actions", e);
    }
    return false;
  }

  public List<IdpGroup> getGroups(String search) {
    if (authenticationEntity == null) {
      throw new IllegalStateException("Authentication required");
    }
    LOG.debug("Retrieving groups: search={}", search);
    String apiCall = "/admin/realms/{realm}/groups";
    Map<String, String> uriVariables = createUriVariables("realm", realm);
    if (StringUtils.hasText(search)) {
      apiCall += "?search={search}";
      uriVariables.put("search", search);
    }
    return makeReadApiCall(apiCall, RESPONSE_TYPE_GROUPS, uriVariables);
  }

  public boolean updateGroupAttributes(IdpGroup group, Map<String, List<String>> attributes) {
    if (authenticationEntity == null) {
      throw new IllegalStateException("Authentication required");
    }
    LOG.debug("Updating group ({}) attributes: attributes={}", group.getPath(), attributes);
    String apiCall = "/admin/realms/{realm}/groups/{groupId}";
    Map<String, String> uriVariables = createUriVariables("realm", realm, "groupId", group.getId());
    updateMultiValueMap(group.getAttributes(), attributes);
    try {
      JsonNode attributesNode = mapper.valueToTree(group.getAttributes());
      HttpEntity<String> entity = new HttpEntity<String>(
          "{\"attributes\": " + mapper.writeValueAsString(attributesNode) + "}", authenticationEntity.getHeaders());
      return makeWriteApiCall(apiCall, HttpMethod.PUT, entity, uriVariables);
    } catch (IOException e) {
      LOG.error("Invalid group attribues", e);
    }
    return false;
  }

  public List<IdpUser> getGroupMembers(IdpGroup group) {
    if (authenticationEntity == null) {
      throw new IllegalStateException("Authentication required");
    }
    if (group == null) {
      throw new IllegalStateException("Group required");
    }
    LOG.debug("Retrieving group members from group '{}", group.getPath());
    String apiCall = "/admin/realms/{realm}/groups/{groupId}/members";
    Map<String, String> uriVariables = createUriVariables("realm", realm, "groupId", group.getId());
    return makeReadApiCall(apiCall, RESPONSE_TYPE_USERS, uriVariables);
  }

  protected void prepareAuthenticationEntity(String headerName, String headerValue) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set(headerName, headerValue);
    authenticationEntity = new HttpEntity<String>(headers);
  }

  private HttpEntity<String> createLoginEntity(String clientId, String clientSecret) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    String auth = clientId + ":" + clientSecret;
    String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(Charset.forName("US-ASCII")));
    headers.set("Authorization", "Basic " + encodedAuth);
    return new HttpEntity<String>("grant_type=client_credentials", headers);
  }

  private void updateMultiValueMap(Map<String, List<String>> existing, Map<String, List<String>> additional) {
    for (String key : additional.keySet()) {
      List<String> additionalValues = additional.get(key);
      if (additionalValues == null) {
        existing.remove(key);
      } else {
        existing.put(key, new ArrayList<>(additionalValues));
      }
    }
  }
}
