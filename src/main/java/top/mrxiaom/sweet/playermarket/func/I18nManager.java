package top.mrxiaom.sweet.playermarket.func;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.utils.*;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.func.i18n.BMCLAPISource;
import top.mrxiaom.sweet.playermarket.func.i18n.IDownloadSource;
import top.mrxiaom.sweet.playermarket.func.i18n.MojangSource;

import java.io.*;
import java.util.*;

public class I18nManager extends AbstractModule {
    private final Gson gson = new GsonBuilder().create();
    private final File configFile;
    private String minecraftVersion;
    private IDownloadSource downloadSource;
    private final Map<String, String> translation = new HashMap<>();
    private final boolean supportTranslatable = Util.isPresent("org.bukkit.Translatable");
    private final boolean supportTranslatableAdventure = Util.isPresent("net.kyori.adventure.translation.Translatable");
    public I18nManager(SweetPlayerMarket plugin) {
        super(plugin);
        this.configFile = new File(plugin.getDataFolder(), "versions/config.yml");
        this.reloadConfig();
    }

    @SuppressWarnings({"ConstantValue", "RedundantIfStatement"})
    private boolean checkSupportTranslatable() {
        ItemStack item = new ItemStack(Material.STONE);
        if (supportTranslatableAdventure) {
            if (item instanceof net.kyori.adventure.translation.Translatable) {
                return true;
            }
        }
        if (supportTranslatable) {
            if (item instanceof org.bukkit.Translatable) {
                return true;
            }
        }
        return false;
    }

    public void reloadConfig() {
        if (!checkSupportTranslatable()) {
            info("当前服务端不支持 Translatable 特性，不启用客户端资源下载器");
            return;
        }
        if (!configFile.exists()) {
            plugin.saveResource("versions.yml", configFile);
        }
        YamlConfiguration config = ConfigUtils.load(configFile);
        String gameVersion = config.getString("game-version", "auto");
        if (gameVersion.equals("auto")) {
            this.minecraftVersion = Versioning.getMinecraftVersion();
        } else {
            this.minecraftVersion = gameVersion;
        }
        String gameLanguage = config.getString("game-language", "zh-cn");
        String downloadSourceStr = config.getString("download-source", "auto").toLowerCase();
        switch (downloadSourceStr) {
            case "auto":
                if (Locale.getDefault().getCountry().equals("CN")) {
                    downloadSource = BMCLAPISource.INSTANCE;
                } else {
                    downloadSource = MojangSource.INSTANCE;
                }
                break;
            case "bmclapi":
                downloadSource = BMCLAPISource.INSTANCE;
                break;
            case "mojang":
            default:
                downloadSource = MojangSource.INSTANCE;
                break;
        }
        info("使用下载源 " + downloadSource.getName());
        try {
            reloadLanguage(gameLanguage);
        } catch (Throwable t) {
            warn("下载语言文件时出现异常", t);
        }
    }

    public void reloadLanguage(String code) {
        File folder = new File(plugin.getDataFolder(), "versions/" + minecraftVersion);
        if (!folder.exists()) {
            Util.mkdirs(folder);
        }
        info("指定的 Minecraft 版本: " + minecraftVersion);
        File indexFile = new File(folder, "assetIndex.json");
        JsonObject assetIndexJson;
        if (!indexFile.exists()) {
            info("找不到 assetIndex.json，正在通过版本 json 获取");
            File versionJsonFile = new File(folder, minecraftVersion + ".json");
            JsonObject versionJson;
            if (!versionJsonFile.exists()) {
                info("找不到版本 json 文件 (" + minecraftVersion + ".json)，正在下载");
                JsonObject manifest = downloadSource.getVersionsManifest();
                versionJson = downloadSource.getVersionJson(manifest, minecraftVersion);
                try (FileWriter writer = new FileWriter(versionJsonFile)) {
                    gson.toJson(versionJson, writer);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                try (FileReader reader = new FileReader(versionJsonFile)) {
                    versionJson = gson.fromJson(reader, JsonObject.class);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            info("正在下载 assetIndex.json");
            assetIndexJson = downloadSource.getAssetIndex(versionJson);
            try (FileWriter writer = new FileWriter(indexFile)) {
                gson.toJson(assetIndexJson, writer);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            try (FileReader reader = new FileReader(indexFile)) {
                assetIndexJson = gson.fromJson(reader, JsonObject.class);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        reloadLanguageFromAsset(folder, assetIndexJson, code);
    }

    private void reloadLanguageFromAsset(File folder, JsonObject assetIndexJson, String code) {
        JsonObject objects = assetIndexJson.get("objects").getAsJsonObject();
        Pair<String, String> pair = getHash(objects,
                "minecraft/lang/" + code.replace("-", "_") + ".lang",
                "minecraft/lang/" + code.replace("-", "_") + ".json",
                "minecraft/lang/" + code + ".lang",
                "minecraft/lang/" + code + ".json");
        translation.clear();
        if (pair == null) {
            warn("无法在资源索引中找到名为 " + code + " 的语言文件");
        } else {
            String fileName = pair.key();
            String hash = pair.value();
            File objectFolder = new File(folder, "objects/" + hash.substring(0, 2));
            if (!objectFolder.exists()) {
                Util.mkdirs(objectFolder);
            }
            File file = new File(objectFolder, hash);
            if (!file.exists()) {
                info("正在下载语言文件 " + fileName + " (" + hash + ")");
                downloadSource.downloadAsset(file, hash);
            }
            if (fileName.endsWith(".json")) {
                JsonObject data;
                try (FileReader reader = new FileReader(file)) {
                    data = gson.fromJson(reader, JsonObject.class);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                for (String key : data.keySet()) {
                    JsonElement element = data.get(key);
                    if (element != null && element.isJsonPrimitive()) {
                        String value = element.getAsString();
                        translation.put(key, value);
                    }
                }
            }
            if (fileName.endsWith(".lang")) {
                Properties data = new Properties();
                try (FileReader reader = new FileReader(file)) {
                    data.load(reader);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                for (Map.Entry<Object, Object> entry : data.entrySet()) {
                    String key = String.valueOf(entry.getKey());
                    String value = String.valueOf(entry.getValue());
                    translation.put(key, value);
                }
            }
            File extraLanguageFile = new File(folder, "lang.json");
            if (extraLanguageFile.exists()) {
                JsonObject data;
                try (FileReader reader = new FileReader(extraLanguageFile)) {
                    data = gson.fromJson(reader, JsonObject.class);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                for (String key : data.keySet()) {
                    JsonElement element = data.get(key);
                    if (element != null && element.isJsonPrimitive()) {
                        String value = element.getAsString();
                        translation.put(key, value);
                    }
                }
            }
            info("从语言文件加载了 " + translation.size() + " 个项");
        }
    }

    @Nullable
    public String getOrNull(String translationKey) {
        return translation.get(translationKey);
    }

    @NotNull
    public String get(String translationKey) {
        return translation.getOrDefault(translationKey, translationKey);
    }

    private static Pair<String, String> getHash(JsonObject objects, String... files) {
        for (String key : objects.keySet()) {
            boolean match = false;
            for (String file : files) {
                if (file.equalsIgnoreCase(key)) {
                    match = true;
                    break;
                }
            }
            if (!match) continue;
            JsonElement element = objects.get(key);
            if (element == null || !element.isJsonObject()) continue;
            JsonObject object = element.getAsJsonObject();
            return Pair.of(key, object.get("hash").getAsString());
        }
        return null;
    }

    public static I18nManager inst() {
        return instanceOf(I18nManager.class);
    }
}
