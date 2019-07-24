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

  @Override
  protected String getApiPath() {
    return "/api/v4";
  }

  @Override
  protected <T> List<T> makeReadListApiCall(String apiCall, ParameterizedTypeReference<List<T>> typeReference,
      Map<String, String> uriVariables) {
    String url = getApiUrl(apiCall + (apiCall.contains("?") ? "&" : "?") + "per_page={perPage}");
    uriVariables.put("perPage", perPageAsString());
    try {
      List<T> entities;
      ResponseEntity<List<T>> response = restTemplate.exchange(url, HttpMethod.GET, authenticationEntity, typeReference,
          uriVariables);
      LinkHeader linkHeader = LinkHeader.parse(response.getHeaders());
      if (linkHeader == null || !linkHeader.hasLink(LinkHeader.Rel.NEXT)) {
        entities = response.getBody();
      } else {
        entities = new ArrayList<>(response.getBody());
        do {
          URI nextResourceUri = linkHeader.getLink(LinkHeader.Rel.NEXT).getResourceUri();
          response = restTemplate.exchange(nextResourceUri, HttpMethod.GET, authenticationEntity, typeReference);
          entities.addAll(response.getBody());
          linkHeader = LinkHeader.parse(response.getHeaders());
        } while (linkHeader != null && linkHeader.hasLink(LinkHeader.Rel.NEXT));
      }
      return entities;
    } catch (RestClientException e) {
      LOG.error("API call {} '{}' {} failed", "GET", url, uriVariables, e);
    }
    return null;
  }

  public List<GLUser> getUsers(String search) {
    LOG.debug("Retrieving users: search={}", search);
    String apiCall = "/users";
    Map<String, String> uriVariables = createUriVariables();
    if (StringUtils.hasText(search)) {
      apiCall += "?search={search}";
      uriVariables.put("search", search);
    }
    return makeReadListApiCall(apiCall, RESPONSE_TYPE_USERS, uriVariables);
  }

  public List<GLGroup> getGroupsWithMembers(String search, boolean withStatistics) {
    LOG.debug("Retrieving groups with members: search={}, withStatistics={}", search, withStatistics);
    List<GLGroup> groups = getGroups(search, withStatistics);
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

  public List<GLGroup> getGroups(String search, boolean withStatistics) {
    LOG.debug("Retrieving groups: search={}, withStatistics={}", search, withStatistics);
    String apiCall = "/groups?statistics={statistics}";
    Map<String, String> uriVariables = createUriVariables("statistics", Boolean.toString(withStatistics));
    if (StringUtils.hasText(search)) {
      apiCall += "&search={search}";
      uriVariables.put("search", search);
    }
    return makeReadListApiCall(apiCall, RESPONSE_TYPE_GROUPS, uriVariables);
  }

  public List<GLUser> getGroupMembers(GLGroup group) {
    if (group == null || !StringUtils.hasText(group.getId())) {
      throw new IllegalStateException("GitLab group with valid ID required");
    }
    LOG.debug("Retrieving members for group '{}'", group.getName());
    String apiCall = "/groups/{groupId}/members";
    Map<String, String> uriVariables = createUriVariables("groupId", group.getId());
    return makeReadListApiCall(apiCall, RESPONSE_TYPE_USERS, uriVariables);
  }

  public boolean addMemberToGroup(GLGroup group, GLUser user, GLPermission permission) {
    if (group == null || !StringUtils.hasText(group.getId())) {
      throw new IllegalStateException("GitLab group with valid ID required");
    }
    if (user == null || !StringUtils.hasText(user.getId())) {
      throw new IllegalStateException("GitLab user with valid ID required");
    }
    LOG.info("Adding user '{}' to group '{}' as {}", user.getUsername(), group.getPath(), permission);
    String apiCall = "/groups/{groupId}/members?user_id={userId}&access_level={accessLevel}";
    Map<String, String> uriVariables = createUriVariables("groupId", group.getId(), "userId", user.getId(),
        "accessLevel", permission.getAccessLevel());
    return makeWriteApiCall(apiCall, HttpMethod.POST, uriVariables);
  }

  public boolean removeMemberFromGroup(GLGroup group, GLUser user) {
    if (group == null || !StringUtils.hasText(group.getId())) {
      throw new IllegalStateException("GitLab group with valid ID required");
    }
    if (user == null || !StringUtils.hasText(user.getId())) {
      throw new IllegalStateException("GitLab user with valid ID required");
    }
    LOG.info("Removing user '{}' from group '{}'", user.getUsername(), group.getPath());
    String apiCall = "/groups/{groupId}/members/{userId}";
    Map<String, String> uriVariables = createUriVariables("groupId", group.getId(), "userId", user.getId());
    return makeWriteApiCall(apiCall, HttpMethod.DELETE, uriVariables);
  }

  public GLGroup createGroup(String path, String name, String description) {
    LOG.info("Creating group: path={}, name={}", path, name);
    if (!StringUtils.hasText(path)) {
      throw new IllegalStateException("path required");
    }
    if (!StringUtils.hasText(name)) {
      name = path;
    }
    String apiCall = "/groups?path={path}&name={name}&request_access_enabled=false&share_with_group_lock=false";
    Map<String, String> uriVariables = createUriVariables("name", name, "path", path);
    if (StringUtils.hasText(description)) {
      apiCall += "&description={description}";
      uriVariables.put("description", description);
    }
    return makeWriteApiCall(apiCall, GLGroup.class, uriVariables);
  }

  public GLUser createUser(String username, String name, String email, String provider, String externUid) {
    LOG.info("Creating user: username={}, name={}, email={}", username, name, email);
    if (!StringUtils.hasText(username) || !StringUtils.hasText(name) || !StringUtils.hasText(email)) {
      throw new IllegalStateException("username, name and email required");
    }
    String apiCall = "/users?username={username}&name={name}&email={email}&password={password}&skip_confirmation=true";
    Map<String, String> uriVariables = createUriVariables("username", username, "name", name, "email", email,
        "password", UUID.randomUUID().toString());

    // Associate user with external authentication provider (e.g. LDAP)
    // as described in
    // https://gitlab.com/gitlab-org/gitlab-ee/issues/699#note_19890755
    if (StringUtils.hasText(provider) && StringUtils.hasText(externUid)) {
      apiCall += "&provider={provider}&extern_uid={externUid}";
      uriVariables.put("provider", provider);
      uriVariables.put("externUid", externUid.toLowerCase());
    }
    return makeWriteApiCall(apiCall, GLUser.class, uriVariables);
  }

  public boolean blockUser(GLUser user) {
    if (user == null || !StringUtils.hasText(user.getId())) {
      throw new IllegalStateException("GitLab user with valid ID required");
    }
    LOG.info("Blocking user '{}' ({})", user.getUsername(), user.getId());
    String apiCall = "/users/{id}/block";
    Map<String, String> uriVariables = createUriVariables("id", user.getId());
    if (makeWriteApiCall(apiCall, HttpMethod.POST, uriVariables)) {
      user.setState(GLState.BLOCKED);
      return true;
    }
    return false;
  }

  public boolean unblockUser(GLUser user) {
    if (user == null || !StringUtils.hasText(user.getId())) {
      throw new IllegalStateException("GitLab user with valid ID required");
    }
    LOG.info("Unblocking user '{}' ({})", user.getUsername(), user.getId());
    String apiCall = "/users/{id}/unblock";
    Map<String, String> uriVariables = createUriVariables("id", user.getId());
    if (makeWriteApiCall(apiCall, HttpMethod.POST, uriVariables)) {
      user.setState(GLState.ACTIVE);
      return true;
    }
    return false;
  }

  public boolean addIdentityToUser(GLUser user, String provider, String externUid) {
    LOG.info("Adding identity to user '{}' ({}): provider={}, externUid={}", user.getUsername(), user.getId(), provider,
        externUid);
    if (!StringUtils.hasText(provider) || !StringUtils.hasText(externUid)) {
      throw new IllegalStateException("provider and externUid required");
    }
    String apiCall = "/users/{id}?provider={provider}&extern_uid={externUid}";
    Map<String, String> uriVariables = createUriVariables("id", user.getId(), "provider", provider, "externUid",
        externUid.toLowerCase());
    if (makeWriteApiCall(apiCall, HttpMethod.PUT, uriVariables)) {
      user.addIdentity(provider, externUid);
      return true;
    }
    return false;
  }

  public boolean deleteUser(GLUser user, boolean hard) {
    if (user == null || !StringUtils.hasText(user.getId())) {
      throw new IllegalStateException("GitLab user with valid ID required");
    }
    LOG.info("Deleting user '{}' ({})", user.getUsername(), user.getId());
    String apiCall = "/users/{id}?hard_delete={hard}";
    Map<String, String> uriVariables = createUriVariables("id", user.getId(), "hard", Boolean.toString(hard));
    return makeWriteApiCall(apiCall, HttpMethod.DELETE, uriVariables);
  }

  public List<GLProject> getProjectsFromGroup(GLGroup group, String search) {
    if (group == null || !StringUtils.hasText(group.getId())) {
      throw new IllegalStateException("GitLab group with valid ID required");
    }
    LOG.debug("Retrieving projects from group '{}': search={}", group.getName(), search);
    String apiCall = "/groups/{groupId}/projects";
    Map<String, String> uriVariables = createUriVariables("groupId", group.getId());
    if (StringUtils.hasText(search)) {
      apiCall += "?search={search}";
      uriVariables.put("search", search);
    }
    return makeReadListApiCall(apiCall, RESPONSE_TYPE_PROJECTS, uriVariables);
  }

  public List<GLUser> getProjectUsers(GLProject project) {
    if (project == null || !StringUtils.hasText(project.getId())) {
      throw new IllegalStateException("GitLab project with valid ID required");
    }
    LOG.debug("Retrieving users for project '{}'", project.getPath());
    String apiCall = "/projects/{projectId}/users";
    Map<String, String> uriVariables = createUriVariables("projectId", project.getId());
    return makeReadListApiCall(apiCall, RESPONSE_TYPE_USERS, uriVariables);
  }

  public boolean removeMemberFromProject(GLProject project, GLUser user) {
    if (project == null || !StringUtils.hasText(project.getId())) {
      throw new IllegalStateException("GitLab project with valid ID required");
    }
    if (user == null || !StringUtils.hasText(user.getId())) {
      throw new IllegalStateException("GitLab user with valid ID required");
    }
    LOG.info("Removing user '{}' from project '{}'", user.getUsername(), project.getPath());
    String apiCall = "/projects/{projectId}/members/{userId}";
    Map<String, String> uriVariables = createUriVariables("projectId", project.getId(), "userId", user.getId());
    return makeWriteApiCall(apiCall, HttpMethod.DELETE, uriVariables);
  }
}
