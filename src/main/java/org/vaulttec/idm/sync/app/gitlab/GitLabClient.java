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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.vaulttec.idm.sync.app.AbstractRestClient;
import org.vaulttec.idm.sync.app.LinkHeader;

public class GitLabClient extends AbstractRestClient {

  private static final Logger LOG = LoggerFactory.getLogger(GitLabClient.class);

  protected static final ParameterizedTypeReference<List<GLGroup>> RESPONSE_TYPE_GROUPS = new ParameterizedTypeReference<List<GLGroup>>() {
  };
  protected static final ParameterizedTypeReference<List<GLUser>> RESPONSE_TYPE_USERS = new ParameterizedTypeReference<List<GLUser>>() {
  };
  protected static final ParameterizedTypeReference<List<GLProject>> RESPONSE_TYPE_PROJECTS = new ParameterizedTypeReference<List<GLProject>>() {
  };

  GitLabClient(String serverUrl, int perPage, String personalAccessToken, String proxyHost, int proxyPort) {
    super(serverUrl, perPage, proxyHost, proxyPort);
    LOG.debug("Init: serverUrl={}, perPage={}, proxyHost={}, proxyPort={}", serverUrl, perPage, proxyHost, proxyPort);
    prepareAuthenticationEntity("PRIVATE-TOKEN", personalAccessToken);
  }

  public List<GLUser> getUsers(String search) {
    LOG.debug("Retrieving users: search={}", search);
    String usersUrl = serverUrl + "/api/v4/users?page=1&per_page={perPage}";
    Map<String, String> uriVariables = createUriVariables("perPage", perPageAsString());
    if (StringUtils.hasText(search)) {
      usersUrl += "?search={search}";
      uriVariables.put("search", search);
    }
    try {
      List<GLUser> users;
      ResponseEntity<List<GLUser>> response = restTemplate.exchange(usersUrl, HttpMethod.GET, authenticationEntity,
          RESPONSE_TYPE_USERS, uriVariables);
      LinkHeader linkHeader = LinkHeader.parse(response.getHeaders());
      if (linkHeader == null || !linkHeader.hasLink(LinkHeader.Rel.NEXT)) {
        users = response.getBody();
      } else {
        users = new ArrayList<>(response.getBody());
        do {
          URI nextResourceUri = linkHeader.getLink(LinkHeader.Rel.NEXT).getResourceUri();
          response = restTemplate.exchange(nextResourceUri, HttpMethod.GET, authenticationEntity, RESPONSE_TYPE_USERS);
          users.addAll(response.getBody());
          linkHeader = LinkHeader.parse(response.getHeaders());
        } while (linkHeader != null && linkHeader.hasLink(LinkHeader.Rel.NEXT));
      }
      return users;
    } catch (RestClientException e) {
      LOG.error("Retrieving users failed", e);
    }
    return null;
  }

  public List<GLGroup> getGroupsWithMembers(String search) {
    LOG.debug("Retrieving groups with members: search={}", search);
    List<GLGroup> groups = getGroups(search);
    if (groups != null) {
      for (GLGroup group : groups) {
        List<GLUser> members = getGroupMembers(group);
        if (members != null) {
          for (GLUser member : members) {
            member.addGroup(group);
            group.addMember(member, member.getPermission());
          }
        }
      }
      return groups;
    }
    return null;
  }

