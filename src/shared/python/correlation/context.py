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

import contextvars
import uuid

HEADER_NAME = "X-Correlation-ID"
METADATA_KEY = "x-correlation-id"

_correlation_id: contextvars.ContextVar[str] = contextvars.ContextVar(
    "correlation_id", default=""
)


def get_correlation_id() -> str:
    return _correlation_id.get()


def set_correlation_id(correlation_id: str) -> None:
    _correlation_id.set(correlation_id)


def new_id() -> str:
    return str(uuid.uuid4())


def extract_or_generate_from_http(headers) -> str:
    correlation_id = headers.get(HEADER_NAME) or headers.get(HEADER_NAME.lower())
    if correlation_id:
        return correlation_id
    return new_id()
