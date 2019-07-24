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
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.vaulttec.idm.sync.app.AbstractApplication;
import org.vaulttec.idm.sync.app.model.AppStatistics;
import org.vaulttec.idm.sync.idp.IdpGroup;
import org.vaulttec.idm.sync.idp.IdpGroupRepresentation;
import org.vaulttec.idm.sync.idp.IdpUser;
import org.vaulttec.util.StringUtils;

public class Mattermost extends AbstractApplication {

  private static final Logger LOG = LoggerFactory.getLogger(Mattermost.class);

  public static final String APPLICATION_ID = "mattermost";
  public static final String USER_ID_ATTRIBUTE = "MATTERMOST_USER_ID";

  private final MattermostClient client;
  private final Set<String> excludedUsers;
  private final String authService;
  private final String authUidAttribute;

  Mattermost(MattermostClient client, AuditEventRepository eventRepository, String groupSearch, String groupRegExp,
      String excludedUsers, String authService, String authUidAttribute) {
    super(eventRepository, groupSearch, groupRegExp);
    LOG.debug("Init: groupSearch={}, groupRegExp={}", groupSearch, groupRegExp);
    this.client = client;
    this.excludedUsers = StringUtils.commaDelimitedListToTrimmedSet(excludedUsers);
    this.authService = authService;
    this.authUidAttribute = authUidAttribute;
  }

  @Override
  public String getId() {
    return APPLICATION_ID;
  }

  @Override
  public String getName() {
    return "Mattermost";
  }

  @Override
  public IdpGroupRepresentation getGroupRepresentation(IdpGroup group) {
    if (group != null) {
      Matcher matcher = getGroupNameMatcher(group.getName());
      String teamName = matcher.group("teamName");
      MMRole teamRole = MMRole.TEAM_USER;
      try {
        if (matcher.group("teamAdmin") != null) {
          teamRole = MMRole.TEAM_ADMIN;
        }
      } catch (IllegalArgumentException e) {
        // ignore missing teamAdmin matching group
      }
      if (teamName != null) {
        return new IdpGroupRepresentation("Team", teamName, teamRole.name());
      }
    }
    return null;
  }

  @Override
  public boolean sync(List<IdpGroup> idpGroups) {
    Map<String, MMTeam> targetGroups = new HashMap<>();
    Map<String, MMUser> targetUsers = new HashMap<>();
    retrieveTargetGroupsAndUsers(idpGroups, targetGroups, targetUsers);
    Map<String, MMUser> syncedUsers = syncUsers(targetUsers);
    if (syncedUsers != null) {
      if (syncTeams(targetGroups, syncedUsers)) {
        return true;
      }
    }
    return false;
  }

  protected Map<String, MMUser> syncUsers(Map<String, MMUser> targetUsers) {
    List<MMUser> sourceUsers = client.getUsers();
    if (sourceUsers != null) {
      Map<String, MMUser> syncedUsers = new HashMap<>();

      // Activate existing users associated with Mattermost team now
      for (MMUser sourceUser : sourceUsers) {
        MMUser targetUser = targetUsers.get(sourceUser.getUsername());
        if (targetUser != null) {
          if (!sourceUser.isActive()) {
            if (client.updateUserActiveStatus(sourceUser, true)) {
              publishSyncEvent(MattermostEvents.userActivated(sourceUser));
            }
            sourceUser.setDeleteAt("0");
          }
          syncedUsers.put(sourceUser.getUsername(), sourceUser);
          updateUserIdAttribute(targetUser.getIdpUser(), sourceUser);
        }
      }

      // Mark existing users (by removing from list of current users) and create
      // non-existing ones
      for (MMUser targetUser : targetUsers.values()) {
        if (sourceUsers.contains(targetUser)) {
          sourceUsers.remove(targetUser);
        } else {
          if (StringUtils.hasText(targetUser.getAuthService()) && !StringUtils.hasText(targetUser.getAuthData())) {
            LOG.warn("New user '{}' not created - missing required authentication data for authentication service '{}'",
                targetUser.getUsername(), targetUser.getAuthService());
          } else if (!StringUtils.hasText(targetUser.getEmail())) {
            LOG.warn("New user '{}' not created - missing required email address", targetUser.getUsername());
          } else {
            MMUser newUser = client.createUser(targetUser.getUsername(), targetUser.getFirstName(),
                targetUser.getLastName(), targetUser.getEmail(), targetUser.getAuthService(), targetUser.getAuthData());
            if (newUser != null) {
              publishSyncEvent(MattermostEvents.userCreated(newUser));
              syncedUsers.put(newUser.getUsername(), newUser);
              updateUserIdAttribute(targetUser.getIdpUser(), newUser);
            }
          }
        }
      }

      // Deactivate existing users which are not associated with Mattermost groups
      // anymore
      for (MMUser sourceUser : sourceUsers) {
        if (!excludedUsers.contains(sourceUser.getUsername())) {
          if (sourceUser.isActive()) {
            if (client.updateUserActiveStatus(sourceUser, false)) {
              publishSyncEvent(MattermostEvents.userDeactivated(sourceUser));
            }
            sourceUser.setDeleteAt("1");
          }
          syncedUsers.put(sourceUser.getUsername(), sourceUser);
        }
      }
      return syncedUsers;
    }
    return null;
  }

