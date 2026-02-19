package top.mrxiaom.sweet.playermarket.func.i18n;

import com.google.gson.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public abstract class AbstractSource implements IDownloadSource {
    private final Gson gson = new GsonBuilder().create();
    protected abstract String getVersionsManifestURL();
    protected abstract String getVersionJsonURL(JsonObject versionObject);
    protected abstract String getAssetIndexURL(JsonObject versionJson);

    @Override
    public JsonObject getVersionsManifest() {
        try {
            String url = getVersionsManifestURL();
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.connect();
            try (InputStream stream = conn.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
            ) {
                return gson.fromJson(reader, JsonObject.class);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public JsonObject getVersionJson(JsonObject manifestJson, String version) {
        try {
            JsonArray versions = manifestJson.get("versions").getAsJsonArray();
            JsonObject versionObject = null;
            for (JsonElement element : versions) {
                JsonObject obj = element.getAsJsonObject();
                if (version.equals(obj.get("id").getAsString())) {
                    versionObject = obj;
                    break;
                }
            }
            if (versionObject == null) {
                throw new IllegalArgumentException("找不到版本 " + version);
            }
            String url = getVersionJsonURL(versionObject);
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.connect();
            try (InputStream stream = conn.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
            ) {
                return gson.fromJson(reader, JsonObject.class);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public JsonObject getAssetIndex(JsonObject versionJson) {
        try {
            String url = getAssetIndexURL(versionJson);
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.connect();
            try (InputStream stream = conn.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
            ) {
                return gson.fromJson(reader, JsonObject.class);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
