/*
 * IDM Syncronizer
 * Copyright (c) 2019 Torsten Juergeleit
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
package org.vaulttec.idm.sync.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.vaulttec.idm.sync.app.Application;
import org.vaulttec.idm.sync.app.model.AppApplication;
import org.vaulttec.idm.sync.app.model.AppOrganization;
import org.vaulttec.idm.sync.app.model.AppStatistics;
import org.vaulttec.idm.sync.app.model.AppUser;
import org.vaulttec.idm.sync.idp.IdentityProvider;
import org.vaulttec.idm.sync.idp.IdpGroup;
import org.vaulttec.idm.sync.idp.IdpGroupRepresentation;
import org.vaulttec.idm.sync.idp.IdpUser;

@RestController
@RequestMapping("/api")
public class ApiController {

  private static final Logger LOG = LoggerFactory.getLogger(ApiController.class);

  private final IdentityProvider idp;
  private final List<Application> applications;

  ApiController(IdentityProvider idp, List<Application> applications) {
    this.idp = idp;
    this.applications = applications;
  }

  @GetMapping("/applications")
  public @ResponseBody List<AppApplication> applications() {
    return applications.stream().map(a -> new AppApplication(a.getId(), a.getName())).collect(Collectors.toList());
  }

  @GetMapping("/{appId}/organizations")
  public @ResponseBody Collection<AppOrganization> getOrganizations(@PathVariable("appId") String appId,
      @RequestParam(name = "search", required = false) String search) {
    Application application = getApplication(appId);
    LOG.debug("Getting groups for application '{}'", application.getName());
    if (idp.authenticate()) {
      return getOrganisations(application, search).values();
    }
    return null;
  }

  private Application getApplication(String appId) {
    Application application = applications.stream().filter(app -> app.getId().equals(appId)).findFirst().orElse(null);
    if (application == null) {
      throw new IllegalArgumentException("Unkown application id '" + appId + "'");
    }
    return application;
  }

  private Map<String, AppOrganization> getOrganisations(Application application, String search) {
    Map<String, AppOrganization> organizations = new HashMap<String, AppOrganization>();
    List<IdpGroup> groups = idp.getGroups(application.getGroupSearch());
    groups.forEach(g -> {
      IdpGroupRepresentation groupRepresentation = application.getGroupRepresentation(g);
      if (search == null || groupRepresentation.getName().contains(search)) {
        AppOrganization organization = organizations.get(groupRepresentation.getName());
        if (organization == null) {
          organization = new AppOrganization(groupRepresentation.getName());
          organizations.put(organization.getName(), organization);
        }
        organization.addRole(groupRepresentation.getRole());
      }
    });
    return organizations;
  }

  @GetMapping("/{appId}/organizations/{orgName}/members")
  public @ResponseBody Collection<AppUser> getOrganizationMembers(@PathVariable("appId") String appId,
      @PathVariable("orgName") String orgName) {
    Application application = getApplication(appId);
    LOG.debug("Getting members of organization '{}' of application '{}'", orgName, application.getName());
    if (idp.authenticate()) {
      return getOrganisationMembers(application, orgName).values();
    }
    return null;
  }

  private Map<String, AppUser> getOrganisationMembers(Application application, String orgName) {
    List<IdpGroup> groups = idp.getGroups(application.getGroupSearch());
    List<IdpGroup> orgGroups = new ArrayList<IdpGroup>();
    groups.forEach(g -> {
      IdpGroupRepresentation groupRepresentation = application.getGroupRepresentation(g);
      if (orgName.equals(groupRepresentation.getName())) {
        orgGroups.add(g);
      }
    });
    return getMembers(application, orgGroups);
  }

  private Map<String, AppUser> getMembers(Application application, List<IdpGroup> groups) {
    Map<String, AppUser> users = new HashMap<String, AppUser>();
    for (IdpGroup group : groups) {
      IdpGroupRepresentation groupRepresentation = application.getGroupRepresentation(group);
      List<IdpUser> members = idp.getGroupMembers(group);
      if (members != null) {
        for (IdpUser member : members) {
          if (!users.containsKey(member.getUsername())) {
            users.put(member.getUsername(), new AppUser(member.getUsername(), member.getName()));
          }
          AppUser user = users.get(member.getUsername());
          user.addOrganization(new AppOrganization(groupRepresentation.getName(), groupRepresentation.getRole()));
        }
      }
    }
    return users;
  }

  @GetMapping(path = "/{appId}/statistics", produces = { MediaType.APPLICATION_JSON_VALUE,
      ApiConfig.MEDIA_TYPE_CSV_VALUE })
  public @ResponseBody List<AppStatistics> getOrganizationsStatistics(@PathVariable("appId") String appId) {
    Application application = getApplication(appId);
    LOG.debug("Getting statistics for application '{}'", application.getName());
    if (idp.authenticate()) {
      return application.getStatistics();
    }
    return null;
  }

  @GetMapping("/{appId}/users")
  public @ResponseBody Collection<AppUser> getUsers(@PathVariable("appId") String appId,
      @RequestParam(name = "search", required = false) String search) {
    Application application = getApplication(appId);
    LOG.debug("Getting users for application '{}'", application.getName());
    if (idp.authenticate()) {
      return getUsers(application, search).values();
    }
    return null;
  }

  private Map<String, AppUser> getUsers(Application application, String search) {
    Map<String, AppUser> users = new HashMap<String, AppUser>();
    List<IdpGroup> groups = idp.getGroups(application.getGroupSearch());
    for (IdpGroup group : groups) {
      IdpGroupRepresentation groupRepresentation = application.getGroupRepresentation(group);
      List<IdpUser> members = idp.getGroupMembers(group);
      if (members != null) {
        for (IdpUser member : members) {
          if (search == null || member.getUsername().contains(search)) {
            if (!users.containsKey(member.getUsername())) {
              users.put(member.getUsername(), new AppUser(member.getUsername(), member.getName()));
            }
            AppUser user = users.get(member.getUsername());
            user.addOrganization(new AppOrganization(groupRepresentation.getName(), groupRepresentation.getRole()));
          }
        }
      }
    }
    return users;
  }
}
