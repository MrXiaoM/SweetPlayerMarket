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
import top.mrxiaom.pluginbase.utils.ClassLoaderWrapper;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.pluginbase.utils.inventory.InventoryFactory;
import top.mrxiaom.pluginbase.utils.item.ItemEditor;
import top.mrxiaom.pluginbase.utils.scheduler.FoliaLibScheduler;
import top.mrxiaom.sweet.playermarket.actions.*;
import top.mrxiaom.sweet.playermarket.api.IEconomyResolver;
import top.mrxiaom.sweet.playermarket.api.ItemTagResolver;
import top.mrxiaom.sweet.playermarket.api.MarketAPI;
import top.mrxiaom.sweet.playermarket.data.DisplayNames;
import top.mrxiaom.sweet.playermarket.data.MarketItem;
import top.mrxiaom.sweet.playermarket.data.MarketItemBuilder;
import top.mrxiaom.sweet.playermarket.database.MarketplaceDatabase;
import top.mrxiaom.sweet.playermarket.economy.*;
import top.mrxiaom.sweet.playermarket.utils.Utils;

import java.io.File;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Consumer;

public class SweetPlayerMarket extends BukkitPlugin {
    public static SweetPlayerMarket getInstance() {
        return (SweetPlayerMarket) BukkitPlugin.getInstance();
    }

    public static MarketAPI api() {
        return getInstance().api;
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
        File librariesDir = ClassLoaderWrapper.isSupportLibraryLoader
                ? new File("libraries")
                : new File(this.getDataFolder(), "libraries");
        DefaultLibraryResolver resolver = new DefaultLibraryResolver(getLogger(), librariesDir);

        resolver.addResolvedLibrary(BuildConstants.RESOLVED_LIBRARIES);

        List<URL> libraries = resolver.doResolve();
        info("正在添加 " + libraries.size() + " 个依赖库到类加载器");
        for (URL library : libraries) {
            this.classLoader.addURL(library);
        }

        economyResolvers.add(new VaultEconomy.Resolver(this));
        economyResolvers.add(new PlayerPointsEconomy.Resolver(this));
        economyResolvers.add(new MPointsEconomy.Resolver(this));
    }
    private final MarketAPI api = new API();
    private final List<IEconomyResolver> economyResolvers = new ArrayList<>();
    private boolean onlineMode;
    private IEconomy vault;
    private IEconomy playerPoints;
    private IEconomyWithSign mPoints;
    private ItemTagResolver itemTagResolver = item -> "default"; // TODO 商品分类
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
        for (IEconomyResolver resolver : economyResolvers) {
            IEconomy economy = resolver.parse(str);
            if (economy != null) {
                return economy;
            }
        }
        return null;
    }

    public List<IEconomyResolver> economyResolvers() {
        return Collections.unmodifiableList(economyResolvers);
    }

    public ItemTagResolver itemTagResolver() {
        return itemTagResolver;
    }

    public void itemTagResolver(ItemTagResolver itemTagResolver) {
        this.itemTagResolver = itemTagResolver;
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
                ActionSearchSort.PROVIDER, ActionSearchTag.PROVIDER,
                ActionSearchType.PROVIDER, ActionConfirmCount.PROVIDER,
                ActionOpenConfirmGui.PROVIDER, ActionClaim.PROVIDER,
                ActionTakeDown.PROVIDER, ActionTakeDownByAdmin.PROVIDER,
                ActionDeployCount.PROVIDER, ActionDeployPrice.PROVIDER, ActionDeployCurrency.PROVIDER
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

    public class API implements MarketAPI {
        private API() {}

        @Override
        public void registerEconomy(IEconomyResolver resolver) {
            economyResolvers.add(resolver);
            economyResolvers.sort(Comparator.comparing(IEconomyResolver::priority));
        }

        @Override
        public MarketItem deploy(Player owner, Consumer<MarketItemBuilder> consumer) {
            String playerId = getKey(owner);
            String playerName = owner.getName();
            return deploy(playerId, playerName, consumer);
        }

        @Override
        public MarketItem deploy(String playerId, String playerName, Consumer<MarketItemBuilder> consumer) {
            try (Connection conn = getConnection()) {
                MarketplaceDatabase db = getMarketplace();
                String shopId = db.createNewId(conn);
                if (shopId == null) {
                    throw new IllegalStateException("无法创建新的商品ID，请稍后再试");
                }
                MarketItemBuilder builder = MarketItem.builder(shopId, playerId, playerName);
                consumer.accept(builder);
                MarketItem item = builder.build(itemTagResolver);
                db.putItem(conn, item);
                return item;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
