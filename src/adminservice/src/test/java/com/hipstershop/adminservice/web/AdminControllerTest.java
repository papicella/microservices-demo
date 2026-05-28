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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hipstershop.adminservice.config.ApiKeyAuthFilter;
import com.hipstershop.adminservice.config.SecurityConfig;
import com.hipstershop.adminservice.service.AdminConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminController.class)
@Import({AdminConfigService.class, SecurityConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = "admin.api-key=test-api-key")
class AdminControllerTest {

  private static final String API_KEY_HEADER = ApiKeyAuthFilter.API_KEY_HEADER;

  @Autowired private MockMvc mockMvc;

  @Test
  void getConfigRequiresApiKey() throws Exception {
    mockMvc.perform(get("/api/v1/admin/config")).andExpect(status().isUnauthorized());
  }

  @Test
  void getConfigReturnsDefaultsWithValidApiKey() throws Exception {
    mockMvc
        .perform(get("/api/v1/admin/config").header(API_KEY_HEADER, "test-api-key"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.maintenanceMode").value(false));
  }

  @Test
  void putConfigUpdatesState() throws Exception {
    mockMvc
        .perform(
            put("/api/v1/admin/config")
                .header(API_KEY_HEADER, "test-api-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "maintenanceMode": true,
                      "bannerMessage": "Deploy in progress",
                      "updatedBy": "ops"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.maintenanceMode").value(true))
        .andExpect(jsonPath("$.bannerMessage").value("Deploy in progress"))
        .andExpect(jsonPath("$.updatedBy").value("ops"));

    mockMvc
        .perform(get("/api/v1/admin/config").header(API_KEY_HEADER, "test-api-key"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.maintenanceMode").value(true));
  }

  @Test
  void statusIncludesServiceMetadata() throws Exception {
    mockMvc
        .perform(get("/api/v1/admin/status").header(API_KEY_HEADER, "test-api-key"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.serviceName").exists())
        .andExpect(jsonPath("$.version").exists())
        .andExpect(jsonPath("$.startedAt").exists())
        .andExpect(jsonPath("$.config.maintenanceMode").exists());
  }

  @Test
  void putConfigRejectsMissingMaintenanceMode() throws Exception {
    mockMvc
        .perform(
            put("/api/v1/admin/config")
                .header(API_KEY_HEADER, "test-api-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"bannerMessage\": \"hello\"}"))
        .andExpect(status().isBadRequest());
  }
}
