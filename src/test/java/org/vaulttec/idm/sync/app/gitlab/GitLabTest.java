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
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.vaulttec.idm.sync.app.gitlab.model.GLGroup;
import org.vaulttec.idm.sync.app.gitlab.model.GLPermission;
import org.vaulttec.idm.sync.app.gitlab.model.GLProject;
import org.vaulttec.idm.sync.app.gitlab.model.GLState;
import org.vaulttec.idm.sync.app.gitlab.model.GLUser;
import org.vaulttec.idm.sync.idp.model.IdpGroup;
import org.vaulttec.idm.sync.idp.model.IdpUser;

public class GitLabTest {

  private static final String PROVIDER_NAME = "ldapmain";
  private static final String EXTERNAL_UID_ATTRIBUTE = "LDAP_ENTRY_DN";
  private static final String EXTERNAL_UID = "E42";
  private static final String GITLAB_ID = "G42";
  private GitLabClient client;
  private AuditEventRepository eventRepository;
  private GitLab app;

  @Before
  public void setUp() throws Exception {
    client = mock(GitLabClient.class);
    eventRepository = mock(AuditEventRepository.class);
    app = new GitLabBuilder(client, eventRepository).groupRegExp("APP_GIT_(?<groupPath>\\w*)_(?<permission>\\w*)")
        .removeProjectMembers(true).providerName(PROVIDER_NAME).providerUidAttribute(EXTERNAL_UID_ATTRIBUTE).build();
  }

  @Test
  public void testSyncCreateNewGroup() {
    GLGroup glGroup = new GLGroup();
    glGroup.setPath("grp1");
    glGroup.setName("grp1");

    when(client.createGroup("grp1", "grp1", null)).thenReturn(glGroup);

    List<IdpGroup> idpGroups = new ArrayList<>();
    IdpGroup idpGroup = new IdpGroup();
    idpGroup.setName("APP_GIT_grp1_Maintainer");
    idpGroup.setPath("/APP_GIT_grp1_Maintainer");
    idpGroups.add(idpGroup);
    IdpGroup idpGroup2 = new IdpGroup();
    idpGroup2.setName("APP_GIT_grp1_Developer");
    idpGroup2.setPath("/APP_GIT_grp1_Developer");
    idpGroups.add(idpGroup2);

    app.sync(idpGroups);

    verify(client).getUsers(null);
    verify(client).getGroupsWithMembers(null, false);
    verify(client, never()).createUser(null, null, null, null, null);
    verify(client).createGroup("grp1", "grp1", null);
    verify(client, never()).addMemberToGroup(glGroup, null, null);
    verify(client, never()).removeMemberFromGroup(null, null);
    verify(client, never()).blockUser(null);
    verify(client, never()).unblockUser(null);

    verify(eventRepository, times(1)).add(any(AuditEvent.class));
  }

  @Test
  public void testSyncCreateNewGroupAndUser() {
    GLUser glUser = new GLUser();
    glUser.setId(GITLAB_ID);
    glUser.setUsername("user1");
    glUser.setName("User 1");
    glUser.setEmail("user1@acme.com");
    glUser.setState(GLState.ACTIVE);

    GLGroup glGroup = new GLGroup();
    glGroup.setPath("grp1");
    glGroup.setName("grp1");

    when(client.getUsers(null)).thenReturn(new ArrayList<>());
    when(client.createUser("user1", "User 1", "user1@acme.com", PROVIDER_NAME, EXTERNAL_UID)).thenReturn(glUser);
    when(client.getGroupsWithMembers(null, false)).thenReturn(new ArrayList<>());
    when(client.createGroup("grp1", "grp1", null)).thenReturn(glGroup);
    when(client.addMemberToGroup(glGroup, glUser, GLPermission.MAINTAINER)).thenReturn(true);

    IdpUser idpUser = new IdpUser();
    idpUser.setUsername("user1");
    idpUser.setFirstName("User");
    idpUser.setLastName("1");
    idpUser.setEmail("user1@acme.com");
    Map<String, List<String>> attributes = new HashMap<>();
    attributes.put(EXTERNAL_UID_ATTRIBUTE, Arrays.asList(EXTERNAL_UID));
    idpUser.setAttributes(attributes);

    List<IdpGroup> idpGroups = new ArrayList<>();
    IdpGroup idpGroup = new IdpGroup();
    idpGroup.setName("APP_GIT_grp1_Maintainer");
    idpGroup.setPath("/APP_GIT_grp1_Maintainer");
    idpGroup.addMember(idpUser);
    idpGroups.add(idpGroup);

    app.sync(idpGroups);

    verify(client).getUsers(null);
    verify(client).getGroupsWithMembers(null, false);
    verify(client).createGroup("grp1", "grp1", null);
    verify(client).createUser("user1", "User 1", "user1@acme.com", PROVIDER_NAME, EXTERNAL_UID);
    verify(client).addMemberToGroup(glGroup, glUser, GLPermission.MAINTAINER);
    verify(client, never()).removeMemberFromGroup(glGroup, null);
    verify(client, never()).blockUser(null);
    verify(client, never()).unblockUser(null);

    assertThat(idpUser.isAttributesModified()).isTrue();

    verify(eventRepository, times(3)).add(any(AuditEvent.class));
    assertThat(idpUser.getAttribute(GitLab.USER_ID_ATTRIBUTE)).isEqualTo(GITLAB_ID);
  }

