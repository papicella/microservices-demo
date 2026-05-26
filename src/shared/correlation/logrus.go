// Copyright 2026 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package correlation

import (
	"context"

	"github.com/sirupsen/logrus"
)

// Entry returns a logrus entry enriched with correlation_id from ctx when present.
func Entry(ctx context.Context, log *logrus.Logger, serviceName string) *logrus.Entry {
	fields := logrus.Fields{"service": serviceName}
	if id := FromContext(ctx); id != "" {
		fields["correlation_id"] = id
	}
	return log.WithFields(fields)
}

// FieldLogger returns a FieldLogger enriched with correlation_id from ctx when present.
func FieldLogger(ctx context.Context, log logrus.FieldLogger, serviceName string) logrus.FieldLogger {
	fields := logrus.Fields{"service": serviceName}
	if id := FromContext(ctx); id != "" {
		fields["correlation_id"] = id
	}
	return log.WithFields(fields)
}
