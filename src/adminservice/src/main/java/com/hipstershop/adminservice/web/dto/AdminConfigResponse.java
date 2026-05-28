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

package com.hipstershop.adminservice.web.dto;

import com.hipstershop.adminservice.domain.AdminConfig;
import java.time.Instant;

public record AdminConfigResponse(
    boolean maintenanceMode, String bannerMessage, Instant updatedAt, String updatedBy) {

  public static AdminConfigResponse from(AdminConfig config) {
    return new AdminConfigResponse(
        config.maintenanceMode(),
        config.bannerMessage(),
        config.updatedAt(),
        config.updatedBy());
  }
}