  @Test
  public void testSyncCreateNewUserInExistingGroup() {
    List<GLUser> glUsers = new ArrayList<>();
    GLUser glUser = new GLUser();
    glUser.setUsername("user1");
    glUser.setName("User 1");
    glUser.setEmail("user1@acme.com");
    glUser.setState(GLState.ACTIVE);
    glUsers.add(glUser);
    GLUser glUser2 = new GLUser();
    glUser2.setId(GITLAB_ID);
    glUser2.setUsername("user2");
    glUser2.setName("User 2");
    glUser2.setEmail("user2@acme.com");
    glUser2.setState(GLState.ACTIVE);

    List<GLGroup> glGroups = new ArrayList<>();
    GLGroup glGroup = new GLGroup();
    glGroup.setPath("grp1");
    glGroup.setName("grp1");
    glGroup.addMember(glUser, GLPermission.MAINTAINER);
    glGroups.add(glGroup);

    when(client.getUsers(null)).thenReturn(glUsers);
    when(client.getGroupsWithMembers(null, false)).thenReturn(glGroups);
    when(client.createUser("user2", "User 2", "user2@acme.com", PROVIDER_NAME, EXTERNAL_UID)).thenReturn(glUser2);
    when(client.addMemberToGroup(glGroup, glUser2, GLPermission.DEVELOPER)).thenReturn(true);

    IdpUser idpUser = new IdpUser();
    idpUser.setUsername("user1");
    idpUser.setFirstName("User");
    idpUser.setLastName("1");
    idpUser.setEmail("user1@acme.com");
    Map<String, List<String>> attributes = new HashMap<>();
    attributes.put(EXTERNAL_UID_ATTRIBUTE, Arrays.asList(EXTERNAL_UID));
    idpUser.setAttributes(attributes);
    IdpUser idpUser2 = new IdpUser();
    idpUser2.setUsername("user2");
    idpUser2.setFirstName("User");
    idpUser2.setLastName("2");
    idpUser2.setEmail("user2@acme.com");
    idpUser2.setAttributes(attributes);

    List<IdpGroup> idpGroups = new ArrayList<>();
    IdpGroup idpGroup = new IdpGroup();
    idpGroup.setName("APP_GIT_grp1_Maintainer");
    idpGroup.setPath("/APP_GIT_grp1_Maintainer");
    idpGroup.addMember(idpUser);
    idpGroups.add(idpGroup);
    IdpGroup idpGroup2 = new IdpGroup();
    idpGroup2.setName("APP_GIT_grp1_Developer");
    idpGroup2.setPath("/APP_GIT_grp1_Developer");
    idpGroup2.addMember(idpUser2);
    idpGroups.add(idpGroup2);

    app.sync(idpGroups);

    verify(client).getUsers(null);
    verify(client).getGroupsWithMembers(null, false);
    verify(client, never()).createGroup("grp1", "grp1", null);
    verify(client, never()).createUser("user1", "User 1", "user1@acme.com", PROVIDER_NAME, EXTERNAL_UID);
    verify(client).createUser("user2", "User 2", "user2@acme.com", PROVIDER_NAME, EXTERNAL_UID);
    verify(client, never()).addMemberToGroup(glGroup, glUser, GLPermission.MAINTAINER);
    verify(client).addMemberToGroup(glGroup, glUser2, GLPermission.DEVELOPER);
    verify(client, never()).removeMemberFromGroup(glGroup, null);
    verify(client, never()).blockUser(null);
    verify(client, never()).unblockUser(null);

    verify(eventRepository, times(2)).add(any(AuditEvent.class));

    assertThat(idpUser2.isAttributesModified()).isTrue();
    assertThat(idpUser2.getAttribute(GitLab.USER_ID_ATTRIBUTE)).isEqualTo(GITLAB_ID);
  }

