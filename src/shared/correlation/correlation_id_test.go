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
	"net/http/httptest"
	"testing"

	"google.golang.org/grpc/metadata"
)

func TestFromContextRoundTrip(t *testing.T) {
	ctx := WithCorrelationID(context.Background(), "test-id")
	if got := FromContext(ctx); got != "test-id" {
		t.Fatalf("FromContext() = %q, want test-id", got)
	}
}

func TestExtractOrGenerateFromHTTP(t *testing.T) {
	req := httptest.NewRequest("GET", "/", nil)
	req.Header.Set(HeaderName, "incoming-123")
	if got := ExtractOrGenerateFromHTTP(req); got != "incoming-123" {
		t.Fatalf("ExtractOrGenerateFromHTTP() = %q, want incoming-123", got)
	}

	req2 := httptest.NewRequest("GET", "/", nil)
	if got := ExtractOrGenerateFromHTTP(req2); got == "" {
		t.Fatal("expected generated correlation ID")
	}
}

func TestExtractOrGenerateFromIncomingGRPC(t *testing.T) {
	ctx := metadata.NewIncomingContext(context.Background(), metadata.Pairs(MetadataKey, "grpc-456"))
	if got := ExtractOrGenerateFromIncomingGRPC(ctx); got != "grpc-456" {
		t.Fatalf("ExtractOrGenerateFromIncomingGRPC() = %q, want grpc-456", got)
	}
}
