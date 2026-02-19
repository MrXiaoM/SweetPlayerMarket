package top.mrxiaom.sweet.playermarket.func.i18n;

import com.google.gson.JsonObject;
import top.mrxiaom.pluginbase.utils.Util;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class MojangSource extends AbstractSource {
    public static final MojangSource INSTANCE = new MojangSource();

    @Override
    public String getName() {
        return "Mojang";
    }

    @Override
    protected String getVersionsManifestURL() {
        return "https://piston-meta.mojang.com/mc/game/version_manifest.json";
    }

    @Override
    protected String getVersionJsonURL(JsonObject versionObject) {
        return versionObject.get("url").getAsString();
    }

    @Override
    protected String getAssetIndexURL(JsonObject versionJson) {
        JsonObject assetIndex = versionJson.get("assetIndex").getAsJsonObject();
        return assetIndex.get("url").getAsString();
    }

    @Override
    public void downloadAsset(File file, String hash) {
        String url = "https://resources.download.minecraft.net/" + hash.substring(0, 2) + "/" + hash;
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.connect();
            Util.mkdirs(file.getParentFile());
            try (InputStream stream = conn.getInputStream();
                 FileOutputStream fos = new FileOutputStream(file)
            ) {
                byte[] buffer = new byte[16384];
                int length;
                while ((length = stream.read(buffer)) != -1) {
                    fos.write(buffer, 0, length);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