  @Test
  public void testSyncUserWithoutGroup() {
    List<GLUser> glUsers = new ArrayList<>();
    GLUser glUser = new GLUser();
    glUser.setUsername("user1");
    glUser.setName("User 1");
    glUser.setEmail("user1@acme.com");
    glUser.setState(GLState.ACTIVE);
    glUsers.add(glUser);

    List<GLGroup> glGroups = new ArrayList<>();
    GLGroup glGroup = new GLGroup();
    glGroup.setPath("grp1");
    glGroup.setName("grp1");
    glGroups.add(glGroup);
    glGroup.addMember(glUser, GLPermission.MAINTAINER);

    when(client.getUsers(null)).thenReturn(glUsers);
    when(client.getGroupsWithMembers(null, false)).thenReturn(glGroups);
    when(client.removeMemberFromGroup(glGroup, glUser)).thenReturn(true);
    when(client.blockUser(glUser)).thenReturn(true);

    List<IdpGroup> idpGroups = new ArrayList<>();
    IdpGroup idpGroup = new IdpGroup();
    idpGroup.setName("APP_GIT_grp1_Maintainer");
    idpGroup.setPath("/APP_GIT_grp1_Maintainer");
    idpGroups.add(idpGroup);

    app.sync(idpGroups);

    verify(client).getUsers(null);
    verify(client).getGroupsWithMembers(null, false);
    verify(client, never()).createGroup("grp1", "grp1", null);
    verify(client, never()).createUser("user1", "User 1", "user1@acme.com", null, null);
    verify(client, never()).addMemberToGroup(glGroup, glUser, GLPermission.MAINTAINER);
    verify(client).removeMemberFromGroup(glGroup, glUser);
    verify(client).blockUser(glUser);
    verify(client, never()).unblockUser(glUser);

    verify(eventRepository, times(2)).add(any(AuditEvent.class));
  }

  @Test
  public void testSyncDeletedGroupWithUser() {
    List<GLUser> glUsers = new ArrayList<>();
    GLUser glUser = new GLUser();
    glUser.setUsername("user1");
    glUser.setName("User 1");
    glUser.setEmail("user1@acme.com");
    glUser.setState(GLState.ACTIVE);
    glUsers.add(glUser);

    List<GLGroup> glGroups = new ArrayList<>();
    GLGroup glGroup = new GLGroup();
    glGroup.setPath("grp1");
    glGroup.setName("grp1");
    glGroups.add(glGroup);
    glGroup.addMember(glUser, GLPermission.MAINTAINER);

    when(client.getUsers(null)).thenReturn(glUsers);
    when(client.getGroupsWithMembers(null, false)).thenReturn(glGroups);
    when(client.removeMemberFromGroup(glGroup, glUser)).thenReturn(true);
    when(client.blockUser(glUser)).thenReturn(true);

    List<IdpGroup> idpGroups = new ArrayList<>();

    app.sync(idpGroups);

    verify(client).getUsers(null);
    verify(client).getGroupsWithMembers(null, false);
    verify(client, never()).createGroup("grp1", "grp1", null);
    verify(client, never()).createUser("user1", "User 1", "user1@acme.com", null, null);
    verify(client, never()).addMemberToGroup(glGroup, glUser, GLPermission.MAINTAINER);
    verify(client).removeMemberFromGroup(glGroup, glUser);
    verify(client).blockUser(glUser);
    verify(client, never()).unblockUser(glUser);

    verify(eventRepository, times(2)).add(any(AuditEvent.class));
  }

