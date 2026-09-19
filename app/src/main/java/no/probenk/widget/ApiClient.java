package no.probenk.widget;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

final class ApiClient {
    private ApiClient() {}

    static JSONObject pair(String baseUrl, String code, String deviceName) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("code", code);
        payload.put("device_name", deviceName);
        return request(Prefs.normalize(baseUrl) + "api/widget-pair.php", "POST", null, payload.toString());
    }

    static JSONObject fetchToday(String baseUrl, String token) throws Exception {
        return request(Prefs.normalize(baseUrl) + "api/widget-today.php", "GET", token, null);
    }

    private static JSONObject request(String urlString, String method, String token, String body) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(urlString).openConnection();
        c.setRequestMethod(method);
        c.setConnectTimeout(9000);
        c.setReadTimeout(12000);
        c.setRequestProperty("Accept", "application/json");
        c.setRequestProperty("User-Agent", "ProBenkWidget/1.0 Android");
        if (token != null && !token.isEmpty()) c.setRequestProperty("Authorization", "Bearer " + token);

        if (body != null) {
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            c.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream os = c.getOutputStream()) {
                os.write(bytes);
            }
        }

        int status = c.getResponseCode();
        InputStream in = status >= 200 && status < 300 ? c.getInputStream() : c.getErrorStream();
        StringBuilder sb = new StringBuilder();
        if (in != null) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
            }
        }
        c.disconnect();

        JSONObject json;
        try {
            json = new JSONObject(sb.length() == 0 ? "{}" : sb.toString());
        } catch (Exception ex) {
            throw new Exception("Serveren svarte ikke med gyldig JSON (HTTP " + status + ").");
        }

        if (status < 200 || status >= 300 || !json.optBoolean("ok", false)) {
            throw new Exception(json.optString("error", "HTTP " + status));
        }
        return json;
    }
}
