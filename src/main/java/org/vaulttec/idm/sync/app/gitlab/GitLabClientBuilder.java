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

public final class GitLabClientBuilder {

  private final String serverUrl;
  private int perPage;
  private String privateToken;
  private String proxyHost;
  private int proxyPort;

  public GitLabClientBuilder(String serverUrl) {
    this.serverUrl = serverUrl;
  }

  public GitLabClientBuilder perPage(int perPage) {
    this.perPage = perPage;
    return this;
  }

  /**
   * Sets <a href=
   * "https://docs.gitlab.com/ce/user/profile/personal_access_tokens.html">personal
   * access token</a> of a user allowed to manage all groups and users.
   */
  public GitLabClientBuilder privateToken(String privateToken) {
    this.privateToken = privateToken;
    return this;
  }

  public GitLabClientBuilder proxyHost(String proxyHost) {
    this.proxyHost = proxyHost;
    return this;
  }

  public GitLabClientBuilder proxyPort(int proxyPort) {
    this.proxyPort = proxyPort;
    return this;
  }

  public GitLabClient build() {
    if (serverUrl == null) {
      throw new IllegalStateException("serverUrl required");
    }
    if (perPage == 0) {
      perPage = 100;
    }
    if (privateToken == null) {
      throw new IllegalStateException("privateToken required");
    }
    if (proxyHost != null && proxyPort == 0) {
      throw new IllegalStateException("proxyPort required");
    }
    return new GitLabClient(serverUrl, perPage, privateToken, proxyHost, proxyPort);
  }
}