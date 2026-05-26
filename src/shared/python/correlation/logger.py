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
import sys

from pythonjsonlogger import jsonlogger

from .context import get_correlation_id


class CorrelationJsonFormatter(jsonlogger.JsonFormatter):
    def __init__(self, service_name: str, *args, **kwargs):
        self._service_name = service_name
        super().__init__(*args, **kwargs)

    def add_fields(self, log_record, record, message_dict):
        super().add_fields(log_record, record, message_dict)
        if not log_record.get("timestamp"):
            log_record["timestamp"] = record.created
        log_record["service"] = self._service_name
        correlation_id = get_correlation_id()
        if correlation_id:
            log_record["correlation_id"] = correlation_id
        if log_record.get("severity"):
            log_record["severity"] = log_record["severity"].upper()
        else:
            log_record["severity"] = record.levelname


def get_json_logger(name: str, service_name: str) -> logging.Logger:
    logger = logging.getLogger(name)
    if logger.handlers:
        return logger
    handler = logging.StreamHandler(sys.stdout)
    formatter = CorrelationJsonFormatter(
        service_name,
        "%(timestamp)s %(severity)s %(name)s %(message)s",
    )
    handler.setFormatter(formatter)
    logger.addHandler(handler)
    logger.setLevel(logging.INFO)
    logger.propagate = False
    return logger
