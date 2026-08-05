# Rate Limiter Module Architecture (Mermaid)

This file contains Mermaid diagrams visualizing the structure and design of the Rate Limiter plugin (`crud-engine-plugin-ratelimiter`).

## 1. Class Structure

```mermaid
classDiagram
    class WebFilter {
        <<interface>>
        +filter(ServerWebExchange, WebFilterChain) Mono
    }

    class ReactiveRateLimiterFilter {
        -MAX_TOKENS : int
        -REFILL_DURATION_MS : long
        -Map~String, TokenBucket~ ipBuckets
        -AppModeConfig appModeConfig
        -forceRateLimit : boolean
        +filter(ServerWebExchange, WebFilterChain) Mono
        +reset() void
    }

    class TokenBucket {
        -capacity : int
        -refillDurationMs : long
        -tokens : double
        -lastRefillTime : long
        +tryConsume() boolean
        -refill() void
    }

    ReactiveRateLimiterFilter ..|> WebFilter : implements
    ReactiveRateLimiterFilter *-- TokenBucket : manages
```

## 2. Token Bucket Consumption Flow

```mermaid
graph TD
    A[Request arrives] --> B[Retrieve bucket for IP]
    B --> C[Call bucket.tryConsume]
    C --> D[Refill tokens based on elapsed time]
    D --> E{Are tokens >= 1?}
    E -- Yes --> F[Subtract 1 token]
    F --> G[Return true: Request Allowed]
    E -- No --> H[Return false: Request Blocked with HTTP 429]
```
