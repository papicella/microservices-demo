// Copyright 2020 Google LLC
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

using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Threading.Tasks;
using Grpc.Core;
using Grpc.Core.Interceptors;
using Microsoft.Extensions.Logging;

namespace cartservice.interceptors
{
    public class CorrelationIdInterceptor : Interceptor
    {
        private const string CorrelationIdHeader = "x-correlation-id";
        private readonly ILogger<CorrelationIdInterceptor> _logger;

        public CorrelationIdInterceptor(ILogger<CorrelationIdInterceptor> logger)
        {
            _logger = logger;
        }

        public override async Task<TResponse> UnaryServerHandler<TRequest, TResponse>(
            TRequest request,
            ServerCallContext context,
            UnaryServerMethod<TRequest, TResponse> continuation)
        {
            var correlationId = context.RequestHeaders
                .FirstOrDefault(h => string.Equals(h.Key, CorrelationIdHeader, StringComparison.OrdinalIgnoreCase))?.Value;
            if (string.IsNullOrEmpty(correlationId))
            {
                correlationId = Guid.NewGuid().ToString();
            }

            using (_logger.BeginScope(new Dictionary<string, object>
            {
                ["correlation_id"] = correlationId
            }))
            {
                _logger.LogDebug("request started for {GrpcMethod}", context.Method);
                var stopwatch = Stopwatch.StartNew();
                try
                {
                    return await continuation(request, context);
                }
                finally
                {
                    _logger.LogDebug(
                        "request complete for {GrpcMethod} in {DurationMs}ms",
                        context.Method,
                        stopwatch.ElapsedMilliseconds);
                }
            }
        }
    }
}
