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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.vaulttec.idm.sync.idp.IdpGroup;
import org.vaulttec.idm.sync.idp.IdpUser;

@RunWith(MockitoJUnitRunner.class)
public class GitLabTest {

  private static final String PROVIDER_NAME = "ldapmain";
  private GitLabClient client;
  private GitLab app;

  @Before
  public void setUp() throws Exception {
    client = mock(GitLabClient.class);
    app = new GitLabBuilder(client).groupRegExp("APP_GIT_(?<groupPath>\\w*)_(?<permission>\\w*)")
        .removeProjectMembers(true).providerName(PROVIDER_NAME).providerUidAttribute("LDAP_ENTRY_DN").build();
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
    verify(client).getGroupsWithMembers(null);
    verify(client, never()).createUser(null, null, null, null, null);
    verify(client).createGroup("grp1", "grp1", null);
    verify(client, never()).addMemberToGroup(glGroup, null, null);
    verify(client, never()).removeMemberFromGroup(null, null);
    verify(client, never()).blockUser(null);
    verify(client, never()).unblockUser(null);
  }

  @Test
  public void testSyncCreateNewGroupAndUser() {
    GLUser glUser = new GLUser();
    glUser.setUsername("user1");
    glUser.setName("User 1");
    glUser.setEmail("user1@acme.com");
    glUser.setState(GLState.ACTIVE);

    GLGroup glGroup = new GLGroup();
    glGroup.setPath("grp1");
    glGroup.setName("grp1");

    when(client.getUsers(null)).thenReturn(new ArrayList<>());
    when(client.createUser("user1", "User 1", "user1@acme.com", PROVIDER_NAME, null)).thenReturn(glUser);
    when(client.getGroupsWithMembers(null)).thenReturn(new ArrayList<>());
    when(client.createGroup("grp1", "grp1", null)).thenReturn(glGroup);

    IdpUser idpUser = new IdpUser();
    idpUser.setUsername("user1");
    idpUser.setFirstName("User");
    idpUser.setLastName("1");
    idpUser.setEmail("user1@acme.com");

    List<IdpGroup> idpGroups = new ArrayList<>();
    IdpGroup idpGroup = new IdpGroup();
    idpGroup.setName("APP_GIT_grp1_Maintainer");
    idpGroup.setPath("/APP_GIT_grp1_Maintainer");
    idpGroup.addMember(idpUser);
    idpGroups.add(idpGroup);

    app.sync(idpGroups);

    verify(client).getUsers(null);
    verify(client).getGroupsWithMembers(null);
    verify(client).createGroup("grp1", "grp1", null);
    verify(client).createUser("user1", "User 1", "user1@acme.com", PROVIDER_NAME, null);
    verify(client).addMemberToGroup(glGroup, glUser, GLPermission.MAINTAINER);
    verify(client, never()).removeMemberFromGroup(glGroup, null);
    verify(client, never()).blockUser(null);
    verify(client, never()).unblockUser(null);
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
    when(client.getGroupsWithMembers(null)).thenReturn(glGroups);
    when(client.createUser("user2", "User 2", "user2@acme.com", PROVIDER_NAME, null)).thenReturn(glUser2);

    IdpUser idpUser = new IdpUser();
    idpUser.setUsername("user1");
    idpUser.setFirstName("User");
    idpUser.setLastName("1");
    idpUser.setEmail("user1@acme.com");
    IdpUser idpUser2 = new IdpUser();
    idpUser2.setUsername("user2");
    idpUser2.setFirstName("User");
    idpUser2.setLastName("2");
    idpUser2.setEmail("user2@acme.com");

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
    verify(client).getGroupsWithMembers(null);
    verify(client, never()).createGroup("grp1", "grp1", null);
    verify(client, never()).createUser("user1", "User 1", "user1@acme.com", PROVIDER_NAME, null);
    verify(client).createUser("user2", "User 2", "user2@acme.com", PROVIDER_NAME, null);
    verify(client, never()).addMemberToGroup(glGroup, glUser, GLPermission.MAINTAINER);
    verify(client).addMemberToGroup(glGroup, glUser2, GLPermission.DEVELOPER);
    verify(client, never()).removeMemberFromGroup(glGroup, null);
    verify(client, never()).blockUser(null);
    verify(client, never()).unblockUser(null);
  }

