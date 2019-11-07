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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.vaulttec.idm.sync.app.gitlab.GitLab;
import org.vaulttec.idm.sync.app.mattermost.model.MMRole;
import org.vaulttec.idm.sync.app.mattermost.model.MMTeam;
import org.vaulttec.idm.sync.app.mattermost.model.MMUser;
import org.vaulttec.idm.sync.idp.model.IdpGroup;
import org.vaulttec.idm.sync.idp.model.IdpUser;

@RunWith(MockitoJUnitRunner.class)
public class MattermostTest {

  private static final String AUTH_SERVICE = "gitlab";
  private static final String AUTH_UID_ATTRIBUTE = GitLab.USER_ID_ATTRIBUTE;
  private static final String AUTH_DATA = "42";
  private MattermostClient client;
  private AuditEventRepository eventRepository;
  private Mattermost app;

  @Before
  public void setUp() throws Exception {
    client = mock(MattermostClient.class);
    eventRepository = mock(AuditEventRepository.class);
    app = new MattermostBuilder(client, eventRepository)
        .groupRegExp(".*_GIT_(?<teamName>\\w*)_(?<teamAdmin>Maintainer)?.*").authService(AUTH_SERVICE)
        .authUidAttribute("GITLAB_USER_ID").build();
  }

  @Test
  public void testSyncCreateNewTeam() {
    MMTeam team = new MMTeam();
    team.setName("team1");

    when(client.createTeam("team1", "team1")).thenReturn(team);

    List<IdpGroup> idpGroups = new ArrayList<>();
    IdpGroup idpGroup = new IdpGroup();
    idpGroup.setName("APP_GIT_team1_Maintainer");
    idpGroup.setPath("/APP_GIT_team1_Maintainer");
    idpGroups.add(idpGroup);
    IdpGroup idpGroup2 = new IdpGroup();
    idpGroup2.setName("APP_GIT_team1_Developer");
    idpGroup2.setPath("/APP_GIT_team1_Developer");
    idpGroups.add(idpGroup2);

    app.sync(idpGroups);

    verify(client).getUsers();
    verify(client).getTeamsWithMembers();
    verify(client, never()).createUser(null, null, null, null, null, null);
    verify(client).createTeam("team1", "team1");
    verify(client, never()).addMemberToTeam(team, null);
    verify(client, never()).updateTeamMemberRoles(team, null, null);
    verify(client, never()).removeMemberFromTeam(null, null);
    verify(client, never()).updateUserActiveStatus(null, false);

    verify(eventRepository, times(1)).add(any(AuditEvent.class));
  }

  @Test
  public void testSyncCreateNewTeamAndUser() {
    MMUser mmUser = new MMUser();
    mmUser.setId("1");
    mmUser.setUsername("user1");
    mmUser.setFirstName("John");
    mmUser.setLastName("Doo 1");
    mmUser.setEmail("user1@acme.com");
    mmUser.setDeleteAt("0");

    MMTeam team = new MMTeam();
    team.setName("team1");

    when(client.getUsers()).thenReturn(new ArrayList<>());
    when(client.createUser("user1", "John", "Doo 1", "user1@acme.com", null, null /*AUTH_SERVICE, AUTH_DATA*/)).thenReturn(mmUser);
    when(client.getTeamsWithMembers()).thenReturn(new ArrayList<>());
    when(client.createTeam("team1", "team1")).thenReturn(team);
    when(client.addMemberToTeam(team, mmUser)).thenReturn(true);
    when(client.updateTeamMemberRoles(team, mmUser, Arrays.asList(MMRole.TEAM_ADMIN))).thenReturn(true);

    IdpUser idpUser = new IdpUser();
    idpUser.setUsername("user1");
    idpUser.setFirstName("John");
    idpUser.setLastName("Doo 1");
    idpUser.setEmail("user1@acme.com");
    Map<String, List<String>> attributes = new HashMap<>();
    attributes.put(AUTH_UID_ATTRIBUTE, Arrays.asList(AUTH_DATA));
    idpUser.setAttributes(attributes);

    List<IdpGroup> idpGroups = new ArrayList<>();
    IdpGroup idpGroup = new IdpGroup();
    idpGroup.setName("APP_GIT_team1_Maintainer");
    idpGroup.setPath("/APP_GIT_team1_Maintainer");
    idpGroup.addMember(idpUser);
    idpGroups.add(idpGroup);

    app.sync(idpGroups);

    verify(client).getUsers();
    verify(client).getTeamsWithMembers();
    verify(client).createTeam("team1", "team1");
    verify(client).createUser("user1", "John", "Doo 1", "user1@acme.com", null, null /*AUTH_SERVICE, AUTH_DATA*/);
    verify(client).addMemberToTeam(team, mmUser);
    verify(client).updateTeamMemberRoles(team, mmUser, Arrays.asList(MMRole.TEAM_ADMIN));
    verify(client, never()).removeMemberFromTeam(team, null);
    verify(client, never()).updateUserActiveStatus(null, false);

    assertThat(idpUser.isAttributesModified()).isTrue();

    verify(eventRepository, times(4)).add(any(AuditEvent.class));
  }

