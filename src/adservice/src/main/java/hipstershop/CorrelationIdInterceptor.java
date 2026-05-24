/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hipstershop;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import java.util.UUID;
import org.apache.logging.log4j.ThreadContext;

public final class CorrelationIdInterceptor implements ServerInterceptor {
  public static final Context.Key<String> CORRELATION_ID_CTX =
      Context.key("correlation_id");
  private static final Metadata.Key<String> CORRELATION_ID_KEY =
      Metadata.Key.of("x-correlation-id", Metadata.ASCII_STRING_MARSHALLER);

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
      ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
    String correlationId = headers.get(CORRELATION_ID_KEY);
    if (correlationId == null || correlationId.isEmpty()) {
      correlationId = UUID.randomUUID().toString();
    }
    final String resolvedCorrelationId = correlationId;
    Context context = Context.current().withValue(CORRELATION_ID_CTX, resolvedCorrelationId);
    ServerCall.Listener<ReqT> listener = Contexts.interceptCall(context, call, headers, next);
    return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(listener) {
      @Override
      public void onMessage(ReqT message) {
        ThreadContext.put("correlation_id", resolvedCorrelationId);
        super.onMessage(message);
      }

      @Override
      public void onHalfClose() {
        ThreadContext.put("correlation_id", resolvedCorrelationId);
        super.onHalfClose();
      }

      @Override
      public void onComplete() {
        try {
          super.onComplete();
        } finally {
          ThreadContext.remove("correlation_id");
        }
      }

      @Override
      public void onCancel() {
        try {
          super.onCancel();
        } finally {
          ThreadContext.remove("correlation_id");
        }
      }
    };
  }
}
