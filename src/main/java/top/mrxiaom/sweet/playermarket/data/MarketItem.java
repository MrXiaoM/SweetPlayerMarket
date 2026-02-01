package top.mrxiaom.sweet.playermarket.data;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.utils.ListPair;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.economy.IEconomy;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 全球市场商品信息
 */
public class MarketItem {
    private final @NotNull String shopId;
    private final @NotNull String playerId;
    private final @NotNull EnumMarketType type;
    private final @NotNull LocalDateTime createTime;
    private final @Nullable LocalDateTime outdateTime;
    private final @NotNull String currencyName;
    private final @Nullable IEconomy currency;
    private final double price;
    private final int amount;
    private final int noticeFlag;
    private final @NotNull String tag;

    private final @NotNull ItemStack item;
    private final @NotNull String playerName;
    private final @NotNull ConfigurationSection params;
    public MarketItem(
            @NotNull String shopId,
            @NotNull String playerId,
            @NotNull EnumMarketType type,
            @NotNull LocalDateTime createTime,
            @Nullable LocalDateTime outdateTime,
            @NotNull String currencyName,
            @Nullable IEconomy currency,
            double price,
            int amount,
            int noticeFlag,
            @NotNull String tag,
            @NotNull YamlConfiguration data
    ) {
        this.shopId = shopId;
        this.playerId = playerId;
        this.type = type;
        this.createTime = createTime;
        this.outdateTime = outdateTime;
        this.currencyName = currencyName;
        this.currency = currency;
        this.price = price;
        this.amount = amount;
        this.noticeFlag = noticeFlag;
        this.tag = tag;

        ItemStack item = data.getItemStack("item");
        if (item == null) throw new IllegalStateException("`item` not found!");
        this.item = item;

        String playerName = data.getString("player.name");
        if (playerName == null) throw new IllegalStateException("`player.name` not found!");
        this.playerName = playerName;

        ConfigurationSection params = data.getConfigurationSection("params");
        this.params = params != null ? params : new MemoryConfiguration();
    }

    /**
     * 商品ID
     */
    public @NotNull String shopId() {
        return shopId;
    }

    /**
     * 玩家ID
     */
    public @NotNull String playerId() {
        return playerId;
    }

    /**
     * 玩家名称
     */
    public @NotNull String playerName() {
        return playerName;
    }

    /**
     * 商品类型
     */
    public @NotNull EnumMarketType type() {
        return type;
    }

    /**
     * 商品创建时间
     */
    public @NotNull LocalDateTime createTime() {
        return createTime;
    }

    /**
     * 商品到期时间
     */
    public @Nullable LocalDateTime outdateTime() {
        return outdateTime;
    }

    public boolean isOutdated(LocalDateTime now) {
        if (outdateTime == null) return false;
        return now.isAfter(outdateTime);
    }

    /**
     * 商品货币
     */
    public @Nullable IEconomy currency() {
        return currency;
    }

    /**
     * 商品货币名
     */
    public @NotNull String currencyName() {
        return currencyName;
    }

    /**
     * 商品价格
     */
    public double price() {
        return price;
    }

    /**
     * 商品数量
     */
    public int amount() {
        return amount;
    }

    /**
     * 提醒标记
     */
    public int noticeFlag() {
        return noticeFlag;
    }

    /**
     * 商品标签
     */
    public @NotNull String tag() {
        return tag;
    }

    /**
     * 物品
     */
    public @NotNull ItemStack item() {
        return item.clone();
    }

    /**
     * 商品额外参数
     */
    public @NotNull ConfigurationSection params() {
        return params;
    }

    /**
     * 生成用于数据表的 data 配置
     */
    public @NotNull YamlConfiguration data() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("player.name", playerName);
        config.set("item", item);
        config.set("params", params);
        return config;
    }

    public ListPair<String, Object> replacements(DisplayNames displayNames, Player player) {
        ListPair<String, Object> r = new ListPair<>();
        r.add("%creator%", playerName);
        r.add("%item%", displayNames.getDisplayName(item, player));
        r.add("%shop_type%", displayNames.getMarketTypeName(type));
        r.add("%shop_type_raw%", type.name().toLowerCase());
        r.add("%currency%", displayNames.getCurrencyName(currencyName));
        r.add("%currency_raw%", currencyName);
        r.add("%price%", displayNames.formatMoney(price));
        r.add("%price_raw%", price);
        r.add("%amount%", amount);
        r.add("%tag%", tag);
        return r;
    }

    public MarketItemBuilder toBuilder() {
        return builder(shopId, playerId, playerName)
                .type(type)
                .createTime(createTime)
                .outdateTime(outdateTime)
                .currency(currency, currencyName)
                .price(price)
                .amount(amount)
                .noticeFlag(noticeFlag)
                .tag(tag)
                .item(item)
                .params(params);
    }

    public static MarketItemBuilder builder(String shopId, String playerId, String playerName) {
        return new MarketItemBuilder(shopId, playerId, playerName);
    }

    public static MarketItemBuilder builder(String playerId, String playerName) {
        return builder(UUID.randomUUID().toString(), playerId, playerName);
    }

    public static MarketItemBuilder builder(String shopId, Player player) {
        String playerId = SweetPlayerMarket.getInstance().getKey(player);
        return builder(shopId, playerId, player.getName());
    }

    public static MarketItemBuilder builder(Player player) {
        String playerId = SweetPlayerMarket.getInstance().getKey(player);
        return builder(playerId, player.getName());
    }

    public static MarketItemBuilder builder(String serverCustomName) {
        return builder("#server#", serverCustomName);
    }
}