  public List<GLGroup> getGroups(String search) {
    LOG.debug("Retrieving groups: search={}", search);
    String groupsUrl = serverUrl + "/api/v4/groups?page=1&per_page={perPage}";
    Map<String, String> uriVariables = createUriVariables("perPage", perPageAsString());
    if (StringUtils.hasText(search)) {
      groupsUrl += "?search={search}";
      uriVariables.put("search", search);
    }
    try {
      List<GLGroup> groups;
      ResponseEntity<List<GLGroup>> response = restTemplate.exchange(groupsUrl, HttpMethod.GET, authenticationEntity,
          RESPONSE_TYPE_GROUPS, uriVariables);
      LinkHeader linkHeader = LinkHeader.parse(response.getHeaders());
      if (linkHeader == null || !linkHeader.hasLink(LinkHeader.Rel.NEXT)) {
        groups = response.getBody();
      } else {
        groups = new ArrayList<>(response.getBody());
        do {
          URI nextResourceUri = linkHeader.getLink(LinkHeader.Rel.NEXT).getResourceUri();
          response = restTemplate.exchange(nextResourceUri, HttpMethod.GET, authenticationEntity, RESPONSE_TYPE_GROUPS);
          groups.addAll(response.getBody());
          linkHeader = LinkHeader.parse(response.getHeaders());
        } while (linkHeader != null && linkHeader.hasLink(LinkHeader.Rel.NEXT));
      }
      return groups;
    } catch (RestClientException e) {
      LOG.error("Retrieving groups failed", e);
    }
    return null;
  }

  public List<GLUser> getGroupMembers(GLGroup group) {
    if (group == null || !StringUtils.hasText(group.getId())) {
      throw new IllegalStateException("GitLab group with valid ID required");
    }
    LOG.debug("Retrieving members for group '{}'", group.getName());
    String groupMembersUrl = serverUrl + "/api/v4/groups/{groupId}/members?page=1&per_page={perPage}";
    Map<String, String> uriVariables = createUriVariables("groupId", group.getId(), "perPage", perPageAsString());
    try {
      List<GLUser> users;
      ResponseEntity<List<GLUser>> response = restTemplate.exchange(groupMembersUrl, HttpMethod.GET,
          authenticationEntity, RESPONSE_TYPE_USERS, uriVariables);
      LinkHeader linkHeader = LinkHeader.parse(response.getHeaders());
      if (linkHeader == null || !linkHeader.hasLink(LinkHeader.Rel.NEXT)) {
        users = response.getBody();
      } else {
        users = new ArrayList<>(response.getBody());
        do {
          URI nextResourceUri = linkHeader.getLink(LinkHeader.Rel.NEXT).getResourceUri();
          response = restTemplate.exchange(nextResourceUri, HttpMethod.GET, authenticationEntity, RESPONSE_TYPE_USERS);
          users.addAll(response.getBody());
          linkHeader = LinkHeader.parse(response.getHeaders());
        } while (linkHeader != null && linkHeader.hasLink(LinkHeader.Rel.NEXT));
      }
      return users;
    } catch (RestClientException e) {
      LOG.error("Retrieving group members failed", e);
    }
    return null;
  }

  public boolean addMemberToGroup(GLGroup group, GLUser user, GLPermission permission) {
    if (group == null || !StringUtils.hasText(group.getId())) {
      throw new IllegalStateException("GitLab group with valid ID required");
    }
    if (user == null || !StringUtils.hasText(user.getId())) {
      throw new IllegalStateException("GitLab user with valid ID required");
    }
    LOG.info("Adding user '{}' to group '{}' as {}", user.getUsername(), group.getPath(), permission);
    String groupMembersUrl = serverUrl + "/api/v4/groups/{groupId}/members?user_id={userId}&access_level={accessLevel}";
    Map<String, String> uriVariables = createUriVariables("groupId", group.getId(), "userId", user.getId(),
        "accessLevel", permission.getAccessLevel());
    try {
      restTemplate.exchange(groupMembersUrl, HttpMethod.POST, authenticationEntity, Void.class, uriVariables);
      return true;
    } catch (RestClientException e) {
      LOG.error("Adding user to group failed", e);
    }
    return false;
  }

  public boolean removeMemberFromGroup(GLGroup group, GLUser user) {
    if (group == null || !StringUtils.hasText(group.getId())) {
      throw new IllegalStateException("GitLab group with valid ID required");
    }
    if (user == null || !StringUtils.hasText(user.getId())) {
      throw new IllegalStateException("GitLab user with valid ID required");
    }
    LOG.info("Removing user '{}' from group '{}'", user.getUsername(), group.getPath());
    String groupMembersUrl = serverUrl + "/api/v4/groups/{groupId}/members/{userId}";
    Map<String, String> uriVariables = createUriVariables("groupId", group.getId(), "userId", user.getId());
    try {
      restTemplate.exchange(groupMembersUrl, HttpMethod.DELETE, authenticationEntity, Void.class, uriVariables);
      return true;
    } catch (RestClientException e) {
      LOG.error("Removing user from group failed", e);
    }
    return false;
  }

