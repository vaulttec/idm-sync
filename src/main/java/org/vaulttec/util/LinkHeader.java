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
package org.vaulttec.util;

import org.springframework.http.HttpHeaders;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LinkHeader {
  private final Map<Rel, Link> links;

  protected LinkHeader(Map<Rel, Link> links) {
    this.links = links;
  }

  public boolean hasLink(Rel rel) {
    return links.containsKey(rel);
  }

  public Link getLink(Rel rel) {
    return links.get(rel);
  }

  public static LinkHeader parse(HttpHeaders headers, String pageParameterName, String perPageParameterName ) {
    String linkValue = headers.getFirst(HttpHeaders.LINK);
    if (StringUtils.hasText(linkValue)) {
      String[] linkValues = linkValue.split(",");
      if (linkValues.length > 0) {
        Map<Rel, Link> links = new HashMap<>();
        for (String value : linkValues) {
            Link link = Link.fromSource(value, pageParameterName, perPageParameterName);
            if (link != null && link.rel() != null) {
                links.put(link.rel(), link);
            }
        }
        return new LinkHeader(links);
      }

    }
    return null;
  }

  public enum Rel {
    FIRST, PREV, NEXT, LAST;

    public static Rel fromSource(String source) {
      if (source != null) {
        for (Rel rel : Rel.values()) {
          String pattern = "rel=\"" + rel.name().toLowerCase() + "\"";
          if (source.contains(pattern)) {
            return rel;
          }
        }
      }
      return null;
    }
  }

  public record Link(URI resourceUri, int page, int perPage, Rel rel) {
    private static final Pattern QUERY_PATTERN = Pattern.compile("([^&=]+)=?([^&]+)?");

    public static Link fromSource(String source, String pageParameterName, String perPageParameterName) {
        if (ObjectUtils.isEmpty(source)) {
          return null;
        }
        URI resourceUri = getResourceUri(source);
        Rel rel = Rel.fromSource(source);
        int page = getQueryParameterValue(resourceUri, pageParameterName);
        int perPage = getQueryParameterValue(resourceUri, perPageParameterName);
        return new Link(resourceUri, page, perPage, rel);
      }

      private static URI getResourceUri(String source) {
        int startIndex = source.indexOf("<");
        int endIndex = source.indexOf(">");
        if (startIndex < 0 || endIndex < 0) {
          return null;
        } else {
          String resourceString = source.substring(startIndex, endIndex + 1);
          try {
            return new URI(resourceString.substring(1, resourceString.length() - 1));
          } catch (URISyntaxException e) {
            throw new IllegalStateException("", e);
          }
        }
      }

      private static int getQueryParameterValue(URI resourceUri, String paramName) {
        if (resourceUri == null) {
          return -1;
        }
        String query = resourceUri.getQuery();
        if (query == null)
          return -1;
        Matcher matcher = QUERY_PATTERN.matcher(query);
        while (matcher.find()) {
          String name = matcher.group(1);
          String value = matcher.group(2);
          if (name.equals(paramName)) {
            if (ObjectUtils.isEmpty(value)) {
              return -1;
            } else {
              return Integer.parseInt(value);
            }
          }
        }
        return -1;
      }
    }
}