  @Test
  public void testSyncCreateNewUserInExistingTeam() {
    List<MMUser> mmUsers = new ArrayList<>();
    MMUser mmUser = new MMUser();
    mmUser.setUsername("user1");
    mmUser.setFirstName("User");
    mmUser.setLastName("1");
    mmUser.setEmail("user1@acme.com");
    mmUser.setDeleteAt("1");
    mmUser.setAuthService(AUTH_SERVICE);
    mmUser.setAuthService(AUTH_DATA);
    mmUsers.add(mmUser);
    MMUser mmUser2 = new MMUser();
    mmUser2.setUsername("user2");
    mmUser2.setFirstName("User");
    mmUser2.setLastName("2");
    mmUser2.setEmail("user2@acme.com");
    mmUser2.setDeleteAt("0");
    mmUser2.setAuthService(AUTH_SERVICE);
    mmUser2.setAuthService(AUTH_DATA);

    List<MMTeam> teams = new ArrayList<>();
    MMTeam team = new MMTeam();
    team.setName("team1");
    team.addMember(mmUser, MMRole.TEAM_USER);
    teams.add(team);

    when(client.getUsers()).thenReturn(mmUsers);
    when(client.getTeamsWithMembers()).thenReturn(teams);
    when(client.createUser("user2", "User", "2", "user2@acme.com", null, null /*AUTH_SERVICE, AUTH_DATA*/)).thenReturn(mmUser2);
    when(client.addMemberToTeam(team, mmUser2)).thenReturn(true);

    IdpUser idpUser = new IdpUser();
    idpUser.setUsername("user1");
    idpUser.setFirstName("User");
    idpUser.setLastName("1");
    idpUser.setEmail("user1@acme.com");
    Map<String, List<String>> attributes = new HashMap<>();
    attributes.put(AUTH_UID_ATTRIBUTE, Arrays.asList(AUTH_DATA));
    idpUser.setAttributes(attributes);

    IdpUser idpUser2 = new IdpUser();
    idpUser2.setUsername("user2");
    idpUser2.setFirstName("User");
    idpUser2.setLastName("2");
    idpUser2.setEmail("user2@acme.com");
    idpUser2.setAttributes(attributes);

    List<IdpGroup> idpGroups = new ArrayList<>();
    IdpGroup idpGroup = new IdpGroup();
    idpGroup.setName("APP_GIT_team1_Maintainer");
    idpGroup.setPath("/APP_GIT_team1_Maintainer");
    idpGroup.addMember(idpUser);
    idpGroups.add(idpGroup);
    IdpGroup idpGroup2 = new IdpGroup();
    idpGroup2.setName("APP_GIT_team1_Developer");
    idpGroup2.setPath("/APP_GIT_team1_Developer");
    idpGroup2.addMember(idpUser2);
    idpGroups.add(idpGroup2);

    app.sync(idpGroups);

    verify(client).getUsers();
    verify(client).getTeamsWithMembers();
    verify(client, never()).createTeam("team1", "team1");
    verify(client, never()).createUser("user1", "User", "1", "user1@acme.com", AUTH_SERVICE, AUTH_DATA);
    verify(client).createUser("user2", "User", "2", "user2@acme.com", null, null /*AUTH_SERVICE, AUTH_DATA*/);
    verify(client, never()).addMemberToTeam(team, mmUser);
    verify(client).updateTeamMemberRoles(team, mmUser, Arrays.asList(MMRole.TEAM_ADMIN));
    verify(client).addMemberToTeam(team, mmUser2);
    verify(client, never()).updateTeamMemberRoles(team, mmUser2, Arrays.asList(MMRole.TEAM_USER));
    verify(client, never()).removeMemberFromTeam(team, null);
    verify(client, never()).updateUserActiveStatus(null, false);

    verify(eventRepository, times(2)).add(any(AuditEvent.class));
  }