  public GLGroup createGroup(String path, String name, String description) {
    LOG.info("Creating group: path={}, name={}", path, name);
    if (!StringUtils.hasText(path)) {
      throw new IllegalStateException("path required");
    }
    if (!StringUtils.hasText(name)) {
      name = path;
    }
    String groupsUrl = serverUrl + "/api/v4/groups?path={path}&name={name}"
        + "&request_access_enabled=false&share_with_group_lock=false";
    Map<String, String> uriVariables = createUriVariables("name", name, "path", path);
    if (StringUtils.hasText(description)) {
      groupsUrl += "&description={description}";
      uriVariables.put("description", description);
    }
    try {
      GLGroup group = restTemplate.postForObject(groupsUrl, authenticationEntity, GLGroup.class, uriVariables);
      return group;
    } catch (RestClientException e) {
      LOG.error("Creating group failed", e);
    }
    return null;
  }

  public GLUser createUser(String username, String name, String email, String provider, String externUid) {
    LOG.info("Creating user: username={}, name={}, email={}", username, name, email);
    if (!StringUtils.hasText(username) || !StringUtils.hasText(name) || !StringUtils.hasText(email)) {
      throw new IllegalStateException("username, name and email required");
    }
    String usersUrl = serverUrl + "/api/v4/users?username={username}&name={name}&email={email}"
        + "&password={password}&skip_confirmation=true";
    Map<String, String> uriVariables = createUriVariables("username", username, "name", name, "email", email,
        "password", UUID.randomUUID().toString());

    // Associate user with external authentication provider (e.g. LDAP)
    // as described in https://gitlab.com/gitlab-org/gitlab-ee/issues/699#note_19890755
    if (StringUtils.hasText(provider) && StringUtils.hasText(externUid)) {
      usersUrl += "&provider={provider}&extern_uid={externUid}";
      uriVariables.put("provider", provider);
      uriVariables.put("externUid", externUid.toLowerCase());
    }
    try {
      GLUser user = restTemplate.postForObject(usersUrl, authenticationEntity, GLUser.class, uriVariables);
      return user;
    } catch (RestClientException e) {
      LOG.error("Creating user failed", e);
    }
    return null;
  }

  public boolean blockUser(GLUser user) {
    if (user == null || !StringUtils.hasText(user.getId())) {
      throw new IllegalStateException("GitLab user with valid ID required");
    }
    LOG.info("Blocking user '{}' ({})", user.getUsername(), user.getId());
    String userUrl = serverUrl + "/api/v4/users/{id}/block";
    Map<String, String> uriVariables = createUriVariables("id", user.getId());
    try {
      restTemplate.exchange(userUrl, HttpMethod.POST, authenticationEntity, Void.class, uriVariables);
      return true;
    } catch (RestClientException e) {
      LOG.error("Blocking user failed", e);
    }
    return false;
  }

  public boolean unblockUser(GLUser user) {
    if (user == null || !StringUtils.hasText(user.getId())) {
      throw new IllegalStateException("GitLab user with valid ID required");
    }
    LOG.info("Unblocking user '{}' ({})", user.getUsername(), user.getId());
    String userUrl = serverUrl + "/api/v4/users/{id}/unblock";
    Map<String, String> uriVariables = createUriVariables("id", user.getId());
    try {
      restTemplate.exchange(userUrl, HttpMethod.POST, authenticationEntity, Void.class, uriVariables);
      user.setState(GLState.ACTIVE);
      return true;
    } catch (RestClientException e) {
      LOG.error("Unblocking user failed", e);
    }
    return false;
  }

