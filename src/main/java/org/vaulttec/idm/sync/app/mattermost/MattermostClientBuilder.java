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

public final class MattermostClientBuilder {

  private final String serverUrl;
  private int perPage;
  private int retryWaitSeconds;
  private String personalAccessToken;
  private String proxyHost;
  private int proxyPort;

  public MattermostClientBuilder(String serverUrl) {
    this.serverUrl = serverUrl;
  }

  public MattermostClientBuilder perPage(int perPage) {
    this.perPage = perPage;
    return this;
  }

  public MattermostClientBuilder retryWaitSeconds(int retryWaitSeconds) {
    this.retryWaitSeconds = retryWaitSeconds;
    return this;
  }

  /**
   * Sets <a href=
   * "https://docs.mattermost.com/developer/personal-access-tokens.html">personal
   * access token</a> of a user allowed to manage all teams and users.
   */
  public MattermostClientBuilder personalAccessToken(String personalAccessToken) {
    this.personalAccessToken = personalAccessToken;
    return this;
  }

  public MattermostClientBuilder proxyHost(String proxyHost) {
    this.proxyHost = proxyHost;
    return this;
  }

  public MattermostClientBuilder proxyPort(int proxyPort) {
    this.proxyPort = proxyPort;
    return this;
  }

  public MattermostClient build() {
    if (serverUrl == null) {
      throw new IllegalStateException("serverUrl required");
    }
    if (perPage == 0) {
      perPage = 100;
    }
    if (retryWaitSeconds == 0) {
      retryWaitSeconds = 1;
    }
    if (personalAccessToken == null) {
      throw new IllegalStateException("personalAccessToken required");
    }
    if (proxyHost != null && proxyPort == 0) {
      throw new IllegalStateException("proxyPort required");
    }
    return new MattermostClient(serverUrl, perPage, retryWaitSeconds, personalAccessToken, proxyHost, proxyPort);
  }
}