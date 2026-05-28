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

package com.hipstershop.adminservice.web;

import com.hipstershop.adminservice.service.AdminConfigService;
import com.hipstershop.adminservice.web.dto.AdminConfigRequest;
import com.hipstershop.adminservice.web.dto.AdminConfigResponse;
import com.hipstershop.adminservice.web.dto.AdminStatusResponse;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

  private final AdminConfigService adminConfigService;
  private final String serviceName;
  private final String version;
  private final Instant startedAt = Instant.now();

  public AdminController(
      AdminConfigService adminConfigService,
      @Value("${spring.application.name}") String serviceName,
      @Value("${info.app.version:0.1.0-SNAPSHOT}") String version) {
    this.adminConfigService = adminConfigService;
    this.serviceName = serviceName;
    this.version = version;
  }

  @GetMapping("/status")
  public AdminStatusResponse status() {
    return new AdminStatusResponse(
        serviceName,
        version,
        startedAt,
        AdminConfigResponse.from(adminConfigService.getConfig()));
  }

  @GetMapping("/config")
  public AdminConfigResponse getConfig() {
    return AdminConfigResponse.from(adminConfigService.getConfig());
  }

  @PutMapping("/config")
  public ResponseEntity<AdminConfigResponse> updateConfig(
      @Valid @RequestBody AdminConfigRequest request) {
    var updated =
        adminConfigService.updateConfig(
            request.maintenanceMode(), request.bannerMessage(), request.updatedBy());
    return ResponseEntity.ok(AdminConfigResponse.from(updated));
  }
}
