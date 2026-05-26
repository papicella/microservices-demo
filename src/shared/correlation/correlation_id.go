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
	"net/http"

	"github.com/google/uuid"
	"google.golang.org/grpc/metadata"
)

const (
	// HeaderName is the HTTP header used for correlation IDs.
	HeaderName = "X-Correlation-ID"
	// MetadataKey is the gRPC metadata key (lowercase per gRPC convention).
	MetadataKey = "x-correlation-id"
)

type ctxKey struct{}

// FromContext returns the correlation ID from ctx, or empty string if unset.
func FromContext(ctx context.Context) string {
	if v, ok := ctx.Value(ctxKey{}).(string); ok {
		return v
	}
	return ""
}

// WithCorrelationID returns a copy of ctx carrying correlationID.
func WithCorrelationID(ctx context.Context, correlationID string) context.Context {
	return context.WithValue(ctx, ctxKey{}, correlationID)
}

// NewID generates a new correlation ID (UUID v4).
func NewID() string {
	return uuid.NewString()
}

// ExtractOrGenerateFromHTTP reads X-Correlation-ID from headers or generates a new ID.
func ExtractOrGenerateFromHTTP(r *http.Request) string {
	if r != nil {
		if id := r.Header.Get(HeaderName); id != "" {
			return id
		}
	}
	return NewID()
}

// ExtractOrGenerateFromIncomingGRPC reads x-correlation-id from incoming gRPC metadata or generates a new ID.
func ExtractOrGenerateFromIncomingGRPC(ctx context.Context) string {
	if md, ok := metadata.FromIncomingContext(ctx); ok {
		if vals := md.Get(MetadataKey); len(vals) > 0 && vals[0] != "" {
			return vals[0]
		}
	}
	return NewID()
}

// OutgoingGRPCMetadata returns outgoing metadata with the correlation ID from ctx.
func OutgoingGRPCMetadata(ctx context.Context) metadata.MD {
	id := FromContext(ctx)
	if id == "" {
		return metadata.MD{}
	}
	return metadata.Pairs(MetadataKey, id)
}