  @Test
  public void testSyncUserWithoutGroup() {
    List<MMUser> mmUsers = new ArrayList<>();
    MMUser mmUser = new MMUser();
    mmUser.setUsername("user1");
    mmUser.setFirstName("User");
    mmUser.setLastName("1");
    mmUser.setEmail("user1@acme.com");
    mmUser.setDeleteAt("0");
    mmUsers.add(mmUser);

    List<MMTeam> teams = new ArrayList<>();
    MMTeam team = new MMTeam();
    team.setName("team1");
    teams.add(team);
    team.addMember(mmUser, MMRole.TEAM_USER);

    when(client.getUsers()).thenReturn(mmUsers);
    when(client.getTeamsWithMembers()).thenReturn(teams);
    when(client.removeMemberFromTeam(team, mmUser)).thenReturn(true);
    when(client.updateUserActiveStatus(mmUser, false)).thenReturn(true);

    List<IdpGroup> idpGroups = new ArrayList<>();
    IdpGroup idpGroup = new IdpGroup();
    idpGroup.setName("APP_GIT_team1_Maintainer");
    idpGroup.setPath("/APP_GIT_team1_Maintainer");
    idpGroups.add(idpGroup);

    app.sync(idpGroups);

    verify(client).getUsers();
    verify(client).getTeamsWithMembers();
    verify(client, never()).createTeam("team1", "team1");
    verify(client, never()).createUser("user1", "User", "1", "user1@acme.com", AUTH_SERVICE, AUTH_DATA);
    verify(client, never()).addMemberToTeam(team, mmUser);
    verify(client, never()).updateTeamMemberRoles(team, mmUser, Arrays.asList(MMRole.TEAM_ADMIN));
    verify(client).removeMemberFromTeam(team, mmUser);
    verify(client).updateUserActiveStatus(mmUser, false);

    verify(eventRepository, times(2)).add(any(AuditEvent.class));
  }