  /**
   * Make sure that the value of the IdP's user attribute "MATTERMOST_USER_ID"
   * matches the given Mattermost user's ID.
   */
  protected void updateUserIdAttribute(IdpUser idpUser, MMUser mmUser) {
    String userId = idpUser.getAttribute(USER_ID_ATTRIBUTE);
    if (userId == null || !userId.equals(mmUser.getId())) {
      idpUser.getAttributes().put(USER_ID_ATTRIBUTE, Arrays.asList(mmUser.getId()));
      idpUser.setAttributesModified(true);
    }
  }

  protected boolean syncTeams(Map<String, MMTeam> targetTeams, Map<String, MMUser> syncedUsers) {
    List<MMTeam> sourceTeams = client.getTeamsWithMembers();
    if (sourceTeams != null) {

      // Update memberships of existing groups
      for (MMTeam sourceTeam : sourceTeams) {
        MMTeam targetTeam = targetTeams.get(sourceTeam.getName());
        if (targetTeam != null) {

          // Add missing members and update member roles
          for (MMUser targetMember : targetTeam.getMembers()) {
            MMUser sourceUser = syncedUsers.get(targetMember.getUsername());
            if (sourceUser != null) {
              MMRole targetRole = targetTeam.getMemberRole(targetMember.getUsername());
              if (!sourceTeam.hasMember(sourceUser)) {
                if (client.addMemberToTeam(sourceTeam, sourceUser)) {
                  publishSyncEvent(MattermostEvents.userAddedToTeam(sourceUser, sourceTeam));
                }
                sourceTeam.addMember(sourceUser, MMRole.TEAM_USER);
              }
              if (sourceTeam.getMemberRole(targetMember.getUsername()) != targetRole) {
                if (client.updateTeamMemberRoles(sourceTeam, sourceUser, Arrays.asList(targetRole))) {
                  publishSyncEvent(MattermostEvents.userRoleUpdatedInTeam(sourceUser, sourceTeam, targetRole));
                }
              }
            }
          }

          // Remove inactive users or users which are not members any more
          for (MMUser sourceUser : sourceTeam.getMembers()) {
            if (!sourceUser.isActive() || !targetTeam.hasMember(sourceUser)) {
              if (client.removeMemberFromTeam(sourceTeam, sourceUser)) {
                publishSyncEvent(MattermostEvents.userRemovedFromTeam(sourceUser, sourceTeam));
              }
            }
          }
        }
      }

      // Mark existing teams (by removing from list of current teams) and create new
      // ones
      for (MMTeam targetTeam : targetTeams.values()) {
        if (sourceTeams.contains(targetTeam)) {
          sourceTeams.remove(targetTeam);
        } else {
          MMTeam newTeam = client.createTeam(targetTeam.getName(), targetTeam.getName());
          if (newTeam != null) {
            publishSyncEvent(MattermostEvents.teamCreated(newTeam));

            // Adding team members
            for (MMUser targetMember : targetTeam.getMembers()) {
              MMUser sourceUser = syncedUsers.get(targetMember.getUsername());
              if (sourceUser != null) {
                MMRole role = targetTeam.getMemberRole(targetMember.getUsername());
                if (client.addMemberToTeam(newTeam, sourceUser)) {
                  publishSyncEvent(MattermostEvents.userAddedToTeam(sourceUser, newTeam));
                }
                if (role != MMRole.TEAM_USER
                    && client.updateTeamMemberRoles(newTeam, sourceUser, Arrays.asList(role))) {
                  publishSyncEvent(MattermostEvents.userRoleUpdatedInTeam(sourceUser, newTeam, role));
                }
              }
            }
          }
        }
      }

      // Remove all users from Mattermost teams which are not available in IDP anymore
      for (MMTeam sourceTeam : sourceTeams) {
        for (MMUser sourceUser : sourceTeam.getMembers()) {
          if (!excludedUsers.contains(sourceUser.getUsername())) {
            if (client.removeMemberFromTeam(sourceTeam, sourceUser)) {
              publishSyncEvent(MattermostEvents.userRemovedFromTeam(sourceUser, sourceTeam));
            }
          }
        }
      }
      return true;
    }
    return false;
  }

