/*
 * Copyright 2018 Google LLC
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

const crypto = require('crypto');
const grpc = require('@grpc/grpc-js');

const CORRELATION_ID_METADATA_KEY = 'x-correlation-id';

function getCorrelationIdFromMetadata(metadata) {
  const values = metadata.get(CORRELATION_ID_METADATA_KEY);
  if (values && values.length > 0 && values[0]) {
    return values[0];
  }
  return null;
}

function generateCorrelationId() {
  return crypto.randomUUID();
}

function wrapServiceImplementation(serviceImplementation, logger) {
  const wrapped = {};
  for (const [methodName, handler] of Object.entries(serviceImplementation)) {
    wrapped[methodName] = (call, callback) => {
      const correlationId = getCorrelationIdFromMetadata(call.metadata) || generateCorrelationId();
      const reqLogger = logger.child({ correlation_id: correlationId });
      call.logger = reqLogger;
      reqLogger.debug({ grpc_method: methodName }, 'request started');
      const start = Date.now();
      const wrappedCallback = (err, response) => {
        reqLogger.debug({
          grpc_method: methodName,
          grpc_resp_took_ms: Date.now() - start,
          error: !!err,
        }, 'request complete');
        callback(err, response);
      };
      return handler(call, wrappedCallback);
    };
  }
  return wrapped;
}

function createCorrelationClientInterceptor() {
  return (options, nextCall) => {
    return new grpc.InterceptingCall(nextCall(options), {
      start: (metadata, listener, next) => {
        const correlationId = metadata.get(CORRELATION_ID_METADATA_KEY);
        if (!correlationId || correlationId.length === 0 || !correlationId[0]) {
          metadata.set(CORRELATION_ID_METADATA_KEY, generateCorrelationId());
        }
        next(metadata, listener);
      },
    });
  };
}

module.exports = {
  CORRELATION_ID_METADATA_KEY,
  wrapServiceImplementation,
  createCorrelationClientInterceptor,
  getCorrelationIdFromMetadata,
};