  @Test
  public void testSyncGroupWithRemovedUsers() {
    List<GLUser> glUsers = new ArrayList<>();
    GLUser glUser = new GLUser();
    glUser.setUsername("user1");
    glUser.setName("User 1");
    glUser.setEmail("user1@acme.com");
    glUser.setState(GLState.ACTIVE);
    glUsers.add(glUser);
    GLUser glUser2 = new GLUser();
    glUser2.setUsername("user2");
    glUser2.setName("User 2");
    glUser2.setEmail("user2@acme.com");
    glUser2.setState(GLState.ACTIVE);
    glUsers.add(glUser2);

    List<GLGroup> glGroups = new ArrayList<>();
    GLGroup glGroup = new GLGroup();
    glGroup.setPath("grp1");
    glGroup.setName("grp1");
    glGroup.addMember(glUser, GLPermission.MAINTAINER);
    glGroup.addMember(glUser2, GLPermission.MAINTAINER);
    glGroups.add(glGroup);

    when(client.getUsers(null)).thenReturn(glUsers);
    when(client.getGroupsWithMembers(null, false)).thenReturn(glGroups);
    when(client.removeMemberFromGroup(glGroup, glUser2)).thenReturn(true);
    when(client.blockUser(glUser2)).thenReturn(true);

    IdpUser idpUser = new IdpUser();
    idpUser.setUsername("user1");
    idpUser.setFirstName("User");
    idpUser.setLastName("1");
    idpUser.setEmail("user1@acme.com");
    Map<String, List<String>> attributes = new HashMap<>();
    attributes.put(EXTERNAL_UID_ATTRIBUTE, Arrays.asList(EXTERNAL_UID));
    idpUser.setAttributes(attributes);

    List<IdpGroup> idpGroups = new ArrayList<>();
    IdpGroup idpGroup = new IdpGroup();
    idpGroup.setName("APP_GIT_grp1_Maintainer");
    idpGroup.setPath("/APP_GIT_grp1_Maintainer");
    idpGroup.addMember(idpUser);
    idpGroups.add(idpGroup);

    app.sync(idpGroups);

    verify(client).getUsers(null);
    verify(client).getGroupsWithMembers(null, false);
    verify(client, never()).createGroup("grp1", "grp1", null);
    verify(client, never()).createUser("user1", "User 1", "user1@acme.com", null, null);
    verify(client, never()).addMemberToGroup(glGroup, glUser, GLPermission.MAINTAINER);
    verify(client).removeMemberFromGroup(glGroup, glUser2);
    verify(client).blockUser(glUser2);
    verify(client, never()).unblockUser(glUser);

    verify(eventRepository, times(2)).add(any(AuditEvent.class));
  }

  @Test
  public void testSyncGroupWithBlockedUsers() {
    List<GLUser> glUsers = new ArrayList<>();
    GLUser glUser = new GLUser();
    glUser.setId(GITLAB_ID);
    glUser.setUsername("user1");
    glUser.setName("User 1");
    glUser.setEmail("user1@acme.com");
    glUser.setState(GLState.BLOCKED);
    glUsers.add(glUser);
    GLUser glUser2 = new GLUser();
    glUser2.setUsername("user2");
    glUser2.setName("User 2");
    glUser2.setEmail("user2@acme.com");
    glUser2.setState(GLState.BLOCKED);
    glUsers.add(glUser2);

    List<GLGroup> glGroups = new ArrayList<>();
    GLGroup glGroup = new GLGroup();
    glGroup.setPath("grp1");
    glGroup.setName("grp1");
    glGroup.addMember(glUser2, GLPermission.MAINTAINER);
    glGroups.add(glGroup);

    when(client.getUsers(null)).thenReturn(glUsers);
    when(client.getGroupsWithMembers(null, false)).thenReturn(glGroups);
    when(client.removeMemberFromGroup(glGroup, glUser2)).thenReturn(true);
    when(client.unblockUser(glUser)).thenReturn(true);

    IdpUser idpUser = new IdpUser();
    idpUser.setUsername("user1");
    idpUser.setFirstName("User");
    idpUser.setLastName("1");
    idpUser.setEmail("user1@acme.com");
    Map<String, List<String>> attributes = new HashMap<>();
    attributes.put(EXTERNAL_UID_ATTRIBUTE, Arrays.asList(EXTERNAL_UID));
    idpUser.setAttributes(attributes);

    List<IdpGroup> idpGroups = new ArrayList<>();
    IdpGroup idpGroup = new IdpGroup();
    idpGroup.setName("APP_GIT_grp1_Maintainer");
    idpGroup.setPath("/APP_GIT_grp1_Maintainer");
    idpGroup.addMember(idpUser);
    idpGroups.add(idpGroup);

    app.sync(idpGroups);

    verify(client).getUsers(null);
    verify(client).getGroupsWithMembers(null, false);
    verify(client, never()).createGroup("grp1", "grp1", null);
    verify(client, never()).createUser("user1", "User 1", "user1@acme.com", null, null);
    verify(client).addMemberToGroup(glGroup, glUser, GLPermission.MAINTAINER);
    verify(client).removeMemberFromGroup(glGroup, glUser2);
    verify(client, never()).blockUser(glUser);
    verify(client).unblockUser(glUser);

    verify(eventRepository, times(2)).add(any(AuditEvent.class));

    assertThat(idpUser.isAttributesModified()).isTrue();
    assertThat(idpUser.getAttribute(GitLab.USER_ID_ATTRIBUTE)).isEqualTo(GITLAB_ID);
  }

