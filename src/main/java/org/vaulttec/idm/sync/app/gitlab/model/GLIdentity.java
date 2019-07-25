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
package org.vaulttec.idm.sync.app.gitlab.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GLIdentity {

  private String provider;
  @JsonAlias("extern_uid")
  private String externUid;

  public String getProvider() {
    return provider;
  }

  public void setProvider(String provider) {
    this.provider = provider;
  }

  public String getExternUid() {
    return externUid;
  }

  public void setExternUid(String externUid) {
    this.externUid = externUid;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((externUid == null) ? 0 : externUid.hashCode());
    result = prime * result + ((provider == null) ? 0 : provider.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    GLIdentity other = (GLIdentity) obj;
    if (externUid == null) {
      if (other.externUid != null) {
        return false;
      }
    } else if (!externUid.equals(other.externUid)) {
      return false;
    }
    if (provider == null) {
      if (other.provider != null) {
        return false;
      }
    } else if (!provider.equals(other.provider)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return provider + "(" + externUid + ")";
  }
}