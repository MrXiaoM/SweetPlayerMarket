package top.mrxiaom.sweet.playermarket;
        
import de.tr7zw.changeme.nbtapi.utils.MinecraftVersion;
import me.yic.mpoints.MPointsAPI;
import net.milkbowl.vault.economy.Economy;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.BukkitPlugin;
import top.mrxiaom.pluginbase.economy.IEconomy;
import top.mrxiaom.pluginbase.economy.VaultEconomy;
import top.mrxiaom.pluginbase.resolver.DefaultLibraryResolver;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.pluginbase.utils.scheduler.FoliaLibScheduler;
import top.mrxiaom.sweet.playermarket.database.MarketplaceDatabase;
import top.mrxiaom.sweet.playermarket.economy.IEconomyWithSign;
import top.mrxiaom.sweet.playermarket.economy.MPointsEconomy;
import top.mrxiaom.sweet.playermarket.economy.PlayerPointsEconomy;

import java.io.File;
import java.net.URL;
import java.util.List;

public class SweetPlayerMarket extends BukkitPlugin {
    public static SweetPlayerMarket getInstance() {
        return (SweetPlayerMarket) BukkitPlugin.getInstance();
    }

    public SweetPlayerMarket() throws Exception {
        super(options()
                .adventure(true)
                .bungee(true)
                .database(true)
                .reconnectDatabaseWhenReloadConfig(false)
                .scanIgnore("top.mrxiaom.sweet.playermarket.libs")
        );
        this.scheduler = new FoliaLibScheduler(this);

        info("正在检查依赖库状态");
        File librariesDir = new File(this.getDataFolder(), "libraries");
        DefaultLibraryResolver resolver = new DefaultLibraryResolver(getLogger(), librariesDir);

        resolver.addLibrary(BuildConstants.LIBRARIES);

        List<URL> libraries = resolver.doResolve();
        info("正在添加 " + libraries.size() + " 个依赖库到类加载器");
        for (URL library : libraries) {
            this.classLoader.addURL(library);
        }
    }
    private boolean onlineMode;
    private IEconomy vault;
    private IEconomy playerPoints;
    private IEconomyWithSign mPoints;
    private MarketplaceDatabase marketplaceDatabase;

    public boolean isOnlineMode() {
        return onlineMode;
    }

    @Nullable
    public IEconomy getVault() {
        return vault;
    }
    @Nullable
    public IEconomy getPlayerPoints() {
        return playerPoints;
    }
    @Nullable
    public IEconomyWithSign getMPoints() {
        return mPoints;
    }
    @Nullable
    public IEconomy parseEconomy(String str) {
        if (str.equals("Vault")) {
            return getVault();
        }
        if (str.equals("PlayerPoints")) {
            return getPlayerPoints();
        }
        if (str.startsWith("MPoints:")) {
            IEconomyWithSign withSign = getMPoints();
            if (withSign != null) {
                return withSign.of(str.substring(8));
            }
        }
        return null;
    }
    @Nullable
    public String economyToString(IEconomy economy) {
        if (economy instanceof VaultEconomy) {
            return "Vault";
        }
        if (economy instanceof PlayerPointsEconomy) {
            return "PlayerPoints";
        }
        if (economy instanceof MPointsEconomy) {
            return "MPoints:" + ((MPointsEconomy) economy).sign();
        }
        return null;
    }

    @NotNull
    public MarketplaceDatabase getMarketplace() {
        return marketplaceDatabase;
    }

    @Override
    protected void beforeLoad() {
        MinecraftVersion.replaceLogger(getLogger());
        MinecraftVersion.disableUpdateCheck();
        MinecraftVersion.disableBStats();
        MinecraftVersion.getVersion();
    }

    @Override
    protected void beforeEnable() {
        options.registerDatabase(
                this.marketplaceDatabase = new MarketplaceDatabase(this)
        );

        initEconomy();
    }

    private void initEconomy() {
        try {
            if (Util.isPresent("net.milkbowl.vault.economy.Economy")) {
                RegisteredServiceProvider<Economy> service = Bukkit.getServicesManager().getRegistration(Economy.class);
                Economy provider = service == null ? null : service.getProvider();
                if (provider != null) {
                    vault = new VaultEconomy(provider);
                } else {
                    warn("已发现 Vault，但经济插件未加载，无法挂钩经济插件");
                }
            }
        } catch (NoClassDefFoundError ignored) {
        }
        try {
            if (Util.isPresent("org.black_ixx.playerpoints.PlayerPointsAPI")) {
                PlayerPointsAPI api = PlayerPoints.getInstance().getAPI();
                playerPoints = new PlayerPointsEconomy(api);
            }
        } catch (NoClassDefFoundError ignored) {
        }
        try {
            if (Util.isPresent("me.yic.mpoints.MPointsAPI")) {
                mPoints = new MPointsEconomy(new MPointsAPI(), null);
            }
        } catch (NoClassDefFoundError ignored) {
        }
        if (vault != null) info("已挂钩经济插件 " + vault.getName());
        if (playerPoints != null) info("已挂钩经济插件 " + playerPoints.getName());
        if (mPoints != null) info("已挂钩经济插件 " + mPoints.getName());
    }

    @Override
    protected void beforeReloadConfig(FileConfiguration config) {
        String onlineMode = config.getString("online-mode", "auto").toLowerCase();
        switch (onlineMode) {
            case "true":
                this.onlineMode = true;
                break;
            case "false":
                this.onlineMode = false;
                break;
            case "auto":
            default:
                this.onlineMode = Bukkit.getServer().getOnlineMode();
                break;
        }
    }

    @Override
    protected void afterEnable() {
        getLogger().info("SweetPlayerMarket 加载完毕");
    }

    public String getKey(Player player) {
        if (onlineMode) {
            return player.getUniqueId().toString();
        } else {
            return player.getName();
        }
    }

    @Nullable
    public String getOfflineKey(OfflinePlayer player) {
        if (onlineMode) {
            return player.getUniqueId().toString();
        } else {
            return player.getName();
        }
    }
}