  @Test
  public void testSyncGroupWithExistingBlockedUser() {
    List<GLUser> glUsers = new ArrayList<>();
    GLUser glUser = new GLUser();
    glUser.setId(GITLAB_ID);
    glUser.setUsername("user1");
    glUser.setName("User 1");
    glUser.setEmail("user1@acme.com");
    glUser.setState(GLState.BLOCKED);
    glUsers.add(glUser);

    List<GLGroup> glGroups = new ArrayList<>();
    GLGroup glGroup = new GLGroup();
    glGroup.setPath("grp1");
    glGroup.setName("grp1");
    glGroups.add(glGroup);

    when(client.getUsers(null)).thenReturn(glUsers);
    when(client.getGroupsWithMembers(null, false)).thenReturn(glGroups);
    when(client.addMemberToGroup(glGroup, glUser, GLPermission.MAINTAINER)).thenReturn(true);
    when(client.unblockUser(glUser)).thenReturn(true);

    IdpUser idpUser = new IdpUser();
    idpUser.setUsername("user1");
    idpUser.setFirstName("User");
    idpUser.setLastName("1");
    idpUser.setEmail("user1@acme.com");
    Map<String, List<String>> attributes = new HashMap<>();
    attributes.put(EXTERNAL_UID_ATTRIBUTE, Arrays.asList(EXTERNAL_UID));
    idpUser.setAttributes(attributes);

    List<IdpGroup> idpGroups = new ArrayList<>();
    IdpGroup idpGroup = new IdpGroup();
    idpGroup.setName("APP_GIT_grp1_Maintainer");
    idpGroup.setPath("/APP_GIT_grp1_Maintainer");
    idpGroup.addMember(idpUser);
    idpGroups.add(idpGroup);

    app.sync(idpGroups);

    verify(client).getUsers(null);
    verify(client).getGroupsWithMembers(null, false);
    verify(client, never()).createGroup("grp1", "grp1", null);
    verify(client, never()).createUser("user1", "User 1", "user1@acme.com", null, null);
    verify(client).addMemberToGroup(glGroup, glUser, GLPermission.MAINTAINER);
    verify(client, never()).removeMemberFromGroup(glGroup, glUser);
    verify(client, never()).blockUser(glUser);
    verify(client).unblockUser(glUser);

    assertThat(idpUser.isAttributesModified()).isTrue();
    assertThat(idpUser.getAttribute(GitLab.USER_ID_ATTRIBUTE)).isEqualTo(GITLAB_ID);

    verify(eventRepository, times(2)).add(any(AuditEvent.class));
  }

