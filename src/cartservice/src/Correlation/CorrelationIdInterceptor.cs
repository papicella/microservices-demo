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

using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Grpc.Core;
using Grpc.Core.Interceptors;
using Microsoft.Extensions.Logging;

namespace cartservice.Correlation
{
    public class CorrelationIdInterceptor : Interceptor
    {
        public const string MetadataKey = "x-correlation-id";
        private readonly ILogger<CorrelationIdInterceptor> _logger;
        private readonly string _serviceName;

        public CorrelationIdInterceptor(ILogger<CorrelationIdInterceptor> logger, string serviceName = "cartservice")
        {
            _logger = logger;
            _serviceName = serviceName;
        }

        public override async Task<TResponse> UnaryServerHandler<TRequest, TResponse>(
            TRequest request,
            ServerCallContext context,
            UnaryServerMethod<TRequest, TResponse> continuation)
        {
            var correlationId = context.RequestHeaders.GetValue(MetadataKey);
            if (string.IsNullOrEmpty(correlationId))
            {
                correlationId = Guid.NewGuid().ToString();
            }

            using (_logger.BeginScope(new Dictionary<string, object>
            {
                ["correlation_id"] = correlationId,
                ["service"] = _serviceName,
                ["grpc.method"] = context.Method,
            }))
            {
                _logger.LogInformation("grpc request started");
                var response = await continuation(request, context);
                _logger.LogInformation("grpc request completed");
                return response;
            }
        }
    }
}
