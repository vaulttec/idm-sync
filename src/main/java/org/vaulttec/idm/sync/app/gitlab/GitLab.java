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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.vaulttec.idm.sync.app.AbstractApplication;
import org.vaulttec.idm.sync.idp.IdpGroup;
import org.vaulttec.idm.sync.idp.IdpUser;
import org.vaulttec.util.StringUtils;

public class GitLab extends AbstractApplication {

  private static final Logger LOG = LoggerFactory.getLogger(GitLab.class);

  public static final String APPLICATION_ID = "gitlab";
  public static final String USER_ID_ATTRIBUTE = "GITLAB_USER_ID";

  private final GitLabClient client;
  private final Set<String> excludedUsers;
  private boolean removeProjectMembers;
  private final String providerName;
  private final String providerUidAttribute;

  GitLab(GitLabClient client, AuditEventRepository eventRepository, String groupSearch, String groupRegExp,
      String excludedUsers, boolean removeProjectMembers, String providerName, String providerUidAttribute) {
    super(eventRepository, groupSearch, groupRegExp);
    LOG.debug("Init: groupSearch={}, groupRegExp={}", groupSearch, groupRegExp);
    this.client = client;
    this.excludedUsers = StringUtils.commaDelimitedListToTrimmedSet(excludedUsers);
    this.removeProjectMembers = removeProjectMembers;
    this.providerName = providerName;
    this.providerUidAttribute = providerUidAttribute;
  }

  @Override
  public String getId() {
    return APPLICATION_ID;
  }

  @Override
  public String getName() {
    return "GitLab";
  }

  @Override
  public boolean sync(List<IdpGroup> idpGroups) {
    Map<String, GLGroup> targetGroups = new HashMap<>();
    Map<String, GLUser> targetUsers = new HashMap<>();
    retrieveTargetGroupsAndUsers(idpGroups, targetGroups, targetUsers);
    Map<String, GLUser> syncedUsers = syncUsers(targetUsers);
    if (syncedUsers != null) {
      if (syncGroups(targetGroups, syncedUsers)) {
        return true;
      }
    }
    return false;
  }

  protected Map<String, GLUser> syncUsers(Map<String, GLUser> targetUsers) {
    List<GLUser> sourceUsers = client.getUsers(null);
    if (sourceUsers != null) {
      Map<String, GLUser> syncedUsers = new HashMap<>();

      // Unblock existing users associated with GitLab group now
      for (GLUser sourceUser : sourceUsers) {
        GLUser targetUser = targetUsers.get(sourceUser.getUsername());
        if (targetUser != null) {
          if (sourceUser.getState() == GLState.BLOCKED) {
            if (client.unblockUser(sourceUser)) {
              publishSyncEvent(GitLabEvents.userUnblocked(sourceUser));
            }
            sourceUser.setState(GLState.ACTIVE);
          }
          syncedUsers.put(sourceUser.getUsername(), sourceUser);
          updateUserIdAttribute(targetUser.getIdpUser(), sourceUser);
        }
      }

      // Mark existing users (by removing from list of current users) and create
      // non-existing ones
      for (GLUser targetUser : targetUsers.values()) {
        if (sourceUsers.contains(targetUser)) {
          sourceUsers.remove(targetUser);
        } else {
          if (StringUtils.hasText(targetUser.getProvider()) && !StringUtils.hasText(targetUser.getExternUid())) {
            LOG.warn("New user '{}' not created - missing required external user ID for provider '{}'",
                targetUser.getUsername(), targetUser.getProvider());
          } else if (!StringUtils.hasText(targetUser.getEmail())) {
            LOG.warn("New user '{}' not created - missing required email address", targetUser.getUsername());
          } else {
            GLUser newUser = client.createUser(targetUser.getUsername(), targetUser.getName(), targetUser.getEmail(),
                targetUser.getProvider(), targetUser.getExternUid());
            if (newUser != null) {
              publishSyncEvent(GitLabEvents.userCreated(newUser));
              syncedUsers.put(newUser.getUsername(), newUser);
              updateUserIdAttribute(targetUser.getIdpUser(), newUser);
            }
          }
        }
      }

      // Block existing users which are not associated with GitLab groups anymore
      for (GLUser sourceUser : sourceUsers) {
        if (!excludedUsers.contains(sourceUser.getUsername())) {
          if (sourceUser.getState() != GLState.BLOCKED) {
            if (client.blockUser(sourceUser)) {
              publishSyncEvent(GitLabEvents.userBlocked(sourceUser));
            }
            sourceUser.setState(GLState.BLOCKED);
          }
          syncedUsers.put(sourceUser.getUsername(), sourceUser);
        }
      }
      return syncedUsers;
    }
    return null;
  }

