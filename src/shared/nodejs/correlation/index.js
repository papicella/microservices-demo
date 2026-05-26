/*
 * Copyright 2026 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

'use strict';

const { AsyncLocalStorage } = require('async_hooks');
const { randomUUID } = require('crypto');

const HEADER_NAME = 'X-Correlation-ID';
const METADATA_KEY = 'x-correlation-id';

const storage = new AsyncLocalStorage();

function getCorrelationId() {
  return storage.getStore()?.correlationId || '';
}

function runWithCorrelationId(correlationId, fn) {
  return storage.run({ correlationId }, fn);
}

function extractOrGenerateFromHeaders(headers) {
  if (!headers) {
    return randomUUID();
  }
  const id = headers[HEADER_NAME] || headers[HEADER_NAME.toLowerCase()];
  return id || randomUUID();
}

function extractOrGenerateFromMetadata(metadata) {
  if (!metadata) {
    return randomUUID();
  }
  const values = metadata.get(METADATA_KEY);
  if (values && values.length > 0 && values[0]) {
    return values[0];
  }
  return randomUUID();
}

module.exports = {
  HEADER_NAME,
  METADATA_KEY,
  getCorrelationId,
  runWithCorrelationId,
  extractOrGenerateFromHeaders,
  extractOrGenerateFromMetadata,
};
