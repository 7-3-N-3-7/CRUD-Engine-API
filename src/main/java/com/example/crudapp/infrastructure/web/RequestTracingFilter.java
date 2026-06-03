package com.example.crudapp.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.slf4j.MDC;
import java.util.UUID;

public class RequestTracingFilter implements Handler {
    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    public static final String MDC_KEY = "requestId";

    @Override
    public void handle(Context ctx) throws Exception {
        String requestId = ctx.header(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        MDC.put(MDC_KEY, requestId);
        ctx.header(REQUEST_ID_HEADER, requestId);
        ctx.attribute(MDC_KEY, requestId);
    }

    public static void clear() {
        MDC.remove(MDC_KEY);
    }
}