  /**
   * Make sure that the value of the IdP's user attribute "GITLAB_USER_ID" with
   * given GitLab user's ID.
   */
  protected void updateUserIdAttribute(IdpUser idpUser, GLUser glUser) {
    String userId = idpUser.getAttribute(USER_ID_ATTRIBUTE);
    if (userId == null || !userId.equals(glUser.getId())) {
      idpUser.getAttributes().put(USER_ID_ATTRIBUTE, Arrays.asList(glUser.getId()));
      idpUser.setAttributesModified(true);
    }
  }

  protected boolean syncGroups(Map<String, GLGroup> targetGroups, Map<String, GLUser> syncedUsers) {
    List<GLGroup> sourceGroups = client.getGroupsWithMembers(null);
    if (sourceGroups != null) {

      // Update memberships of existing groups
      for (GLGroup sourceGroup : sourceGroups) {
        GLGroup targetGroup = targetGroups.get(sourceGroup.getPath());
        if (targetGroup != null) {

          // Fix permissions of existing members
          for (GLUser targetMember : targetGroup.getMembers()) {
            GLUser sourceUser = syncedUsers.get(targetMember.getUsername());
            if (sourceUser != null) {
              GLPermission sourcePermission = sourceGroup.getPermission(sourceUser);
              GLPermission targetPermission = targetGroup.getPermission(targetMember);
              if (sourcePermission != targetPermission) {
                if (sourcePermission != null) {
                  if (client.removeMemberFromGroup(sourceGroup, sourceUser)) {
                    publishSyncEvent(GitLabEvents.userRemovedFromGroup(sourceUser, sourceGroup));
                  }
                }
                if (client.addMemberToGroup(sourceGroup, sourceUser, targetPermission)) {
                  publishSyncEvent(GitLabEvents.userAddedToGroup(sourceUser, sourceGroup, targetPermission));
                }
                sourceGroup.addMember(sourceUser, targetPermission);
              }
            }
          }

          // Remove blocked users
          for (GLUser sourceUser : sourceGroup.getMembers()) {
            if (sourceUser.getState() == GLState.BLOCKED) {
              if (client.removeMemberFromGroup(sourceGroup, sourceUser)) {
                publishSyncEvent(GitLabEvents.userRemovedFromGroup(sourceUser, sourceGroup));
              }
            }
          }

          // Optionally remove manually added users from projects
          if (removeProjectMembers) {
            syncProjects(sourceGroup, syncedUsers);
          }
        }
      }

      // Mark existing groups (by removing from list of current groups) and create new
      // ones
      for (GLGroup targetGroup : targetGroups.values()) {
        if (sourceGroups.contains(targetGroup)) {
          sourceGroups.remove(targetGroup);
        } else {
          GLGroup newGroup = client.createGroup(targetGroup.getPath(), targetGroup.getPath(), null);
          if (newGroup != null) {
            publishSyncEvent(GitLabEvents.groupCreated(newGroup));

            // Adding group members
            for (GLUser targetMember : targetGroup.getMembers()) {
              GLPermission targetPermission = targetGroup.getPermission(targetMember);
              GLUser sourceUser = syncedUsers.get(targetMember.getUsername());
              if (sourceUser != null) {
                if (client.addMemberToGroup(newGroup, sourceUser, targetPermission)) {
                  publishSyncEvent(GitLabEvents.userAddedToGroup(sourceUser, newGroup, targetPermission));
                }
              }
            }
          }
        }
      }

      // Remove all users from GitLab groups which are not available in IDP anymore
      for (GLGroup sourceGroup : sourceGroups) {
        for (GLUser sourceUser : sourceGroup.getMembers()) {
          if (!excludedUsers.contains(sourceUser.getUsername())) {
            if (client.removeMemberFromGroup(sourceGroup, sourceUser)) {
              publishSyncEvent(GitLabEvents.userRemovedFromGroup(sourceUser, sourceGroup));
            }
          }
        }
      }
      return true;
    }
    return false;
  }

