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
	"time"

	"github.com/sirupsen/logrus"
	"google.golang.org/grpc"
)

// UnaryServerInterceptor extracts or generates a correlation ID and logs RPC access.
func UnaryServerInterceptor(serviceName string, log *logrus.Logger) grpc.UnaryServerInterceptor {
	return func(ctx context.Context, req interface{}, info *grpc.UnaryServerInfo, handler grpc.UnaryHandler) (interface{}, error) {
		correlationID := ExtractOrGenerateFromIncomingGRPC(ctx)
		ctx = WithCorrelationID(ctx, correlationID)

		entry := log.WithFields(logrus.Fields{
			"service":        serviceName,
			"correlation_id": correlationID,
			"grpc.method":    info.FullMethod,
		})
		start := time.Now()
		entry.Debug("grpc request started")
		resp, err := handler(ctx, req)
		entry.WithField("grpc.duration_ms", time.Since(start).Milliseconds()).Debug("grpc request completed")
		return resp, err
	}
}
