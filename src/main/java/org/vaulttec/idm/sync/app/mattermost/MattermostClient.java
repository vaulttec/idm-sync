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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.vaulttec.idm.sync.app.AbstractRestClient;
import org.vaulttec.idm.sync.app.mattermost.model.MMRole;
import org.vaulttec.idm.sync.app.mattermost.model.MMTeam;
import org.vaulttec.idm.sync.app.mattermost.model.MMTeamChannel;
import org.vaulttec.idm.sync.app.mattermost.model.MMTeamMember;
import org.vaulttec.idm.sync.app.mattermost.model.MMUser;

public class MattermostClient extends AbstractRestClient {

  private static final Logger LOG = LoggerFactory.getLogger(MattermostClient.class);

  protected static final ParameterizedTypeReference<List<MMTeam>> RESPONSE_TYPE_TEAMS = new ParameterizedTypeReference<List<MMTeam>>() {
  };
  protected static final ParameterizedTypeReference<List<MMUser>> RESPONSE_TYPE_USERS = new ParameterizedTypeReference<List<MMUser>>() {
  };
  protected static final ParameterizedTypeReference<List<MMTeamMember>> RESPONSE_TYPE_TEAM_MEMBERS = new ParameterizedTypeReference<List<MMTeamMember>>() {
  };
  protected static final ParameterizedTypeReference<List<MMTeamChannel>> RESPONSE_TYPE_TEAM_CHANNELS = new ParameterizedTypeReference<List<MMTeamChannel>>() {
  };

  MattermostClient(String serverUrl, int perPage, String personalAccessToken, String proxyHost, int proxyPort) {
    super(serverUrl, perPage, proxyHost, proxyPort);
    LOG.debug("Init: serverUrl={}, perPage={}, proxyHost={}, proxyPort={}", serverUrl, perPage, proxyHost, proxyPort);
    prepareAuthenticationEntity("Authorization", "Bearer " + personalAccessToken);
  }

  @Override
  protected String getApiPath() {
    return "/api/v4";
  }