  @Test
  public void testSyncCreateNewUserWithoutEmail() {
    GLUser glUser = new GLUser();
    glUser.setUsername("user1");
    glUser.setName("User 1");
    glUser.setEmail("user1" + GitLab.DUMMY_EMAIL_DOMAIN); // dummy email
    glUser.setState(GLState.ACTIVE);

    List<GLGroup> glGroups = new ArrayList<>();
    GLGroup glGroup = new GLGroup();
    glGroup.setPath("grp1");
    glGroup.setName("grp1");
    glGroups.add(glGroup);

    when(client.getUsers(null)).thenReturn(new ArrayList<>());
    when(client.getGroupsWithMembers(null)).thenReturn(glGroups);
    when(client.createUser("user1", "User 1", "user1" + GitLab.DUMMY_EMAIL_DOMAIN, PROVIDER_NAME, null))
        .thenReturn(glUser);

    IdpUser idpUser = new IdpUser();
    idpUser.setUsername("user1");
    idpUser.setFirstName("User");
    idpUser.setLastName("1");
    idpUser.setEmail(null); // missing email

    List<IdpGroup> idpGroups = new ArrayList<>();
    IdpGroup idpGroup = new IdpGroup();
    idpGroup.setName("APP_GIT_grp1_Maintainer");
    idpGroup.setPath("/APP_GIT_grp1_Maintainer");
    idpGroup.addMember(idpUser);
    idpGroups.add(idpGroup);

    app.sync(idpGroups);

    verify(client).getUsers(null);
    verify(client).getGroupsWithMembers(null);
    verify(client, never()).createGroup("grp1", "grp1", null);
    verify(client).createUser("user1", "User 1", "user1" + GitLab.DUMMY_EMAIL_DOMAIN, PROVIDER_NAME, null);
    verify(client).addMemberToGroup(glGroup, glUser, GLPermission.MAINTAINER);
    verify(client, never()).removeMemberFromGroup(glGroup, null);
    verify(client, never()).blockUser(null);
    verify(client, never()).unblockUser(null);
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
    when(client.getGroupsWithMembers(null)).thenReturn(glGroups);

    List<IdpGroup> idpGroups = new ArrayList<>();
    IdpGroup idpGroup = new IdpGroup();
    idpGroup.setName("APP_GIT_grp1_Maintainer");
    idpGroup.setPath("/APP_GIT_grp1_Maintainer");
    idpGroups.add(idpGroup);

    app.sync(idpGroups);

    verify(client).getUsers(null);
    verify(client).getGroupsWithMembers(null);
    verify(client, never()).createGroup("grp1", "grp1", null);
    verify(client, never()).createUser("user1", "User 1", "user1@acme.com", null, null);
    verify(client, never()).addMemberToGroup(glGroup, glUser, GLPermission.MAINTAINER);
    verify(client).removeMemberFromGroup(glGroup, glUser);
    verify(client).blockUser(glUser);
    verify(client, never()).unblockUser(glUser);
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
    when(client.getGroupsWithMembers(null)).thenReturn(glGroups);

    List<IdpGroup> idpGroups = new ArrayList<>();

    app.sync(idpGroups);

    verify(client).getUsers(null);
    verify(client).getGroupsWithMembers(null);
    verify(client, never()).createGroup("grp1", "grp1", null);
    verify(client, never()).createUser("user1", "User 1", "user1@acme.com", null, null);
    verify(client, never()).addMemberToGroup(glGroup, glUser, GLPermission.MAINTAINER);
    verify(client).removeMemberFromGroup(glGroup, glUser);
    verify(client).blockUser(glUser);
    verify(client, never()).unblockUser(glUser);
  }

  @Test
  public void testSyncGroupWithBlockedUsers() {
    List<GLUser> glUsers = new ArrayList<>();
    GLUser glUser = new GLUser();
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
    when(client.getGroupsWithMembers(null)).thenReturn(glGroups);

    IdpUser idpUser = new IdpUser();
    idpUser.setUsername("user1");
    idpUser.setFirstName("User");
    idpUser.setLastName("1");
    idpUser.setEmail("user1@acme.com");

    List<IdpGroup> idpGroups = new ArrayList<>();
    IdpGroup idpGroup = new IdpGroup();
    idpGroup.setName("APP_GIT_grp1_Maintainer");
    idpGroup.setPath("/APP_GIT_grp1_Maintainer");
    idpGroup.addMember(idpUser);
    idpGroups.add(idpGroup);

    app.sync(idpGroups);

    verify(client).getUsers(null);
    verify(client).getGroupsWithMembers(null);
    verify(client, never()).createGroup("grp1", "grp1", null);
    verify(client, never()).createUser("user1", "User 1", "user1@acme.com", null, null);
    verify(client).addMemberToGroup(glGroup, glUser, GLPermission.MAINTAINER);
    verify(client).removeMemberFromGroup(glGroup, glUser2);
    verify(client, never()).blockUser(glUser);
    verify(client).unblockUser(glUser);
  }

