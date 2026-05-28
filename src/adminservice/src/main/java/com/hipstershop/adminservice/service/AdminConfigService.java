/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hipstershop.adminservice.service;

import com.hipstershop.adminservice.domain.AdminConfig;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Service;

@Service
public class AdminConfigService {

  private final AtomicReference<AdminConfig> config = new AtomicReference<>(AdminConfig.initial());

  public AdminConfig getConfig() {
    return config.get();
  }

  public AdminConfig updateConfig(
      boolean maintenanceMode, String bannerMessage, String updatedBy) {
    AdminConfig updated =
        config.get().withUpdate(maintenanceMode, bannerMessage, updatedBy);
    config.set(updated);
    return updated;
  }
}