  @Test
  public void testSyncDeletedTeamWithUser() {
    List<MMUser> mmUsers = new ArrayList<>();
    MMUser mmUser = new MMUser();
    mmUser.setUsername("user1");
    mmUser.setFirstName("User");
    mmUser.setLastName("1");
    mmUser.setEmail("user1@acme.com");
    mmUser.setDeleteAt("0");
    mmUsers.add(mmUser);

    List<MMTeam> teams = new ArrayList<>();
    MMTeam team = new MMTeam();
    team.setName("team1");
    teams.add(team);
    team.addMember(mmUser, MMRole.TEAM_USER);

    when(client.getUsers()).thenReturn(mmUsers);
    when(client.getTeamsWithMembers()).thenReturn(teams);
    when(client.removeMemberFromTeam(team, mmUser)).thenReturn(true);
    when(client.updateUserActiveStatus(mmUser, false)).thenReturn(true);

    List<IdpGroup> idpGroups = new ArrayList<>();

    app.sync(idpGroups);

    verify(client).getUsers();
    verify(client).getTeamsWithMembers();
    verify(client, never()).createTeam("team1", "team1");
    verify(client, never()).createUser("user1", "User", "1", "user1@acme.com", AUTH_SERVICE, AUTH_DATA);
    verify(client, never()).addMemberToTeam(team, mmUser);
    verify(client, never()).updateTeamMemberRoles(team, mmUser, Arrays.asList(MMRole.TEAM_ADMIN));
    verify(client).removeMemberFromTeam(team, mmUser);
    verify(client).updateUserActiveStatus(mmUser, false);

    verify(eventRepository, times(2)).add(any(AuditEvent.class));
  }

  @Test
  public void testSyncTeamWithBlockedUsers() {
    List<MMUser> mmUsers = new ArrayList<>();
    MMUser mmUser = new MMUser();
    mmUser.setUsername("user1");
    mmUser.setFirstName("User");
    mmUser.setLastName("1");
    mmUser.setEmail("user1@acme.com");
    mmUser.setDeleteAt("1");
    mmUsers.add(mmUser);
    MMUser mmUser2 = new MMUser();
    mmUser2.setUsername("user2");
    mmUser2.setFirstName("User");
    mmUser2.setLastName("2");
    mmUser2.setEmail("user2@acme.com");
    mmUser2.setDeleteAt("1");
    mmUsers.add(mmUser2);

    List<MMTeam> teams = new ArrayList<>();
    MMTeam team = new MMTeam();
    team.setName("team1");
    team.addMember(mmUser2, MMRole.TEAM_USER);
    teams.add(team);

    when(client.getUsers()).thenReturn(mmUsers);
    when(client.getTeamsWithMembers()).thenReturn(teams);
    when(client.addMemberToTeam(team, mmUser)).thenReturn(true);
    when(client.updateTeamMemberRoles(team, mmUser, Arrays.asList(MMRole.TEAM_ADMIN))).thenReturn(true);
    when(client.removeMemberFromTeam(team, mmUser2)).thenReturn(true);
    when(client.updateUserActiveStatus(mmUser, true)).thenReturn(true);

    IdpUser idpUser = new IdpUser();
    idpUser.setUsername("user1");
    idpUser.setFirstName("User");
    idpUser.setLastName("1");
    idpUser.setEmail("user1@acme.com");
    Map<String, List<String>> attributes = new HashMap<>();
    attributes.put(AUTH_UID_ATTRIBUTE, Arrays.asList(AUTH_DATA));
    idpUser.setAttributes(attributes);

    List<IdpGroup> idpGroups = new ArrayList<>();
    IdpGroup idpGroup = new IdpGroup();
    idpGroup.setName("APP_GIT_team1_Maintainer");
    idpGroup.setPath("/APP_GIT_team1_Maintainer");
    idpGroup.addMember(idpUser);
    idpGroups.add(idpGroup);

    app.sync(idpGroups);

    verify(client).getUsers();
    verify(client).getTeamsWithMembers();
    verify(client, never()).createTeam("team1", "team1");
    verify(client, never()).createUser("user1", "User", "1", "user1@acme.com", AUTH_SERVICE, AUTH_DATA);
    verify(client).addMemberToTeam(team, mmUser);
    verify(client).updateTeamMemberRoles(team, mmUser, Arrays.asList(MMRole.TEAM_ADMIN));
    verify(client).removeMemberFromTeam(team, mmUser2);
    verify(client).updateUserActiveStatus(mmUser, true);

    verify(eventRepository, times(4)).add(any(AuditEvent.class));
  }

