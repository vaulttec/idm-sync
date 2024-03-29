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
package org.vaulttec.idm.sync;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.vaulttec.idm.sync.app.Application;
import org.vaulttec.idm.sync.idp.IdentityProvider;

import java.util.List;

@ActiveProfiles("test")
@IfProfileValue(name = "run.integration.tests", value = "true")
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
class SyncTaskIntegrationTest {

  @Autowired
  private IdentityProvider idp;
  @Autowired
  private List<Application> applications;
  @Autowired
  private SyncConfig syncConfig;

  @Test
  void testSync() throws InstantiationException, IllegalAccessException {
    SyncTask task = new SyncTask(idp, applications, syncConfig);
    task.sync();
  }
}
