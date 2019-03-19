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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.vaulttec.idm.sync.app.AbstractRestClient;

public class MattermostClient extends AbstractRestClient {

  private static final Logger LOG = LoggerFactory.getLogger(MattermostClient.class);

  protected static final ParameterizedTypeReference<List<MMTeam>> RESPONSE_TYPE_TEAMS = new ParameterizedTypeReference<List<MMTeam>>() {
  };
  protected static final ParameterizedTypeReference<List<MMUser>> RESPONSE_TYPE_USERS = new ParameterizedTypeReference<List<MMUser>>() {
  };
  protected static final ParameterizedTypeReference<List<MMTeamMember>> RESPONSE_TYPE_TEAM_MEMBERS = new ParameterizedTypeReference<List<MMTeamMember>>() {
  };

  MattermostClient(String serverUrl, int perPage, String personalAccessToken, String proxyHost, int proxyPort) {
    super(serverUrl, perPage, proxyHost, proxyPort);
    LOG.debug("Init: serverUrl={}, perPage={}, proxyHost={}, proxyPort={}", serverUrl, perPage, proxyHost, proxyPort);
    prepareAuthenticationEntity("Authorization", "Bearer " + personalAccessToken);
  }

  public List<MMUser> getUsers() {
    LOG.debug("Retrieving users");
    int page = 0;
    String usersUrl = serverUrl + "/api/v4/users?page={page}&per_page={perPage}";
    Map<String, String> uriVariables = createUriVariables("page", Integer.toString(page), "perPage", perPageAsString());
    try {
      ResponseEntity<List<MMUser>> response = restTemplate.exchange(usersUrl, HttpMethod.GET, authenticationEntity,
          RESPONSE_TYPE_USERS, uriVariables);
      if (response.getBody().size() < perPage) {
        return response.getBody();
      } else {
        List<MMUser> users = new ArrayList<>(response.getBody());
        do {
          page++;
          uriVariables.put("page", Integer.toString(page));
          response = restTemplate.exchange(usersUrl, HttpMethod.GET, authenticationEntity, RESPONSE_TYPE_USERS,
              uriVariables);
          users.addAll(response.getBody());
        } while (response.getBody().size() == perPage);
        return users;
      }
    } catch (RestClientException e) {
      LOG.error("Retrieving users failed", e);
    }
    return null;
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
    int page = 0;
    String groupsUrl = serverUrl + "/api/v4/teams?page={page}&per_page={perPage}";
    Map<String, String> uriVariables = createUriVariables("page", Integer.toString(page), "perPage", perPageAsString());
    try {
      ResponseEntity<List<MMTeam>> response = restTemplate.exchange(groupsUrl, HttpMethod.GET, authenticationEntity,
          RESPONSE_TYPE_TEAMS, uriVariables);
      if (response.getBody().size() < perPage) {
        return response.getBody();
      } else {
        List<MMTeam> teams = new ArrayList<>(response.getBody());
        do {
          page++;
          uriVariables.put("page", Integer.toString(page));
          response = restTemplate.exchange(groupsUrl, HttpMethod.GET, authenticationEntity, RESPONSE_TYPE_TEAMS,
              uriVariables);
          teams.addAll(response.getBody());
        } while (response.getBody().size() == perPage);
        return teams;
      }
    } catch (RestClientException e) {
      LOG.error("Retrieving teams failed", e);
    }
    return null;
  }

  public List<MMTeamMember> getTeamMembers(MMTeam team) {
    if (team == null || !StringUtils.hasText(team.getId())) {
      throw new IllegalStateException("Mattermost team with valid ID required");
    }
    LOG.debug("Retrieving members for team '{}'", team.getName());
    int page = 0;
    String teamMembersUrl = serverUrl + "/api/v4/teams/{teamId}/members?page={page}&per_page={perPage}";
    Map<String, String> uriVariables = createUriVariables("teamId", team.getId(), "page", Integer.toString(page),
        "perPage", perPageAsString());
    try {
      ResponseEntity<List<MMTeamMember>> response = restTemplate.exchange(teamMembersUrl, HttpMethod.GET,
          authenticationEntity, RESPONSE_TYPE_TEAM_MEMBERS, uriVariables);
      if (response.getBody().size() < perPage) {
        return response.getBody();
      } else {
        List<MMTeamMember> teamMembers = new ArrayList<>(response.getBody());
        do {
          page++;
          uriVariables.put("page", Integer.toString(page));
          response = restTemplate.exchange(teamMembersUrl, HttpMethod.GET, authenticationEntity,
              RESPONSE_TYPE_TEAM_MEMBERS, uriVariables);
          teamMembers.addAll(response.getBody());
        } while (response.getBody().size() == perPage);
        return teamMembers;
      }
    } catch (RestClientException e) {
      LOG.error("Retrieving team members failed", e);
    }
    return null;
  }