  @Test
  public void testSyncTeamWithExistingBlockedUser() {
    List<MMUser> mmUsers = new ArrayList<>();
    MMUser mmUser = new MMUser();
    mmUser.setUsername("user1");
    mmUser.setFirstName("User");
    mmUser.setLastName("1");
    mmUser.setEmail("user1@acme.com");
    mmUser.setDeleteAt("1");
    mmUsers.add(mmUser);

    List<MMTeam> teams = new ArrayList<>();
    MMTeam team = new MMTeam();
    team.setName("team1");
    teams.add(team);

    when(client.getUsers()).thenReturn(mmUsers);
    when(client.getTeamsWithMembers()).thenReturn(teams);
    when(client.addMemberToTeam(team, mmUser)).thenReturn(true);
    when(client.updateTeamMemberRoles(team, mmUser, Arrays.asList(MMRole.TEAM_ADMIN))).thenReturn(true);
    when(client.updateUserActiveStatus(mmUser, true)).thenReturn(true);

    IdpUser idpUser = new IdpUser();
    idpUser.setUsername("user1");
    idpUser.setFirstName("User");
    idpUser.setLastName("1");
    idpUser.setEmail("user1@acme.com");
    Map<String, List<String>> attributes = new HashMap<>();
    attributes.put(AUTH_UID_ATTRIBUTE, Arrays.asList(AUTH_DATA));
    idpUser.setAttributes(attributes);

    List<IdpGroup> idpGroups = new ArrayList<>();
    IdpGroup idpGroup = new IdpGroup();
    idpGroup.setName("APP_GIT_team1_Maintainer");
    idpGroup.setPath("/APP_GIT_team1_Maintainer");
    idpGroup.addMember(idpUser);
    idpGroups.add(idpGroup);

    app.sync(idpGroups);

    verify(client).getUsers();
    verify(client).getTeamsWithMembers();
    verify(client, never()).createTeam("team1", "team1");
    verify(client, never()).createUser("user1", "User", "1", "user1@acme.com", AUTH_SERVICE, AUTH_DATA);
    verify(client).addMemberToTeam(team, mmUser);
    verify(client).updateTeamMemberRoles(team, mmUser, Arrays.asList(MMRole.TEAM_ADMIN));
    verify(client, never()).removeMemberFromTeam(team, mmUser);
    verify(client).updateUserActiveStatus(mmUser, true);

    verify(eventRepository, times(3)).add(any(AuditEvent.class));
  }

  @Test
  public void testSyncTeamWithRemovedUsers() {
    List<MMUser> mmUsers = new ArrayList<>();
    MMUser mmUser = new MMUser();
    mmUser.setId("1");
    mmUser.setUsername("user1");
    mmUser.setFirstName("User");
    mmUser.setLastName("1");
    mmUser.setEmail("user1@acme.com");
    mmUser.setDeleteAt("0");
    mmUsers.add(mmUser);
    MMUser mmUser2 = new MMUser();
    mmUser2.setId("2");
    mmUser2.setUsername("user2");
    mmUser2.setFirstName("User");
    mmUser2.setLastName("2");
    mmUser2.setEmail("user2@acme.com");
    mmUser2.setDeleteAt("0");
    mmUsers.add(mmUser2);

    List<MMTeam> teams = new ArrayList<>();
    MMTeam team = new MMTeam();
    team.setName("team1");
    team.addMember(mmUser, MMRole.TEAM_USER);
    team.addMember(mmUser2, MMRole.TEAM_ADMIN);
    teams.add(team);

    when(client.getUsers()).thenReturn(mmUsers);
    when(client.getTeamsWithMembers()).thenReturn(teams);
    when(client.removeMemberFromTeam(team, mmUser2)).thenReturn(true);
    when(client.updateUserActiveStatus(mmUser2, false)).thenReturn(true);

    IdpUser idpUser = new IdpUser();
    idpUser.setUsername("user1");
    idpUser.setFirstName("User");
    idpUser.setLastName("1");
    idpUser.setEmail("user1@acme.com");
    Map<String, List<String>> attributes = new HashMap<>();
    attributes.put(AUTH_UID_ATTRIBUTE, Arrays.asList(AUTH_DATA));
    attributes.put(Mattermost.USER_ID_ATTRIBUTE, Arrays.asList("1"));
    idpUser.setAttributes(attributes);

    List<IdpGroup> idpGroups = new ArrayList<>();
    IdpGroup idpGroup = new IdpGroup();
    idpGroup.setName("APP_GIT_team1_Maintainer");
    idpGroup.setPath("/APP_GIT_team1_Maintainer");
    idpGroup.addMember(idpUser);
    idpGroups.add(idpGroup);

    app.sync(idpGroups);

    verify(client).getUsers();
    verify(client).getTeamsWithMembers();
    verify(client, never()).createTeam("team1", "team1");
    verify(client, never()).createUser("user1", "User", "1", "user1@acme.com", AUTH_SERVICE, AUTH_DATA);
    verify(client, never()).addMemberToTeam(team, mmUser);
    verify(client).removeMemberFromTeam(team, mmUser2);
    verify(client).updateUserActiveStatus(mmUser2, false);

    assertThat(idpUser.isAttributesModified()).isFalse();

    verify(eventRepository, times(2)).add(any(AuditEvent.class));
  }

