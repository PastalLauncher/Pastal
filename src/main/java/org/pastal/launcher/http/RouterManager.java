package org.pastal.launcher.http;

import com.google.gson.Gson;
import org.pastal.launcher.Launcher;
import org.pastal.launcher.enums.MediaType;
import org.pastal.launcher.http.annotations.RequestMapping;
import org.pastal.launcher.http.annotations.RestController;
import org.pastal.launcher.event.annotations.EventTarget;
import org.pastal.launcher.event.events.RequestEvent;
import org.pastal.launcher.http.implement.Auth;
import org.pastal.launcher.http.implement.api.Join;
import org.pastal.launcher.http.implement.api.v1.AccountController;
import org.pastal.launcher.http.implement.api.v1.InstallationController;
import org.pastal.launcher.http.implement.api.v1.StatusController;
import org.pastal.launcher.http.implement.api.v1.accounts.CreateController;
import org.tinylog.Logger;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class RouterManager {
    private final Map<String, RouteHandler> routes = new HashMap<>();

    public RouterManager(Launcher launcher){
        launcher.getEventManager().register(this);

        registerRoutes(Auth.class, Join.class);

        registerRoutes(AccountController.class, StatusController.class, CreateController.class,
                InstallationController.class);
    }

    public void registerRoutes(Class<?>... classes) {
        try {
            for (Class<?> clazz : classes) {
                if (clazz.isAnnotationPresent(RestController.class)) {
                    RestController controller = clazz.getAnnotation(RestController.class);
                    String basePath = controller.value();

                    for (Method method : clazz.getDeclaredMethods()) {
                        if (method.isAnnotationPresent(RequestMapping.class)) {
                            RequestMapping mapping = method.getAnnotation(RequestMapping.class);
                            String fullPath = basePath + mapping.path();
                            Logger.debug("Register path {} for {}",fullPath,method.getName());
                            routes.put(mapping.method() + ":" + fullPath, new RouteHandler(clazz, method));
                        }
                    }
                }
            }
        } catch (Exception e) {
            Logger.error(e);
        }
    }


    @EventTarget
    public void onRequest(RequestEvent e) {
        String key = e.getMethod() + ":" + e.getPath();
        RouteHandler handler = routes.get(key);
        if (handler != null) {
            try {
                Object response = handler.invoke(e);

                if (response instanceof String) {
                    e.doResponse(200, (String) response, MediaType.PLAIN_TEXT);
                } else if (response != null) {
                    Gson gson = new Gson();
                    String jsonResponse = gson.toJson(response);
                    e.doResponse(200, jsonResponse, MediaType.JSON);
                }
            } catch (Exception ex) {
                e.doResponse(500, "Internal Server Error", MediaType.PLAIN_TEXT);
                ex.printStackTrace();
            }
        } else {
            e.doResponse(404, "Not Found", MediaType.PLAIN_TEXT);
        }
    }
}
