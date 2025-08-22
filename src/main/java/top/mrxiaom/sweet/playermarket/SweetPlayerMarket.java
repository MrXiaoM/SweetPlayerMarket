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
import top.mrxiaom.pluginbase.actions.ActionProviders;
import top.mrxiaom.pluginbase.func.LanguageManager;
import top.mrxiaom.pluginbase.paper.PaperFactory;
import top.mrxiaom.pluginbase.resolver.DefaultLibraryResolver;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.pluginbase.utils.inventory.InventoryFactory;
import top.mrxiaom.pluginbase.utils.item.ItemEditor;
import top.mrxiaom.pluginbase.utils.scheduler.FoliaLibScheduler;
import top.mrxiaom.sweet.playermarket.actions.*;
import top.mrxiaom.sweet.playermarket.data.DisplayNames;
import top.mrxiaom.sweet.playermarket.database.MarketplaceDatabase;
import top.mrxiaom.sweet.playermarket.economy.*;
import top.mrxiaom.sweet.playermarket.utils.Utils;

import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

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
    private DisplayNames displayNames;
    private DateTimeFormatter datetimeFormatter;
    private String datetimeInfinite;

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
    public IEconomy parseEconomy(@Nullable String str) {
        if (str == null) {
            return null;
        }
        if (str.equals("Vault")) {
            return getVault();
        }
        if (str.equals("PlayerPoints")) {
            return getPlayerPoints();
        }
        if (str.startsWith("MPoints:") && str.length() > 8) {
            IEconomyWithSign withSign = getMPoints();
            if (withSign != null) {
                return withSign.of(str.substring(8));
            }
        }
        return null;
    }

    public DisplayNames displayNames() {
        return displayNames;
    }

    @NotNull
    public MarketplaceDatabase getMarketplace() {
        return marketplaceDatabase;
    }

    @Override
    public @NotNull ItemEditor initItemEditor() {
        return PaperFactory.createItemEditor();
    }

    @Override
    public @NotNull InventoryFactory initInventoryFactory() {
        return PaperFactory.createInventoryFactory();
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

        LanguageManager.inst()
                .setLangFile("messages.yml")
                .register(Messages.class)
                .register(Messages.Command.class)
                .register(Messages.TabComplete.class)
                .register(Messages.Gui.class);

        initEconomy();

        ActionProviders.registerActionProviders(
                ActionPage.PROVIDER, ActionRefresh.PROVIDER,
                ActionSearchCurrency.PROVIDER, ActionSearchNotice.PROVIDER,
                ActionSearchOutdate.PROVIDER, ActionSearchOutOfStock.PROVIDER,
                ActionSearchSort.PROVIDER, ActionSearchType.PROVIDER,
                ActionConfirmCount.PROVIDER
        );
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
        if (displayNames == null) {
            displayNames = DisplayNames.inst();
        }
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
        try {
            String string = config.getString("datetime.format", "yyyy-MM-dd HH:mm:ss");
            datetimeFormatter = DateTimeFormatter.ofPattern(string);
        } catch (DateTimeParseException e) {
            datetimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            warn("加载 datetime.format 时发现格式错误，已切换回默认格式");
        }
        datetimeInfinite = config.getString("datetime.infinite", "无期限");
    }

    @Override
    protected void afterEnable() {
        getLogger().info("SweetPlayerMarket 加载完毕");
    }

    @NotNull
    public String toString(@Nullable LocalDateTime dateTime) {
        if (dateTime == null) {
            return datetimeInfinite;
        } else {
            return dateTime.format(datetimeFormatter);
        }
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

    public Player getPlayer(String key) {
        if (onlineMode) {
            UUID uuid = Utils.parseUUID(key);
            return Util.getOnlinePlayer(uuid).orElse(null);
        } else {
            return Util.getOnlinePlayer(key).orElse(null);
        }
    }

    public OfflinePlayer getOfflinePlayer(String key) {
        if (onlineMode) {
            UUID uuid = Utils.parseUUID(key);
            return Util.getOfflinePlayer(uuid).orElse(null);
        } else {
            return Util.getOfflinePlayer(key).orElse(null);
        }
    }
}