  @Test
  public void testSyncUpdateUserAttributesForExistingUser() {
    List<MMUser> mmUsers = new ArrayList<>();
    MMUser mmUser = new MMUser();
    mmUser.setId("1");
    mmUser.setUsername("user1");
    mmUser.setFirstName("User");
    mmUser.setLastName("1");
    mmUser.setEmail("user1@acme.com");
    mmUser.setDeleteAt("1");
    mmUser.setAuthService(AUTH_SERVICE);
    mmUser.setAuthService(AUTH_DATA);
    mmUsers.add(mmUser);

    List<MMTeam> teams = new ArrayList<>();
    MMTeam team = new MMTeam();
    team.setName("team1");
    team.addMember(mmUser, MMRole.TEAM_USER);
    teams.add(team);

    when(client.getUsers()).thenReturn(mmUsers);
    when(client.getTeamsWithMembers()).thenReturn(teams);
    when(client.updateTeamMemberRoles(team, mmUser, Arrays.asList(MMRole.TEAM_ADMIN))).thenReturn(true);

    IdpUser idpUser = new IdpUser();
    idpUser.setUsername("user1");
    idpUser.setFirstName("User");
    idpUser.setLastName("1");
    idpUser.setEmail("user1@acme.com");
    Map<String, List<String>> attributes = new HashMap<>();
    attributes.put(AUTH_UID_ATTRIBUTE, Arrays.asList(AUTH_DATA));
    idpUser.setAttributes(attributes);

    List<IdpGroup> idpGroups = new ArrayList<>();
    IdpGroup idpGroup = new IdpGroup();
    idpGroup.setName("APP_GIT_team1_Maintainer");
    idpGroup.setPath("/APP_GIT_team1_Maintainer");
    idpGroup.addMember(idpUser);
    idpGroups.add(idpGroup);

    app.sync(idpGroups);

    verify(client).getUsers();
    verify(client).getTeamsWithMembers();
    verify(client, never()).createTeam("team1", "team1");
    verify(client, never()).createUser("user1", "User", "1", "user1@acme.com", AUTH_SERVICE, AUTH_DATA);
    verify(client, never()).addMemberToTeam(team, mmUser);
    verify(client).updateTeamMemberRoles(team, mmUser, Arrays.asList(MMRole.TEAM_ADMIN));
    verify(client, never()).removeMemberFromTeam(team, null);
    verify(client, never()).updateUserActiveStatus(null, false);

    assertThat(idpUser.isAttributesModified()).isTrue();

    verify(eventRepository).add(any(AuditEvent.class));
  }
}