  @Test
  public void testSyncUserWithMultiplePermissions() {
    List<GLUser> glUsers = new ArrayList<>();
    GLUser glUser = new GLUser();
    glUser.setId(GITLAB_ID);
    glUser.setUsername("user1");
    glUser.setName("User 1");
    glUser.setEmail("user1@acme.com");
    glUser.setState(GLState.ACTIVE);
    glUsers.add(glUser);

    List<GLGroup> glGroups = new ArrayList<>();
    GLGroup glGroup = new GLGroup();
    glGroup.setPath("grp1");
    glGroup.setName("grp1");
    glGroups.add(glGroup);
    glGroup.addMember(glUser, GLPermission.REPORTER);

    when(client.getUsers(null)).thenReturn(glUsers);
    when(client.getGroupsWithMembers(null, false)).thenReturn(glGroups);
    when(client.addMemberToGroup(glGroup, glUser, GLPermission.MAINTAINER)).thenReturn(true);
    when(client.removeMemberFromGroup(glGroup, glUser)).thenReturn(true);

    IdpUser idpUser = new IdpUser();
    idpUser.setUsername("user1");
    idpUser.setFirstName("User");
    idpUser.setLastName("1");
    idpUser.setEmail("user1@acme.com");
    Map<String, List<String>> attributes = new HashMap<>();
    attributes.put(EXTERNAL_UID_ATTRIBUTE, Arrays.asList(EXTERNAL_UID));
    idpUser.setAttributes(attributes);

    List<IdpGroup> idpGroups = new ArrayList<>();
    IdpGroup idpGroup = new IdpGroup();
    idpGroup.setName("APP_GIT_grp1_Developer");
    idpGroup.setPath("/APP_GIT_grp1_Developer");
    idpGroup.addMember(idpUser);
    idpGroups.add(idpGroup);
    IdpGroup idpGroup2 = new IdpGroup();
    idpGroup2.setName("APP_GIT_grp1_Maintainer");
    idpGroup2.setPath("/APP_GIT_grp1_Maintainer");
    idpGroup2.addMember(idpUser);
    idpGroups.add(idpGroup2);
    IdpGroup idpGroup3 = new IdpGroup();
    idpGroup3.setName("APP_GIT_grp1_Reporter");
    idpGroup3.setPath("/APP_GIT_grp1_Reporter");
    idpGroup3.addMember(idpUser);
    idpGroups.add(idpGroup3);

    app.sync(idpGroups);

    verify(client).getUsers(null);
    verify(client).getGroupsWithMembers(null, false);
    verify(client, never()).createGroup("grp1", "grp1", null);
    verify(client, never()).createUser("user1", "User 1", "user1@acme.com", null, null);
    verify(client).addMemberToGroup(glGroup, glUser, GLPermission.MAINTAINER);
    verify(client).removeMemberFromGroup(glGroup, glUser);
    verify(client, never()).blockUser(glUser);
    verify(client, never()).unblockUser(glUser);

    assertThat(idpUser.isAttributesModified()).isTrue();
    assertThat(idpUser.getAttribute(GitLab.USER_ID_ATTRIBUTE)).isEqualTo(GITLAB_ID);

    verify(eventRepository, times(2)).add(any(AuditEvent.class));
  }

