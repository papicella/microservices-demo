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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AdminConfigServiceTest {

  @Test
  void updateConfigReplacesState() {
    AdminConfigService service = new AdminConfigService();

    assertThat(service.getConfig().maintenanceMode()).isFalse();

    var updated = service.updateConfig(true, "Maintenance window", "ops");

    assertThat(updated.maintenanceMode()).isTrue();
    assertThat(updated.bannerMessage()).isEqualTo("Maintenance window");
    assertThat(updated.updatedBy()).isEqualTo("ops");
    assertThat(service.getConfig().maintenanceMode()).isTrue();
  }

  @Test
  void updateConfigUsesUnknownWhenUpdatedByBlank() {
    AdminConfigService service = new AdminConfigService();

    var updated = service.updateConfig(false, null, "  ");

    assertThat(updated.updatedBy()).isEqualTo("unknown");
  }
}
