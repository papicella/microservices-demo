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

const grpc = require('@grpc/grpc-js');
const {
  METADATA_KEY,
  extractOrGenerateFromMetadata,
  getCorrelationId,
  runWithCorrelationId,
} = require('./index');

function correlationServerInterceptor(serviceName, logger) {
  return function interceptServer(methodDescriptor, call) {
    const correlationId = extractOrGenerateFromMetadata(call.metadata);
    return runWithCorrelationId(correlationId, () => {
      const log = logger.child({
        correlation_id: correlationId,
        service: serviceName,
        'grpc.method': methodDescriptor.path,
      });
      log.debug('grpc request started');
      const bound = methodDescriptor.originalPath
        ? call
        : call;
      return new grpc.ServerInterceptingCall(call, {
        start: (next) => {
          next();
        },
        sendStatus: (status, next) => {
          log.debug('grpc request completed');
          next(status);
        },
      });
    });
  };
}

function wrapHandlers(handlers, serviceName, logger) {
  const wrapped = {};
  for (const [name, handler] of Object.entries(handlers)) {
    if (typeof handler === 'function') {
      wrapped[name] = (call, callback) => {
        const correlationId = extractOrGenerateFromMetadata(call.metadata);
        runWithCorrelationId(correlationId, () => {
          const log = logger.child({
            correlation_id: correlationId,
            service: serviceName,
            'grpc.method': name,
          });
          log.debug('grpc request started');
          handler(call, (err, response) => {
            log.debug('grpc request completed');
            callback(err, response);
          });
        });
      };
    } else {
      wrapped[name] = handler;
    }
  }
  return wrapped;
}

function correlationClientInterceptor(options, nextCall) {
  const correlationId = getCorrelationId();
  if (!correlationId) {
    return nextCall(options);
  }
  const metadata = options.metadata || new grpc.Metadata();
  metadata.set(METADATA_KEY, correlationId);
  return nextCall({ ...options, metadata });
}

module.exports = {
  correlationServerInterceptor,
  correlationClientInterceptor,
  wrapHandlers,
};