  @Test
  public void testSyncManuallyAddedUsersInProject() {
    List<GLUser> glUsers = new ArrayList<>();
    GLUser glUser = new GLUser();
    glUser.setUsername("user1");
    glUser.setName("User 1");
    glUser.setEmail("user1@acme.com");
    glUser.setState(GLState.ACTIVE);
    glUsers.add(glUser);
    GLUser glUser2 = new GLUser();
    glUser2.setUsername("user2");
    glUser2.setName("User 2");
    glUser2.setEmail("user2@acme.com");
    glUser2.setState(GLState.ACTIVE);
    glUsers.add(glUser2);

    List<GLGroup> glGroups = new ArrayList<>();
    GLGroup glGroup = new GLGroup();
    glGroup.setPath("grp1");
    glGroup.setName("grp1");
    glGroups.add(glGroup);
    glGroup.addMember(glUser, GLPermission.DEVELOPER);

    List<GLProject> glProjects = new ArrayList<>();
    GLProject glProject = new GLProject();
    glProject.setPath("prj1");
    glProject.setName("Project 1");
    glProject.addMember(glUser2);
    glProjects.add(glProject);

    List<GLUser> glProjectUsers = new ArrayList<>();
    glProjectUsers.add(glUser2);

    when(client.blockUser(glUser2)).thenReturn(true);
    when(client.removeMemberFromProject(glProject, glUser2)).thenReturn(true);

    when(client.getUsers(null)).thenReturn(glUsers);
    when(client.getGroupsWithMembers(null, false)).thenReturn(glGroups);
    when(client.getProjectsFromGroup(glGroup, null)).thenReturn(glProjects);
    when(client.getProjectUsers(glProject)).thenReturn(glProjectUsers);

    IdpUser idpUser = new IdpUser();
    idpUser.setUsername("user1");
    idpUser.setFirstName("User");
    idpUser.setLastName("1");
    idpUser.setEmail("user1@acme.com");
    Map<String, List<String>> attributes = new HashMap<>();
    attributes.put(EXTERNAL_UID_ATTRIBUTE, Arrays.asList(EXTERNAL_UID));
    idpUser.setAttributes(attributes);

    IdpUser idpUser2 = new IdpUser();
    idpUser2.setUsername("user2");
    idpUser2.setFirstName("User");
    idpUser2.setLastName("2");
    idpUser2.setEmail("user2@acme.com");
    idpUser2.setAttributes(attributes);

    List<IdpGroup> idpGroups = new ArrayList<>();
    IdpGroup idpGroup = new IdpGroup();
    idpGroup.setName("APP_GIT_grp1_Developer");
    idpGroup.setPath("/APP_GIT_grp1_Developer");
    idpGroup.addMember(idpUser);
    idpGroups.add(idpGroup);

    app.sync(idpGroups);

    verify(client).getUsers(null);
    verify(client).getGroupsWithMembers(null, false);
    verify(client, never()).createGroup("grp1", "grp1", null);
    verify(client, never()).createUser("user1", "User 1", "user1@acme.com", null, null);
    verify(client, never()).addMemberToGroup(glGroup, glUser, GLPermission.DEVELOPER);
    verify(client, never()).removeMemberFromGroup(glGroup, glUser);
    verify(client).blockUser(glUser2);
    verify(client, never()).unblockUser(glUser);
    verify(client).getProjectsFromGroup(glGroup, null);
    verify(client).getProjectUsers(glProject);
    verify(client).removeMemberFromProject(glProject, glUser2);

    verify(eventRepository, times(2)).add(any(AuditEvent.class));
  }

  @Test
  public void testSyncUpdateUserAttributesForExistingUser() {
    List<GLUser> glUsers = new ArrayList<>();
    GLUser glUser = new GLUser();
    glUser.setId(GITLAB_ID);
    glUser.setUsername("user1");
    glUser.setName("User 1");
    glUser.setEmail("user1@acme.com");
    glUser.setState(GLState.ACTIVE);
    glUsers.add(glUser);

    List<GLGroup> glGroups = new ArrayList<>();
    GLGroup glGroup = new GLGroup();
    glGroup.setPath("grp1");
    glGroup.setName("grp1");
    glGroup.addMember(glUser, GLPermission.MAINTAINER);
    glGroups.add(glGroup);

    when(client.getUsers(null)).thenReturn(glUsers);
    when(client.getGroupsWithMembers(null, false)).thenReturn(glGroups);

    IdpUser idpUser = new IdpUser();
    idpUser.setUsername("user1");
    idpUser.setFirstName("User");
    idpUser.setLastName("1");
    idpUser.setEmail("user1@acme.com");
    Map<String, List<String>> attributes = new HashMap<>();
    attributes.put(EXTERNAL_UID_ATTRIBUTE, Arrays.asList(EXTERNAL_UID));
    idpUser.setAttributes(attributes);

    List<IdpGroup> idpGroups = new ArrayList<>();
    IdpGroup idpGroup = new IdpGroup();
    idpGroup.setName("APP_GIT_grp1_Maintainer");
    idpGroup.setPath("/APP_GIT_grp1_Maintainer");
    idpGroup.addMember(idpUser);
    idpGroups.add(idpGroup);

    app.sync(idpGroups);

    verify(client).getUsers(null);
    verify(client).getGroupsWithMembers(null, false);
    verify(client, never()).createGroup("grp1", "grp1", null);
    verify(client, never()).createUser("user1", "User 1", "user1@acme.com", PROVIDER_NAME, EXTERNAL_UID);
    verify(client, never()).addMemberToGroup(glGroup, glUser, GLPermission.MAINTAINER);
    verify(client, never()).removeMemberFromGroup(glGroup, null);
    verify(client, never()).blockUser(null);
    verify(client, never()).unblockUser(null);

    assertThat(idpUser.isAttributesModified()).isTrue();
    assertThat(idpUser.getAttribute(GitLab.USER_ID_ATTRIBUTE)).isEqualTo(GITLAB_ID);

    assertThat(idpUser.isAttributesModified()).isTrue();
    assertThat(idpUser.getAttribute(GitLab.USER_ID_ATTRIBUTE)).isEqualTo(GITLAB_ID);

    verify(eventRepository, never()).add(any(AuditEvent.class));
  }

