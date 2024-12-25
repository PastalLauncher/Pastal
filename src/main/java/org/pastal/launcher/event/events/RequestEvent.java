package org.pastal.launcher.event.events;

import com.sun.net.httpserver.HttpExchange;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.pastal.launcher.enums.MediaType;
import org.pastal.launcher.event.impl.Event;
import org.tinylog.Logger;

import java.util.Map;

@Getter
@AllArgsConstructor
public class RequestEvent implements Event {
    private String method;
    private String path;
    private Map<String, String> query;
    private Map<String, String> headers;
    private String body;
    private HttpExchange exchange;

    public void doResponse(int code, String content, MediaType mediaType) {
        try {
            exchange.getResponseHeaders().set("Content-Type", mediaType.getType());
            exchange.sendResponseHeaders(code, content.getBytes().length);
            exchange.getResponseBody().write(content.getBytes());
            exchange.getResponseBody().flush();
        } catch (Exception e) {
            Logger.error(e);
        } finally {
            try {
                exchange.getResponseBody().close();
            } catch (Exception e) {
                Logger.error(e);
            }
        }
    }

    public void doResponse(int code, byte[] content) {
        try {
            exchange.sendResponseHeaders(code, content.length);
            exchange.getResponseBody().write(content);
            exchange.getResponseBody().flush();
        } catch (Exception e) {
            Logger.error(e);
        } finally {
            try {
                exchange.getResponseBody().close();
            } catch (Exception e) {
                Logger.error(e);
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Method: ").append(method).append("\n");
        sb.append("Path: ").append(path).append("\n");
        if (query != null) {
            sb.append("Query: ").append(query).append("\n");
        }
        sb.append("Headers:\n");
        headers.forEach((key, value) -> sb.append(key).append(": ").append(value).append("\n"));
        sb.append("Body:\n").append(body).append("\n");
        return sb.toString();
    }
}