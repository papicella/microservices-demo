#!/usr/bin/python
#
# Copyright 2018 Google LLC
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

import uuid

import grpc

from logger import correlation_id_var

CORRELATION_ID_METADATA_KEY = 'x-correlation-id'


def get_correlation_id():
    return correlation_id_var.get()


def _extract_correlation_id(context):
    for key, value in context.invocation_metadata():
        if key.lower() == CORRELATION_ID_METADATA_KEY:
            return value
    return ''


def _set_correlation_id(context):
    correlation_id = _extract_correlation_id(context) or str(uuid.uuid4())
    correlation_id_var.set(correlation_id)
    return correlation_id


def _wrap_behavior(behavior):
    def wrapper(request_or_iterator, context):
        _set_correlation_id(context)
        return behavior(request_or_iterator, context)
    return wrapper


class CorrelationIdServerInterceptor(grpc.ServerInterceptor):
    def intercept_service(self, continuation, handler_call_details):
        handler = continuation(handler_call_details)
        if handler is None:
            return None

        if handler.unary_unary:
            return handler._replace(
                unary_unary=grpc.unary_unary_rpc_method_handler(
                    _wrap_behavior(handler.unary_unary),
                    request_deserializer=handler.request_deserializer,
                    response_serializer=handler.response_serializer,
                )
            )
        if handler.unary_stream:
            return handler._replace(
                unary_stream=grpc.unary_stream_rpc_method_handler(
                    _wrap_behavior(handler.unary_stream),
                    request_deserializer=handler.request_deserializer,
                    response_serializer=handler.response_serializer,
                )
            )
        if handler.stream_unary:
            return handler._replace(
                stream_unary=grpc.stream_unary_rpc_method_handler(
                    _wrap_behavior(handler.stream_unary),
                    request_deserializer=handler.request_deserializer,
                    response_serializer=handler.response_serializer,
                )
            )
        if handler.stream_stream:
            return handler._replace(
                stream_stream=grpc.stream_stream_rpc_method_handler(
                    _wrap_behavior(handler.stream_stream),
                    request_deserializer=handler.request_deserializer,
                    response_serializer=handler.response_serializer,
                )
            )
        return handler


class CorrelationIdClientInterceptor(
    grpc.UnaryUnaryClientInterceptor,
    grpc.UnaryStreamClientInterceptor,
    grpc.StreamUnaryClientInterceptor,
    grpc.StreamStreamClientInterceptor,
):
    def _append_metadata(self, client_call_details):
        metadata = list(client_call_details.metadata or [])
        correlation_id = get_correlation_id()
        if correlation_id and not any(k == CORRELATION_ID_METADATA_KEY for k, _ in metadata):
            metadata.append((CORRELATION_ID_METADATA_KEY, correlation_id))
        return client_call_details._replace(metadata=metadata)

    def intercept_unary_unary(self, continuation, client_call_details, request):
        return continuation(self._append_metadata(client_call_details), request)

    def intercept_unary_stream(self, continuation, client_call_details, request):
        return continuation(self._append_metadata(client_call_details), request)

    def intercept_stream_unary(self, continuation, client_call_details, request_iterator):
        return continuation(self._append_metadata(client_call_details), request_iterator)

    def intercept_stream_stream(self, continuation, client_call_details, request_iterator):
        return continuation(self._append_metadata(client_call_details), request_iterator)
