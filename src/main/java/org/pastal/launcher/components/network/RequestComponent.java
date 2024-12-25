package org.pastal.launcher.components.network;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import okhttp3.*;
import org.pastal.launcher.api.interfaces.Component;
import org.pastal.launcher.managers.ComponentManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Getter
public class RequestComponent implements Component {
    private OkHttpClient client;
    private OkHttpClient noRedirectClient;

    @Override
    public void setup(ComponentManager componentManager) {
        client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
        noRedirectClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .followRedirects(false)
                .followSslRedirects(false)
                .build();
    }

    public String getAsHeader(String url, Map<String, String> headers, String headerName) throws IOException {
        Request.Builder requestBuilder = new Request.Builder().get().url(url);
        if (headers != null) {
            headers.forEach(requestBuilder::addHeader);
        }
        try (Response response = noRedirectClient.newCall(requestBuilder.build()).execute()) {
            return response.header(headerName);
        }
    }


    public Map<String, String> createStandardHeaders() {
        final Map<String, String> headers = new LinkedHashMap<>();
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/102.0.0.0 Safari/537.36");
        return headers;
    }


    public String get(final String url, final Map<String, String> headers) throws IOException {
        final Request.Builder requestBuilder = new Request.Builder().url(url);
        return getString(headers, requestBuilder);
    }

    public String postExternal(final String url, final String post, final boolean json) throws IOException {
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/102.0.0.0 Safari/537.36");
        headers.put("Content-Type", json ? "application/json" : "application/x-www-form-urlencoded; charset=UTF-8");
        headers.put("Accept", "application/json");

        return postJson(url, headers, post);
    }


    public JsonObject getAsJsonObject(final String url, final Map<String, String> headers) throws IOException {
        headers.put("Accept", "application/json");
        return JsonParser.parseString(get(url, headers)).getAsJsonObject();
    }

    public String postJson(final String url, final Map<String, String> headers, final String body) throws IOException {
        final RequestBody requestBody = RequestBody.create(body, MediaType.get("application/json"));

        final Request.Builder requestBuilder = new Request.Builder().url(url).post(requestBody);
        return getString(headers, requestBuilder);
    }

    public String postUrlEncoded(final String url, final Map<String, String> headers, final String body) throws IOException {
        final RequestBody requestBody = RequestBody.create(body, MediaType.get("application/x-www-form-urlencoded"));

        final Request.Builder requestBuilder = new Request.Builder().url(url).post(requestBody);
        return getString(headers, requestBuilder);
    }

    private String getString(final Map<String, String> headers, final Request.Builder requestBuilder) throws IOException {
        if (headers != null) {
            headers.forEach(requestBuilder::addHeader);
        }

        final Request request = requestBuilder.build();

        try (final Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response code: " + response.code() + " - " + response.body().string());
            }

            if (response.body() == null) {
                throw new IOException("Response body is null for URL: " + request.url());
            }

            return response.body().string();
        }
    }

    public String put(final String url, final Map<String, String> headers, final String body) throws IOException {
        final RequestBody requestBody = RequestBody.create(body, MediaType.get("application/json; charset=utf-8"));

        final Request.Builder requestBuilder = new Request.Builder().url(url).put(requestBody);
        return getString(headers, requestBuilder);
    }

    public String delete(final String url, final Map<String, String> headers) throws IOException {
        final Request.Builder requestBuilder = new Request.Builder().url(url).delete();
        return getString(headers, requestBuilder);
    }


    public long getFileSize(final String url) throws IOException {
        final OkHttpClient client = new OkHttpClient();

        final Request request = new Request.Builder()
                .url(url)
                .head()
                .build();

        try (final Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                final String contentLength = response.header("Content-Length");
                if (contentLength != null) {
                    return Long.parseLong(contentLength);
                }
            }
        }

        return 0;
    }

}
