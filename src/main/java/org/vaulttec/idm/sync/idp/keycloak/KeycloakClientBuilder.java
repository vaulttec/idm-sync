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
package org.vaulttec.idm.sync.idp.keycloak;

public final class KeycloakClientBuilder {

  private final String serverUrl;
  private String realm;
  private String clientId;
  private String clientSecret;
  private int perPage;
  private String proxyHost;
  private int proxyPort;

  public KeycloakClientBuilder(String serverUrl) {
    this.serverUrl = serverUrl;
  }

  public KeycloakClientBuilder perPage(int perPage) {
    this.perPage = perPage;
    return this;
  }

  public KeycloakClientBuilder realm(String realm) {
    this.realm = realm;
    return this;
  }

  /**
   * Sets id of <a href=
   * "https://www.keycloak.org/docs/latest/server_admin/index.html#oidc-clients">OpenID
   * Connect client</a> with access type <code>confidential</code>.
   */
  public KeycloakClientBuilder clientId(String clientId) {
    this.clientId = clientId;
    return this;
  }

  /**
   * Sets <a href=
   * "https://www.keycloak.org/docs/latest/server_admin/index.html#_client-credentials">secret</a>
   * of Client's <a href=
   * "https://www.keycloak.org/docs/latest/server_admin/index.html#_service_accounts">service
   * account</a>.
   */
  public KeycloakClientBuilder clientSecret(String clientSecret) {
    this.clientSecret = clientSecret;
    return this;
  }

  public KeycloakClientBuilder proxyHost(String proxyHost) {
    this.proxyHost = proxyHost;
    return this;
  }

  public KeycloakClientBuilder proxyPort(int proxyPort) {
    this.proxyPort = proxyPort;
    return this;
  }

  public KeycloakClient build() {
    if (serverUrl == null) {
      throw new IllegalStateException("serverUrl required");
    }
    if (perPage == 0) {
      perPage = 100;
    }
    if (realm == null) {
      throw new IllegalStateException("realm required");
    }
    if (clientId == null) {
      throw new IllegalStateException("clientId required");
    }
    if (clientSecret == null) {
      throw new IllegalStateException("clientSecret required");
    }
    if (proxyHost != null && proxyPort == 0) {
      throw new IllegalStateException("proxyPort required");
    }
    return new KeycloakClient(serverUrl, perPage, realm, clientId, clientSecret, proxyHost, proxyPort);
  }
}