  public List<GLProject> getProjectsFromGroup(GLGroup group, String search) {
    if (group == null || !StringUtils.hasText(group.getId())) {
      throw new IllegalStateException("GitLab group with valid ID required");
    }
    LOG.debug("Retrieving projects from group: search={}", search);
    String projectsUrl = serverUrl + "/api/v4/groups/{groupId}/projects?page=1&per_page={perPage}";
    Map<String, String> uriVariables = createUriVariables("groupId", group.getId(), "perPage", perPageAsString());
    if (StringUtils.hasText(search)) {
      projectsUrl += "?search={search}";
      uriVariables.put("search", search);
    }
    try {
      List<GLProject> projects;
      ResponseEntity<List<GLProject>> response = restTemplate.exchange(projectsUrl, HttpMethod.GET,
          authenticationEntity, RESPONSE_TYPE_PROJECTS, uriVariables);
      LinkHeader linkHeader = LinkHeader.parse(response.getHeaders());
      if (linkHeader == null || !linkHeader.hasLink(LinkHeader.Rel.NEXT)) {
        projects = response.getBody();
      } else {
        projects = new ArrayList<>(response.getBody());
        do {
          URI nextResourceUri = linkHeader.getLink(LinkHeader.Rel.NEXT).getResourceUri();
          response = restTemplate.exchange(nextResourceUri, HttpMethod.GET, authenticationEntity,
              RESPONSE_TYPE_PROJECTS);
          projects.addAll(response.getBody());
          linkHeader = LinkHeader.parse(response.getHeaders());
        } while (linkHeader != null && linkHeader.hasLink(LinkHeader.Rel.NEXT));
      }
      return projects;
    } catch (RestClientException e) {
      LOG.error("Retrieving projects failed", e);
    }
    return null;
  }

  public List<GLUser> getProjectUsers(GLProject project) {
    if (project == null || !StringUtils.hasText(project.getId())) {
      throw new IllegalStateException("GitLab project with valid ID required");
    }
    LOG.debug("Retrieving users for project '{}'", project.getPath());
    String groupMembersUrl = serverUrl + "/api/v4/projects/{projectId}/users?page=1&per_page={perPage}";
    Map<String, String> uriVariables = createUriVariables("projectId", project.getId(), "perPage", perPageAsString());
    try {
      List<GLUser> users;
      ResponseEntity<List<GLUser>> response = restTemplate.exchange(groupMembersUrl, HttpMethod.GET,
          authenticationEntity, RESPONSE_TYPE_USERS, uriVariables);
      LinkHeader linkHeader = LinkHeader.parse(response.getHeaders());
      if (linkHeader == null || !linkHeader.hasLink(LinkHeader.Rel.NEXT)) {
        users = response.getBody();
      } else {
        users = new ArrayList<>(response.getBody());
        do {
          URI nextResourceUri = linkHeader.getLink(LinkHeader.Rel.NEXT).getResourceUri();
          response = restTemplate.exchange(nextResourceUri, HttpMethod.GET, authenticationEntity, RESPONSE_TYPE_USERS);
          users.addAll(response.getBody());
          linkHeader = LinkHeader.parse(response.getHeaders());
        } while (linkHeader != null && linkHeader.hasLink(LinkHeader.Rel.NEXT));
      }
      return users;
    } catch (RestClientException e) {
      LOG.error("Retrieving project users failed", e);
    }
    return null;
  }

  public boolean removeMemberFromProject(GLProject project, GLUser user) {
    if (project == null || !StringUtils.hasText(project.getId())) {
      throw new IllegalStateException("GitLab project with valid ID required");
    }
    if (user == null || !StringUtils.hasText(user.getId())) {
      throw new IllegalStateException("GitLab user with valid ID required");
    }
    LOG.info("Removing user '{}' from project '{}'", user.getUsername(), project.getPath());
    String projectUsersUrl = serverUrl + "/api/v4/projects/{projectId}/members/{userId}";
    Map<String, String> uriVariables = createUriVariables("projectId", project.getId(), "userId", user.getId());
    try {
      restTemplate.exchange(projectUsersUrl, HttpMethod.DELETE, authenticationEntity, Void.class, uriVariables);
      return true;
    } catch (RestClientException e) {
      LOG.error("Removing user from project failed", e);
    }
    return false;
  }
}
