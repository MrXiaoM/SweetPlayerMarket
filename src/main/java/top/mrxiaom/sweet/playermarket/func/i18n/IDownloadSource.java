package top.mrxiaom.sweet.playermarket.func.i18n;

import com.google.gson.JsonObject;

import java.io.File;

public interface IDownloadSource {
    String getName();
    JsonObject getVersionsManifest();
    JsonObject getVersionJson(JsonObject manifestJson, String version);
    JsonObject getAssetIndex(JsonObject versionJson);
    void downloadAsset(File file, String hash);
}
