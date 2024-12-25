package org.pastal.launcher.http;

import java.lang.reflect.Method;

import com.google.gson.Gson;
import org.pastal.launcher.http.annotations.RequestBody;
import org.pastal.launcher.http.annotations.RequestParameter;
import org.pastal.launcher.event.events.RequestEvent;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;

public class RouteHandler {
    private final Class<?> controllerClass;
    private final Method method;

    public RouteHandler(Class<?> controllerClass, Method method) {
        this.controllerClass = controllerClass;
        this.method = method;
    }

    public Object invoke(RequestEvent event) throws Exception {
        Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();

        Parameter[] parameters = method.getParameters();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        Object[] args = new Object[parameters.length];

        Gson gson = new Gson();

        for (int i = 0; i < parameters.length; i++) {
            boolean handled = false;
            for (Annotation annotation : parameterAnnotations[i]) {
                if (annotation instanceof RequestBody) {
                    RequestBody requestBody = (RequestBody) annotation;
                    String body = event.getBody();
                    if (body == null || body.isEmpty()) {
                        if (requestBody.required()) {
                            throw new IllegalArgumentException("Missing required request body");
                        } else {
                            args[i] = null;
                        }
                    } else {
                        if (parameters[i].getType().equals(String.class)) {
                            args[i] = body;
                        } else {
                            args[i] = gson.fromJson(body, parameters[i].getType());
                        }
                    }
                    handled = true;
                    break;
                }
                if (annotation instanceof RequestParameter) {
                    RequestParameter requestParameter = (RequestParameter) annotation;
                    String paramName = requestParameter.value();
                    String paramValue = event.getQuery().get(paramName);

                    if (paramValue == null && requestParameter.required()) {
                        throw new IllegalArgumentException("Missing required parameter: " + paramName);
                    }

                    args[i] = convertType(paramValue, parameters[i].getType());
                    handled = true;
                    break;
                }
            }

            if (!handled && parameters[i].getType().isAssignableFrom(RequestEvent.class)) {
                args[i] = event;
            }
        }

        return method.invoke(controllerInstance, args);
    }

    private Object convertType(String value, Class<?> targetType) {
        if (value == null) return null;
        if (targetType == String.class) return value;
        if (targetType == int.class || targetType == Integer.class) return Integer.parseInt(value);
        if (targetType == long.class || targetType == Long.class) return Long.parseLong(value);
        if (targetType == double.class || targetType == Double.class) return Double.parseDouble(value);
        if (targetType == boolean.class || targetType == Boolean.class) return Boolean.parseBoolean(value);
        throw new IllegalArgumentException("Unsupported parameter type: " + targetType);
    }
}