  public boolean addMemberToTeam(MMTeam team, MMUser user) {
    if (team == null || !StringUtils.hasText(team.getId())) {
      throw new IllegalStateException("Mattermost team with valid ID required");
    }
    if (user == null || !StringUtils.hasText(user.getId())) {
      throw new IllegalStateException("Mattermost user with valid ID required");
    }
    LOG.info("Adding user '{}' to team '{}' as {}", user.getUsername(), team.getName(), MMRole.TEAM_USER);
    String teamMembersUrl = serverUrl + "/api/v4/teams/{teamId}/members";
    Map<String, String> uriVariables = createUriVariables("teamId", team.getId(), "userId", user.getId());
    HttpEntity<String> entity = new HttpEntity<String>("{\"team_id\": \"" + team.getId() + "\", \"user_id\": \""
        + user.getId() + "\", \"roles\": \"" + MMRole.TEAM_USER.name().toLowerCase() + "\"}",
        authenticationEntity.getHeaders());
    try {
      restTemplate.exchange(teamMembersUrl, HttpMethod.POST, entity, Void.class, uriVariables);
      return true;
    } catch (RestClientException e) {
      LOG.error("Adding user to group failed", e);
    }
    return false;
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
    LOG.info("Updating user '{}' in team '{}' wih roles {}", user.getUsername(), team.getName(), roles);
    String teamMemberRolesUrl = serverUrl + "/api/v4/teams/{teamId}/members/{userId}/roles";
    Map<String, String> uriVariables = createUriVariables("teamId", team.getId(), "userId", user.getId());
    String rolesText = roles.get(0).name().toLowerCase();
    for (int i = 1; i < roles.size(); i++) {
      rolesText += " " + roles.get(i).name().toLowerCase();
    }
    HttpEntity<String> entity = new HttpEntity<String>(
        "{\"roles\": \"" + MMRole.TEAM_USER.name().toLowerCase() + " " + rolesText + "\"}",
        authenticationEntity.getHeaders());
    try {
      restTemplate.exchange(teamMemberRolesUrl, HttpMethod.PUT, entity, Void.class, uriVariables);
      return true;
    } catch (RestClientException e) {
      LOG.error("Updating user roles in team failed", e);
    }
    return false;
  }

  public boolean removeMemberFromTeam(MMTeam team, MMUser user) {
    if (team == null || !StringUtils.hasText(team.getId())) {
      throw new IllegalStateException("Mattermost team with valid ID required");
    }
    if (user == null || !StringUtils.hasText(user.getId())) {
      throw new IllegalStateException("Mattermost user with valid ID required");
    }
    LOG.info("Removing user '{}' from team '{}'", user.getUsername(), team.getName());
    String teamMembersUrl = serverUrl + "/api/v4/teams/{teamId}/members/{userId}";
    Map<String, String> uriVariables = createUriVariables("teamId", team.getId(), "userId", user.getId());
    try {
      restTemplate.exchange(teamMembersUrl, HttpMethod.DELETE, authenticationEntity, Void.class, uriVariables);
      return true;
    } catch (RestClientException e) {
      LOG.error("Removing user from team failed", e);
    }
    return false;
  }

  public MMTeam createTeam(String name, String displayName) {
    LOG.info("Creating team: name={}", name);
    if (!StringUtils.hasText(name)) {
      throw new IllegalStateException("name required");
    }
    if (!StringUtils.hasText(displayName)) {
      displayName = name;
    }
    String groupsUrl = serverUrl + "/api/v4/teams";
    HttpEntity<String> entity = new HttpEntity<String>(
        "{\"name\" : \"" + name + "\", \"display_name\" : \"" + displayName + "\", \"type\" : \"I\"}",
        authenticationEntity.getHeaders());
    try {
      MMTeam team = restTemplate.postForObject(groupsUrl, entity, MMTeam.class);
      return team;
    } catch (RestClientException e) {
      LOG.error("Creating team failed", e);
    }
    return null;
  }

  public MMUser createUser(String username, String firstName, String lastName, String email, String authService,
      String authData) {
    LOG.info("Creating user: username={}, firstName={}, lastName={}, email={}", username, firstName, lastName, email);
    if (!StringUtils.hasText(username) || !StringUtils.hasText(email)) {
      throw new IllegalStateException("username, name and email required");
    }
    if (firstName == null) {
      firstName = "";
    }
    if (lastName == null) {
      lastName = "";
    }
    String usersUrl = serverUrl + "/api/v4/users";
    String entityBody = "{\"email\": \"" + email + "\", \"username\": \"" + username + "\", \"first_name\": \""
        + firstName + "\", \"last_name\": \"" + lastName + "\"";

    // Associate user with external authentication provider (GitLab)
    // as described in
    // https://forum.mattermost.org/t/solved-how-to-transition-a-user-to-gitlab-authentication/1070
    if (StringUtils.hasText(authService) && StringUtils.hasText(authData)) {
      entityBody += ", \"auth_service\": \"" + authService + "\", \"auth_data\": \"" + authData + "\"";
    }
    entityBody += "}";
    HttpEntity<String> entity = new HttpEntity<String>(entityBody, authenticationEntity.getHeaders());
    try {
      MMUser user = restTemplate.postForObject(usersUrl, entity, MMUser.class);
      return user;
    } catch (RestClientException e) {
      LOG.error("Creating user failed", e);
    }
    return null;
  }

  public boolean updateUserActiveStatus(MMUser user, boolean active) {
    if (user == null || !StringUtils.hasText(user.getId())) {
      throw new IllegalStateException("Mattermost user with valid ID required");
    }
    LOG.info("Updating user '{}' ({}) active ({})", user.getUsername(), user.getId(), active);
    String userUrl = serverUrl + "/api/v4/users/{id}/active";
    Map<String, String> uriVariables = createUriVariables("id", user.getId());
    HttpEntity<String> entity = new HttpEntity<String>("{\"active\": " + active + "}",
        authenticationEntity.getHeaders());
    try {
      restTemplate.exchange(userUrl, HttpMethod.PUT, entity, Void.class, uriVariables);
      return true;
    } catch (RestClientException e) {
      LOG.error("Updating user's active state failed", e);
    }
    return false;
  }
}
