package org.pastal.launcher.components.network;

import com.sun.net.httpserver.HttpServer;
import lombok.Getter;
import lombok.SneakyThrows;
import org.pastal.launcher.Launcher;
import org.pastal.launcher.api.interfaces.Component;
import org.pastal.launcher.event.events.RequestEvent;
import org.pastal.launcher.managers.ComponentManager;
import org.pastal.launcher.util.IOUtils;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class LocalServer implements Component {
    @Getter
    private int serverPort;
    private HttpServer server;

    @Override
    @SneakyThrows
    public void setup(ComponentManager componentManager) {
        server = HttpServer.create(new InetSocketAddress(41275), 0);
        serverPort = 41275;

        server.createContext("/", exchange -> {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            Map<String, String> query = parseQuery(exchange.getRequestURI().getQuery());
            Map<String, String> headers = exchange.getRequestHeaders().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> String.join(", ", e.getValue())));
            String body = IOUtils.toString(exchange.getRequestBody());

            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");

            RequestEvent requestEvent = new RequestEvent(method, path, query, headers, body, exchange);
            Launcher.getInstance().getEventManager().call(requestEvent);
        });

        server.setExecutor(null);
        server.start();
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> queryMap = new HashMap<>();
        if (query != null && !query.isEmpty()) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=", 2);
                String key = keyValue[0];
                String value = keyValue.length > 1 ? keyValue[1] : "";
                queryMap.put(key, value);
            }
        }
        return queryMap;
    }
}