  @Test
  public void testSyncGroupWithExistingBlockedUser() {
    List<GLUser> glUsers = new ArrayList<>();
    GLUser glUser = new GLUser();
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
    when(client.getGroupsWithMembers(null)).thenReturn(glGroups);

    IdpUser idpUser = new IdpUser();
    idpUser.setUsername("user1");
    idpUser.setFirstName("User");
    idpUser.setLastName("1");
    idpUser.setEmail("user1@acme.com");

    List<IdpGroup> idpGroups = new ArrayList<>();
    IdpGroup idpGroup = new IdpGroup();
    idpGroup.setName("APP_GIT_grp1_Maintainer");
    idpGroup.setPath("/APP_GIT_grp1_Maintainer");
    idpGroup.addMember(idpUser);
    idpGroups.add(idpGroup);

    app.sync(idpGroups);

    verify(client).getUsers(null);
    verify(client).getGroupsWithMembers(null);
    verify(client, never()).createGroup("grp1", "grp1", null);
    verify(client, never()).createUser("user1", "User 1", "user1@acme.com", null, null);
    verify(client).addMemberToGroup(glGroup, glUser, GLPermission.MAINTAINER);
    verify(client, never()).removeMemberFromGroup(glGroup, glUser);
    verify(client, never()).blockUser(glUser);
    verify(client).unblockUser(glUser);
  }

  @Test
  public void testSyncUserWithMultiplePermissions() {
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
    glGroup.addMember(glUser, GLPermission.REPORTER);

    when(client.getUsers(null)).thenReturn(glUsers);
    when(client.getGroupsWithMembers(null)).thenReturn(glGroups);

    IdpUser idpUser = new IdpUser();
    idpUser.setUsername("user1");
    idpUser.setFirstName("User");
    idpUser.setLastName("1");
    idpUser.setEmail("user1@acme.com");

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
    verify(client).getGroupsWithMembers(null);
    verify(client, never()).createGroup("grp1", "grp1", null);
    verify(client, never()).createUser("user1", "User 1", "user1@acme.com", null, null);
    verify(client).addMemberToGroup(glGroup, glUser, GLPermission.MAINTAINER);
    verify(client).removeMemberFromGroup(glGroup, glUser);
    verify(client, never()).blockUser(glUser);
    verify(client, never()).unblockUser(glUser);
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

    when(client.getUsers(null)).thenReturn(glUsers);
    when(client.getGroupsWithMembers(null)).thenReturn(glGroups);
    when(client.getProjectsFromGroup(glGroup, null)).thenReturn(glProjects);
    when(client.getProjectUsers(glProject)).thenReturn(glProjectUsers);

    IdpUser idpUser = new IdpUser();
    idpUser.setUsername("user1");
    idpUser.setFirstName("User");
    idpUser.setLastName("1");
    idpUser.setEmail("user1@acme.com");

    IdpUser idpUser2 = new IdpUser();
    idpUser2.setUsername("user2");
    idpUser2.setFirstName("User");
    idpUser2.setLastName("2");
    idpUser2.setEmail("user2@acme.com");

    List<IdpGroup> idpGroups = new ArrayList<>();
    IdpGroup idpGroup = new IdpGroup();
    idpGroup.setName("APP_GIT_grp1_Developer");
    idpGroup.setPath("/APP_GIT_grp1_Developer");
    idpGroup.addMember(idpUser);
    idpGroups.add(idpGroup);

    app.sync(idpGroups);

    verify(client).getUsers(null);
    verify(client).getGroupsWithMembers(null);
    verify(client, never()).createGroup("grp1", "grp1", null);
    verify(client, never()).createUser("user1", "User 1", "user1@acme.com", null, null);
    verify(client, never()).addMemberToGroup(glGroup, glUser, GLPermission.DEVELOPER);
    verify(client, never()).removeMemberFromGroup(glGroup, glUser);
    verify(client, never()).blockUser(glUser);
    verify(client, never()).unblockUser(glUser);
    verify(client).getProjectsFromGroup(glGroup, null);
    verify(client).getProjectUsers(glProject);
    verify(client).removeMemberFromProject(glProject, glUser2);
  }
}