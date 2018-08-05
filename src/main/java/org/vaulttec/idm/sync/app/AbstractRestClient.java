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

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

public abstract class AbstractRestClient extends AbstractClient {

  protected final RestTemplate restTemplate;
  protected HttpEntity<String> authenticationEntity;

  public AbstractRestClient(String serverUrl, int perPage, String proxyHost, int proxyPort) {
    super(serverUrl, perPage);
    this.restTemplate = createRestTemplate(proxyHost, proxyPort);
  }

  protected RestTemplate createRestTemplate(String proxyHost, int proxyPort) {
    if (proxyHost != null) {
      SimpleClientHttpRequestFactory clientHttpReq = new SimpleClientHttpRequestFactory();
      Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
      clientHttpReq.setProxy(proxy);
      return new RestTemplate(clientHttpReq);
    }
    return new RestTemplate();
  }

  protected Map<String, String> createUriVariables(String... variables) {
    if (variables.length % 2 != 0) {
      throw new IllegalStateException("Key-value required - uneven number of arguments");
    }
    Map<String, String> uriVariables = new HashMap<>();
    for (int i = 0; i < variables.length; i += 2) {
      uriVariables.put(variables[i], variables[i + 1]);
    }
    return uriVariables;
  }

  protected void prepareAuthenticationEntity(String headerName, String headerValue) {
    HttpHeaders headers = new HttpHeaders();
    headers.set(headerName, headerValue);
    authenticationEntity = new HttpEntity<String>(headers);
  }
}
