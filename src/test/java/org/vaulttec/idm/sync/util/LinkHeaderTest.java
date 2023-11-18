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
package org.vaulttec.idm.sync.util;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.vaulttec.util.LinkHeader;
import org.vaulttec.util.LinkHeader.Link;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LinkHeaderTest {

  @Test
  void testParse() {
    String linkValue = "</resource?page=1&per_page=100>; rel=\"first\", "
        + "</resource?page=3&per_page=100>; rel=\"prev\", " //
        + "</resource?page=5&per_page=100>; rel=\"next\", " //
        + "</resource?page=50&per_page=100>; rel=\"last\"";
    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.LINK, linkValue);

    LinkHeader linkHeader = LinkHeader.parse(headers, "page", "per_page");
    assertNotNull(linkHeader);

    Link link = linkHeader.getLink(LinkHeader.Rel.FIRST);
    assertNotNull(link);
    assertEquals("/resource?page=1&per_page=100", link.resourceUri().toString());
    assertEquals(1, link.page());
    assertEquals(100, link.perPage());

    link = linkHeader.getLink(LinkHeader.Rel.PREV);
    assertNotNull(link);
    assertEquals("/resource?page=3&per_page=100", link.resourceUri().toString());
    assertEquals(3, link.page());
    assertEquals(100, link.perPage());

    link = linkHeader.getLink(LinkHeader.Rel.NEXT);
    assertNotNull(link);
    assertEquals("/resource?page=5&per_page=100", link.resourceUri().toString());
    assertEquals(5, link.page());
    assertEquals(100, link.perPage());

    link = linkHeader.getLink(LinkHeader.Rel.LAST);
    assertNotNull(link);
    assertEquals("/resource?page=50&per_page=100", link.resourceUri().toString());
    assertEquals(50, link.page());
    assertEquals(100, link.perPage());
  }
}
