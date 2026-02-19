package top.mrxiaom.sweet.playermarket.func.i18n;

import com.google.gson.JsonObject;
import top.mrxiaom.pluginbase.utils.Util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public class BMCLAPISource extends AbstractSource {
    public static final BMCLAPISource INSTANCE = new BMCLAPISource();

    @Override
    public String getName() {
        return "BMCLAPI";
    }

    @Override
    protected String getVersionsManifestURL() {
        return "https://bmclapi2.bangbang93.com/mc/game/version_manifest.json";
    }

    @Override
    protected String getVersionJsonURL(JsonObject versionObject) {
        String version = versionObject.get("id").getAsString();
        return "https://bmclapi2.bangbang93.com/version/" + version + "/json";
    }

    @Override
    protected String getAssetIndexURL(JsonObject versionJson) {
        JsonObject assetIndex = versionJson.get("assetIndex").getAsJsonObject();
        URI link = URI.create(assetIndex.get("url").getAsString());
        return "https://bmclapi2.bangbang93.com" + link.getPath();
    }

    @Override
    public void downloadAsset(File file, String hash) {
        String url = "https://bmclapi2.bangbang93.com/assets/" + hash.substring(0, 2) + "/" + hash;
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
