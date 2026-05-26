# Copyright 2026 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import logging

import grpc

from .context import METADATA_KEY, new_id, set_correlation_id


class CorrelationServerInterceptor(grpc.ServerInterceptor):
    def __init__(self, service_name: str, log: logging.Logger):
        self._service_name = service_name
        self._log = log

    def intercept_service(self, continuation, handler_call_details):
        handler = continuation(handler_call_details)
        if handler is None:
            return None

        def wrap(behavior, request_deserializer, response_serializer):
            def new_behavior(request, context):
                metadata = dict(context.invocation_metadata())
                correlation_id = metadata.get(METADATA_KEY) or new_id()
                set_correlation_id(correlation_id)
                self._log.info(
                    "grpc request started",
                    extra={"grpc_method": handler_call_details.method},
                )
                try:
                    return behavior(request, context)
                finally:
                    self._log.info(
                        "grpc request completed",
                        extra={"grpc_method": handler_call_details.method},
                    )

            return grpc.unary_unary_rpc_method_handler(
                new_behavior,
                request_deserializer=request_deserializer,
                response_serializer=response_serializer,
            )

        if handler.unary_unary is not None:
            method_handler = handler.unary_unary
            return handler._replace(
                unary_unary=wrap(
                    method_handler.unary_unary,
                    method_handler.request_deserializer,
                    method_handler.response_serializer,
                )
            )
        return handler


class CorrelationClientInterceptor(
    grpc.UnaryUnaryClientInterceptor,
    grpc.UnaryStreamClientInterceptor,
    grpc.StreamUnaryClientInterceptor,
    grpc.StreamStreamClientInterceptor,
):
    def _inject_metadata(self, client_call_details):
        from .context import get_correlation_id

        correlation_id = get_correlation_id()
        if not correlation_id:
            return client_call_details
        metadata = list(client_call_details.metadata or ())
        metadata.append((METADATA_KEY, correlation_id))
        return client_call_details._replace(metadata=metadata)

    def intercept_unary_unary(self, continuation, client_call_details, request):
        return continuation(self._inject_metadata(client_call_details), request)

    def intercept_unary_stream(self, continuation, client_call_details, request):
        return continuation(self._inject_metadata(client_call_details), request)

    def intercept_stream_unary(self, continuation, client_call_details, request_iterator):
        return continuation(self._inject_metadata(client_call_details), request_iterator)

    def intercept_stream_stream(self, continuation, client_call_details, request_iterator):
        return continuation(self._inject_metadata(client_call_details), request_iterator)