  protected void retrieveTargetGroupsAndUsers(List<IdpGroup> idpGroups, Map<String, MMTeam> mmTeams,
      Map<String, MMUser> mmUsers) {
    for (IdpGroup idpGroup : idpGroups) {
      LOG.debug("Converting IDP group '{}'", idpGroup.getPath());
      Matcher matcher = getGroupNameMatcher(idpGroup.getName());
      String teamName = matcher.group("teamName");
      MMRole teamRole = MMRole.TEAM_USER;
      try {
        if (matcher.group("teamAdmin") != null) {
          teamRole = MMRole.TEAM_ADMIN;
        }
      } catch (IllegalArgumentException e) {
        // ignore missing teamAdmin matching group
      }
      LOG.debug("Extracted Mattermost information from IDP group: teamName={}, teamRole={}", teamName, teamRole);
      if (teamName == null) {
        throw new IllegalStateException("Could not extract Mattermost team path from IDP group");
      }

      MMTeam mmTeam = mmTeams.get(teamName);
      if (mmTeam == null) {
        mmTeam = new MMTeam();
        mmTeam.setName(teamName);
        mmTeams.put(teamName, mmTeam);
      }

      for (IdpUser idpUser : idpGroup.getMembers()) {
        MMUser mmUser = mmUsers.get(idpUser.getUsername());
        if (mmUser == null) {
          LOG.debug("Converting IDP user '{} ({})'", idpUser.getUsername(), idpUser.getAttribute(authUidAttribute));
          mmUser = new MMUser(idpUser);
          mmUser.setUsername(idpUser.getUsername());
          mmUser.setFirstName(idpUser.getFirstName());
          mmUser.setLastName(idpUser.getLastName());
          mmUser.setEmail(idpUser.getEmail());
          mmUser.setAuthService(authService);
          mmUser.setAuthData(idpUser.getAttribute(authUidAttribute));
          mmUsers.put(idpUser.getUsername(), mmUser);
        }
        mmUser.addTeam(mmTeam);
        mmTeam.addMember(mmUser, teamRole);
      }
    }
  }

  @Override
  public List<AppStatistics> getStatistics() {
    List<AppStatistics> statistics = new ArrayList<>();
    List<MMTeam> teams = client.getTeamsWithMembers();
    for (MMTeam team : teams) {
      List<MMTeamChannel> channels = client.getTeamChannels(team);
      int messageCount = channels.stream().mapToInt(c -> c.getMessageCount()).sum();
      Optional<MMTeamChannel> channelWithMostMessages = channels.stream()
          .sorted(Comparator.comparing(MMTeamChannel::getMessageCount).reversed()).findFirst();
      AppStatistics groupStatistics = new AppStatistics(team.getName());
      groupStatistics.addStatistic("members", Integer.toString(team.getMembers().size()));
      groupStatistics.addStatistic("channels", Integer.toString(channels.size()));
      groupStatistics.addStatistic("channel_with_most_messages",
          channelWithMostMessages.isPresent() && channelWithMostMessages.get().getMessageCount() > 0
              ? channelWithMostMessages.get().getName()
              : "");
      groupStatistics.addStatistic("messages", Integer.toString(messageCount));
      statistics.add(groupStatistics);
    }
    return statistics;
  }
}
