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
package org.vaulttec.idm.sync.app;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.vaulttec.idm.sync.AbstractSyncEventPublisher;

public abstract class AbstractApplication extends AbstractSyncEventPublisher implements Application {

  private final String groupSearch;
  private final Pattern groupPattern;

  public AbstractApplication(AuditEventRepository eventRepository, String groupSearch, String groupRegExp) {
    super(eventRepository);
    this.groupSearch = groupSearch;
    this.groupPattern = Pattern.compile(groupRegExp);
  }

  @Override
  public String getGroupSearch() {
    return groupSearch;
  }

  protected Matcher getGroupNameMatcher(String groupName) {
    Matcher m = groupPattern.matcher(groupName);
    return m.find() ? m : null;
  }
}