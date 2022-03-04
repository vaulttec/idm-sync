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
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException.TooManyRequests;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

public abstract class AbstractRestClient extends AbstractClient {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractRestClient.class);

  protected final RestTemplate restTemplate;
  protected HttpEntity<String> authenticationEntity;
  protected int retryWaitSeconds;

  public AbstractRestClient(String serverUrl, int perPage, int retryWaitSeconds, String proxyHost, int proxyPort) {
    super(serverUrl, perPage);
    this.retryWaitSeconds = retryWaitSeconds;
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

  protected String getApiUrl(String apiCall) {
    return serverUrl + getApiPath() + apiCall;
  }

  protected abstract String getApiPath();

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
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set(headerName, headerValue);
    authenticationEntity = new HttpEntity<String>(headers);
  }

  protected <T> T makeReadApiCall(String apiCall, ParameterizedTypeReference<T> typeReference,
      Map<String, String> uriVariables) {
    String url = getApiUrl(apiCall);
    for (int retries = 1; retries >= 0; retries--) {
      try {
        ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.GET, authenticationEntity, typeReference,
            uriVariables);
        return response.getBody();
      } catch (TooManyRequests e) {

        // API rate limit exceeded: we have to wait and retry
        sleep(retryWaitSeconds);
      } catch (Exception e) {
        logException(HttpMethod.GET, uriVariables, url, e);
        return null;
      }
    }
    return null;
  }

  protected <T> List<T> makeReadListApiCall(String apiCall, ParameterizedTypeReference<List<T>> typeReference,
      Map<String, String> uriVariables) {
    String url = getApiUrl(apiCall);
    for (int retries = 1; retries >= 0; retries--) {
      try {
        ResponseEntity<List<T>> response = restTemplate.exchange(url, HttpMethod.GET, authenticationEntity,
            typeReference, uriVariables);
        return response.getBody();
      } catch (TooManyRequests e) {

        // API rate limit exceeded: we have to wait and retry
        sleep(retryWaitSeconds);
      } catch (Exception e) {
        logException(HttpMethod.GET, uriVariables, url, e);
        return null;
      }
    }
    return null;
  }

  protected boolean makeWriteApiCall(String apiCall, HttpMethod method, Map<String, String> uriVariables) {
    String url = getApiUrl(apiCall);
    for (int retries = 1; retries >= 0; retries--) {
      try {
        restTemplate.exchange(url, method, authenticationEntity, Void.class, uriVariables);
        return true;
      } catch (TooManyRequests e) {

        // API rate limit exceeded: we have to wait and retry
        sleep(retryWaitSeconds);
      } catch (Exception e) {
        logException(method, uriVariables, url, e);
        return false;
      }
    }
    return false;
  }

  protected <T> T makeWriteApiCall(String apiCall, Class<T> type, Map<String, String> uriVariables) {
    String url = getApiUrl(apiCall);
    for (int retries = 1; retries >= 0; retries--) {
      try {
        return restTemplate.postForObject(url, authenticationEntity, type, uriVariables);
      } catch (TooManyRequests e) {

        // API rate limit exceeded: we have to wait and retry
        sleep(retryWaitSeconds);
      } catch (Exception e) {
        logException(HttpMethod.POST, uriVariables, url, e);
        return null;
      }
    }
    return null;
  }

  protected boolean makeWriteApiCall(String apiCall, HttpMethod method, HttpEntity<String> entity,
      Map<String, String> uriVariables) {
    String url = getApiUrl(apiCall);
    for (int retries = 1; retries >= 0; retries--) {
      try {
        restTemplate.exchange(url, method, entity, Void.class, uriVariables);
        return true;
      } catch (TooManyRequests e) {

        // API rate limit exceeded: we have to wait and retry
        sleep(retryWaitSeconds);
      } catch (Exception e) {
        logException(method, uriVariables, url, e);
        return false;
      }
    }
    return false;
  }

  protected <T> T makeWriteApiCall(String apiCall, HttpEntity<String> entity, Class<T> type,
      Map<String, String> uriVariables) {
    String url = getApiUrl(apiCall);
    for (int retries = 1; retries >= 0; retries--) {
      try {
        return restTemplate.postForObject(url, entity, type, uriVariables);
      } catch (TooManyRequests e) {

        // API rate limit exceeded: we have to wait and retry
        sleep(retryWaitSeconds);
      } catch (Exception e) {
        logException(HttpMethod.POST, uriVariables, url, e);
        return null;
      }
    }
    return null;
  }

  protected <T> T makeWriteApiCall(String apiCall, HttpEntity<String> entity, Class<T> type) {
    String url = getApiUrl(apiCall);
    for (int retries = 1; retries >= 0; retries--) {
      try {
        return restTemplate.postForObject(url, entity, type);
      } catch (TooManyRequests e) {

        // API rate limit exceeded: we have to wait and retry
        sleep(retryWaitSeconds);
      } catch (Exception e) {
        logException(HttpMethod.POST, null, url, e);
        return null;
      }
    }
    return null;
  }

  protected void checkRateLimitRemaining(String rateLimitRemainingValue, String rateLimitResetInSeconds) {
    int rateLimitWaitSeconds;
    try {
      rateLimitWaitSeconds = Integer.parseInt(rateLimitResetInSeconds);
    } catch (NumberFormatException e) {
      rateLimitWaitSeconds = retryWaitSeconds;  // default wait time
    }
    try {
      if (Integer.parseInt(rateLimitRemainingValue) < perPage) {
        sleep(rateLimitWaitSeconds);
      }
    } catch (NumberFormatException e) {
      // Ignore
    }
  }

  protected void sleep(int sleepSeconds) {
    LOG.debug("Sleeping {} seconds due to rate limit");
    try {
      Thread.sleep(sleepSeconds * 1000);
    } catch (InterruptedException e1) {
      // Ignore
    }
  }

  private void logException(HttpMethod method, Map<String, String> uriVariables, String url, Exception e) {
    if (e instanceof RestClientResponseException) {
      LOG.error("API call {} '{}' {} failed with {}: {}", method.name(), url, uriVariables != null ? uriVariables : "",
          e.getMessage(), ((RestClientResponseException) e).getResponseBodyAsString());
    } else if (e instanceof RestClientException) {
      LOG.error("API call {} '{}' {} failed with {}", method.name(), url, uriVariables != null ? uriVariables : "",
          e.getMessage());
    } else {
      LOG.error("API call {} '{}' {} failed", method.name(), url, uriVariables != null ? uriVariables : "", e);
    }
  }
}
