// Copyright 2018 Google LLC
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

package main

import (
	"context"
	"time"

	"github.com/google/uuid"
	"github.com/sirupsen/logrus"
	"google.golang.org/grpc"
	"google.golang.org/grpc/metadata"
)

const correlationIDMetadataKey = "x-correlation-id"

type ctxKeyCorrelationID struct{}

func correlationIDFromContext(ctx context.Context) string {
	if id, ok := ctx.Value(ctxKeyCorrelationID{}).(string); ok && id != "" {
		return id
	}
	return ""
}

func contextWithCorrelationID(ctx context.Context, id string) context.Context {
	return context.WithValue(ctx, ctxKeyCorrelationID{}, id)
}

func correlationIDFromIncomingContext(ctx context.Context) string {
	if md, ok := metadata.FromIncomingContext(ctx); ok {
		if vals := md.Get(correlationIDMetadataKey); len(vals) > 0 && vals[0] != "" {
			return vals[0]
		}
	}
	return ""
}

func correlationUnaryServerInterceptor(baseLog *logrus.Logger) grpc.UnaryServerInterceptor {
	return func(ctx context.Context, req interface{}, info *grpc.UnaryServerInfo, handler grpc.UnaryHandler) (interface{}, error) {
		correlationID := correlationIDFromContext(ctx)
		if correlationID == "" {
			correlationID = correlationIDFromIncomingContext(ctx)
		}
		if correlationID == "" {
			correlationID = uuid.New().String()
		}
		ctx = contextWithCorrelationID(ctx, correlationID)

		entryLog := baseLog.WithFields(logrus.Fields{
			"correlation_id": correlationID,
			"grpc.method":    info.FullMethod,
		})
		entryLog.Debug("request started")
		start := time.Now()
		resp, err := handler(ctx, req)
		entryLog.WithFields(logrus.Fields{
			"grpc.resp.took_ms": time.Since(start).Milliseconds(),
			"error":             err != nil,
		}).Debug("request complete")
		return resp, err
	}
}

func loggerFromContext(ctx context.Context, base *logrus.Logger) *logrus.Entry {
	if id := correlationIDFromContext(ctx); id != "" {
		return base.WithField("correlation_id", id)
	}
	return logrus.NewEntry(base)
}