  @Test
  public void testSyncCrateUserWithMultipleGroups() {
    List<GLUser> glUsers = new ArrayList<>();
    GLUser glUser = new GLUser();
    glUser.setId(GITLAB_ID);
    glUser.setUsername("user1");
    glUser.setName("User 1");
    glUser.setEmail("user1@acme.com");
    glUser.setState(GLState.ACTIVE);

    List<GLGroup> glGroups = new ArrayList<>();
    GLGroup glGroup1 = new GLGroup();
    glGroup1.setPath("grp1");
    glGroup1.setName("grp1");
    glGroups.add(glGroup1);
    GLGroup glGroup2 = new GLGroup();
    glGroup2.setPath("grp2");
    glGroup2.setName("grp2");
    glGroups.add(glGroup2);

    when(client.getUsers(null)).thenReturn(glUsers);
    when(client.getGroupsWithMembers(null, false)).thenReturn(glGroups);
    when(client.createUser("user1", "User 1", "user1@acme.com", PROVIDER_NAME, EXTERNAL_UID)).thenReturn(glUser);
    when(client.addMemberToGroup(glGroup1, glUser, GLPermission.DEVELOPER)).thenReturn(true);
    when(client.addMemberToGroup(glGroup2, glUser, GLPermission.MAINTAINER)).thenReturn(true);

    IdpUser idpUser = new IdpUser();
    idpUser.setUsername("user1");
    idpUser.setFirstName("User");
    idpUser.setLastName("1");
    idpUser.setEmail("user1@acme.com");
    Map<String, List<String>> attributes = new HashMap<>();
    attributes.put(EXTERNAL_UID_ATTRIBUTE, Arrays.asList(EXTERNAL_UID));
    idpUser.setAttributes(attributes);

    List<IdpGroup> idpGroups = new ArrayList<>();
    IdpGroup idpGroup1 = new IdpGroup();
    idpGroup1.setName("APP_GIT_grp1_Developer");
    idpGroup1.setPath("/APP_GIT_grp1_Developer");
    idpGroup1.addMember(idpUser);
    idpGroups.add(idpGroup1);
    IdpGroup idpGroup2 = new IdpGroup();
    idpGroup2.setName("APP_GIT_grp2_Maintainer");
    idpGroup2.setPath("/APP_GIT_grp2_Maintainer");
    idpGroup2.addMember(idpUser);
    idpGroups.add(idpGroup2);

    app.sync(idpGroups);

    verify(client).getUsers(null);
    verify(client).getGroupsWithMembers(null, false);
    verify(client, never()).createGroup("grp1", "grp1", null);
    verify(client, never()).createGroup("grp2", "grp2", null);
    verify(client).createUser("user1", "User 1", "user1@acme.com", PROVIDER_NAME, EXTERNAL_UID);
    verify(client).addMemberToGroup(glGroup1, glUser, GLPermission.DEVELOPER);
    verify(client).addMemberToGroup(glGroup2, glUser, GLPermission.MAINTAINER);
    verify(client, never()).blockUser(glUser);
    verify(client, never()).unblockUser(glUser);

    assertThat(idpUser.isAttributesModified()).isTrue();
    assertThat(idpUser.getAttribute(GitLab.USER_ID_ATTRIBUTE)).isEqualTo(GITLAB_ID);

    verify(eventRepository, times(3)).add(any(AuditEvent.class));
  }
}