  protected void syncProjects(GLGroup group, Map<String, GLUser> syncedUsers) {
    List<GLProject> sourceProjects = client.getProjectsFromGroup(group, null);
    if (sourceProjects != null) {
      for (GLProject project : sourceProjects) {
        LOG.debug("Syncing user of project '{}'", project.getPath());
        List<GLUser> projectUsers = client.getProjectUsers(project);
        for (GLUser user : projectUsers) {
          if (!group.isMember(user)) {
            LOG.warn("Removing user '{}' from project '{}' because this user is not a member of group '{}'",
                user.getUsername(), project.getPath(), group.getPath());
            if (client.removeMemberFromProject(project, user)) {
              publishSyncEvent(GitLabEvents.userRemovedFromProject(user, project));
            }
          }
        }
      }
    }
  }

  protected void retrieveTargetGroupsAndUsers(List<IdpGroup> idpGroups, Map<String, GLGroup> glGroups,
      Map<String, GLUser> glUsers) {
    for (IdpGroup idpGroup : idpGroups) {
      LOG.debug("Converting IDP group '{}'", idpGroup.getPath());
      Matcher matcher = getGroupNameMatcher(idpGroup.getName());
      String groupPath = matcher.group("groupPath");
      String permissionName = matcher.group("permission");
      LOG.debug("Extracted GitLab information from IDP group: groupPath={}, permissionName={}", groupPath,
          permissionName);
      if (groupPath == null) {
        throw new IllegalStateException("Could not extract GitLab group path from IDP group");
      }
      if (permissionName == null) {
        throw new IllegalStateException("Could not extract GitLab permission name from IDP group");
      }

      GLGroup glGroup = glGroups.get(groupPath);
      if (glGroup == null) {
        glGroup = new GLGroup();
        glGroup.setPath(groupPath);
        glGroups.put(groupPath, glGroup);
      }

      GLPermission permission = GLPermission.fromName(permissionName);
      if (permission == null) {
        throw new IllegalStateException("Unsupported GitLab permission '" + permissionName + "'");
      }
      for (IdpUser idpUser : idpGroup.getMembers()) {
        GLUser glUser = glUsers.get(idpUser.getUsername());
        if (glUser == null) {
          String externUid = idpUser.getAttribute(providerUidAttribute);
          LOG.debug("Converting IDP user '{} ({})'", idpUser.getUsername(), externUid);
          glUser = new GLUser(idpUser);
          glUser.setUsername(idpUser.getUsername());
          glUser.setName(idpUser.getName());
          glUser.setEmail(idpUser.getEmail());
          glUser.setPermission(permission);
          glUser.setProvider(providerName);
          glUser.setExternUid(externUid);
          glUsers.put(idpUser.getUsername(), glUser);
        }
        glUser.addGroup(glGroup);
        glGroup.addMember(glUser, permission);
      }
    }
  }

  protected Map<String, GLUser> getUsersFromGroups(Collection<GLGroup> groups) {
    Map<String, GLUser> users = new HashMap<>();
    for (GLGroup group : groups) {
      for (GLUser member : group.getMembers()) {
        GLUser user = users.get(member.getUsername());
        if (user == null) {
          user = new GLUser(member.getIdpUser());
          user.setUsername(member.getUsername());
          user.setName(member.getName());
          user.setEmail(member.getEmail());
          users.put(member.getUsername(), user);
        }
        user.addGroup(group);
      }
    }
    return users;
  }
}