  @Override
  protected <T> List<T> makeReadListApiCall(String apiCall, ParameterizedTypeReference<List<T>> typeReference,
      Map<String, String> uriVariables) {
    int page = 0;
    String url = getApiUrl(apiCall + (apiCall.contains("?") ? "&" : "?") + "page={page}&per_page={perPage}");
    uriVariables.put("page", Integer.toString(page));
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
          page++;
          uriVariables.put("page", Integer.toString(page));
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

  public List<MMUser> getUsers() {
    LOG.debug("Retrieving users");
    String apiCall = "/users";
    Map<String, String> uriVariables = createUriVariables();
    return makeReadListApiCall(apiCall, RESPONSE_TYPE_USERS, uriVariables);
  }

  public Map<String, MMUser> getUsersById() {
    List<MMUser> users = getUsers();
    if (users != null) {
      Map<String, MMUser> usersMap = new HashMap<>();
      for (MMUser user : users) {
        usersMap.put(user.getId(), user);
      }
      return usersMap;
    }
    return null;
  }

  public List<MMTeam> getTeamsWithMembers() {
    LOG.debug("Retrieving teams with members");
    List<MMTeam> teams = getTeams();
    if (teams != null) {
      Map<String, MMUser> users = getUsersById();
      if (users != null) {
        for (MMTeam team : teams) {
          List<MMTeamMember> teamMembers = getTeamMembers(team);
          if (teamMembers != null) {
            for (MMTeamMember teamMember : teamMembers) {
              MMRole role = MMRole.fromJson(teamMember.getRoles()).contains(MMRole.TEAM_ADMIN) ? MMRole.TEAM_ADMIN
                  : MMRole.TEAM_USER;
              team.addMember(users.get(teamMember.getUserId()), role);
            }
          }
        }
      }
      return teams;
    }
    return null;
  }

  public List<MMTeam> getTeams() {
    LOG.debug("Retrieving teams");
    String apiCall = "/teams";
    Map<String, String> uriVariables = createUriVariables();
    return makeReadListApiCall(apiCall, RESPONSE_TYPE_TEAMS, uriVariables);
  }

  public List<MMTeamMember> getTeamMembers(MMTeam team) {
    if (team == null || !StringUtils.hasText(team.getId())) {
      throw new IllegalStateException("Mattermost team with valid ID required");
    }
    LOG.debug("Retrieving members for team '{}'", team.getName());
    String apiCall = "/teams/{teamId}/members";
    Map<String, String> uriVariables = createUriVariables("teamId", team.getId());
    return makeReadListApiCall(apiCall, RESPONSE_TYPE_TEAM_MEMBERS, uriVariables);
  }

  public boolean addMemberToTeam(MMTeam team, MMUser user) {
    if (team == null || !StringUtils.hasText(team.getId())) {
      throw new IllegalStateException("Mattermost team with valid ID required");
    }
    if (user == null || !StringUtils.hasText(user.getId())) {
      throw new IllegalStateException("Mattermost user with valid ID required");
    }
    LOG.info("Adding user '{}' to team '{}' as {}", user.getUsername(), team.getName(), MMRole.TEAM_USER);
    String apiCall = "/teams/{teamId}/members";
    Map<String, String> uriVariables = createUriVariables("teamId", team.getId(), "userId", user.getId());
    HttpEntity<String> entity = new HttpEntity<String>("{\"team_id\": \"" + team.getId() + "\", \"user_id\": \""
        + user.getId() + "\", \"roles\": \"" + MMRole.TEAM_USER.name().toLowerCase() + "\"}",
        authenticationEntity.getHeaders());
    return makeWriteApiCall(apiCall, HttpMethod.POST, entity, uriVariables);
  }

  public boolean updateTeamMemberRoles(MMTeam team, MMUser user, List<MMRole> roles) {
    if (team == null || !StringUtils.hasText(team.getId())) {
      throw new IllegalStateException("Mattermost team with valid ID required");
    }
    if (user == null || !StringUtils.hasText(user.getId())) {
      throw new IllegalStateException("Mattermost user with valid ID required");
    }
    if (roles == null || roles.isEmpty()) {
      throw new IllegalStateException("Mattermost team role required");
    }
    LOG.info("Updating user '{}' in team '{}' with roles {}", user.getUsername(), team.getName(), roles);
    String apiCall = "/teams/{teamId}/members/{userId}/roles";
    Map<String, String> uriVariables = createUriVariables("teamId", team.getId(), "userId", user.getId());
    String rolesText = roles.get(0).name().toLowerCase();
    for (int i = 1; i < roles.size(); i++) {
      rolesText += " " + roles.get(i).name().toLowerCase();
    }
    HttpEntity<String> entity = new HttpEntity<String>(
        "{\"roles\": \"" + MMRole.TEAM_USER.name().toLowerCase() + " " + rolesText + "\"}",
        authenticationEntity.getHeaders());
    return makeWriteApiCall(apiCall, HttpMethod.PUT, entity, uriVariables);
  }

  public boolean removeMemberFromTeam(MMTeam team, MMUser user) {
    if (team == null || !StringUtils.hasText(team.getId())) {
      throw new IllegalStateException("Mattermost team with valid ID required");
    }
    if (user == null || !StringUtils.hasText(user.getId())) {
      throw new IllegalStateException("Mattermost user with valid ID required");
    }
    LOG.info("Removing user '{}' from team '{}'", user.getUsername(), team.getName());
    String apiCall = "/teams/{teamId}/members/{userId}";
    Map<String, String> uriVariables = createUriVariables("teamId", team.getId(), "userId", user.getId());
    return makeWriteApiCall(apiCall, HttpMethod.DELETE, uriVariables);
  }

  public MMTeam createTeam(String name, String displayName) {
    LOG.info("Creating team: name={}", name);
    if (!StringUtils.hasText(name)) {
      throw new IllegalStateException("name required");
    }
    if (!StringUtils.hasText(displayName)) {
      displayName = name;
    }
    String apiCall = "/teams";
    HttpEntity<String> entity = new HttpEntity<String>(
        "{\"name\" : \"" + name + "\", \"display_name\" : \"" + displayName + "\", \"type\" : \"I\"}",
        authenticationEntity.getHeaders());
    return makeWriteApiCall(apiCall, entity, MMTeam.class);
  }

  public MMUser createUser(String username, String firstName, String lastName, String email, String authService,
      String authData) {
    LOG.info("Creating user: username={}, firstName={}, lastName={}, email={}, authService={}", username, firstName,
        lastName, email, authService);
    if (!StringUtils.hasText(username) || !StringUtils.hasText(email)) {
      throw new IllegalStateException("username, name and email required");
    }
    if (firstName == null) {
      firstName = "";
    }
    if (lastName == null) {
      lastName = "";
    }
    String apiCall = "/users";
    String entityBody = "{\"email\": \"" + email + "\", \"username\": \"" + username + "\", \"first_name\": \""
        + firstName + "\", \"last_name\": \"" + lastName + "\"";

    // Associate user with external authentication provider (GitLab) as described in
    // https://forum.mattermost.org/t/solved-how-to-transition-a-user-to-gitlab-authentication/1070
    if (StringUtils.hasText(authService) && StringUtils.hasText(authData)) {
      entityBody += ", \"auth_service\": \"" + authService + "\", \"auth_data\": \"" + authData + "\"";
    } else {
      entityBody += ", \"password\": \"" + UUID.randomUUID().toString() + "\"";
    }
    entityBody += "}";
    HttpEntity<String> entity = new HttpEntity<String>(entityBody, authenticationEntity.getHeaders());
    return makeWriteApiCall(apiCall, entity, MMUser.class);
  }

  public boolean updateUserAuthentication(MMUser user, String authService, String authData) {
    if (user == null || !StringUtils.hasText(user.getId())) {
      throw new IllegalStateException("Mattermost user with valid ID required");
    }
    LOG.info("Updating user '{}' ({}) authentication: authService={}", user.getUsername(), user.getId(), authService);
    String apiCall = "/users/{id}/auth";
    Map<String, String> uriVariables = createUriVariables("id", user.getId());
    HttpEntity<String> entity = new HttpEntity<String>("{\"auth_service\": \"" + authService + "\", \"auth_data\": \""
        + authData + "\", \"password\": \"" + /* "123456" + */"\"}", authenticationEntity.getHeaders());
    return makeWriteApiCall(apiCall, HttpMethod.PUT, entity, uriVariables);
  }

  public boolean updateUserActiveStatus(MMUser user, boolean active) {
    if (user == null || !StringUtils.hasText(user.getId())) {
      throw new IllegalStateException("Mattermost user with valid ID required");
    }
    LOG.info("Updating user '{}' ({}) active ({})", user.getUsername(), user.getId(), active);
    String apiCall = "/users/{id}/active";
    Map<String, String> uriVariables = createUriVariables("id", user.getId());
    HttpEntity<String> entity = new HttpEntity<String>("{\"active\": " + active + "}",
        authenticationEntity.getHeaders());
    return makeWriteApiCall(apiCall, HttpMethod.PUT, entity, uriVariables);
  }

  public List<MMTeamChannel> getTeamChannels(MMTeam team) {
    if (team == null || !StringUtils.hasText(team.getId())) {
      throw new IllegalStateException("Mattermost team with valid ID required");
    }
    LOG.debug("Retrieving channels for team '{}'", team.getName());
    String apiCall = "/teams/{teamId}/channels";
    Map<String, String> uriVariables = createUriVariables("teamId", team.getId());
    return makeReadListApiCall(apiCall, RESPONSE_TYPE_TEAM_CHANNELS, uriVariables);
  }
}
