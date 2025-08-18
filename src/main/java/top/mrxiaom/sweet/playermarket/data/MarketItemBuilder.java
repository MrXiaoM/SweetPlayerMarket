package top.mrxiaom.sweet.playermarket.data;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.sweet.playermarket.economy.IEconomy;

import java.time.LocalDateTime;

/**
 * 全球市场商品构建器
 * @see MarketItem#builder(Player)
 */
public class MarketItemBuilder {
    private @NotNull final String shopId;
    private @NotNull final String playerId;
    private @NotNull String playerName;
    private EnumMarketType type;
    private @NotNull LocalDateTime createTime = LocalDateTime.now();
    private @Nullable LocalDateTime outdateTime = null;
    private IEconomy currency;
    private String currencyName;
    private Double price;
    private int amount = 1;
    private @NotNull String tag = "default";
    private ItemStack item;
    private @NotNull ConfigurationSection params = new MemoryConfiguration();

    protected MarketItemBuilder(@NotNull String shopId, @NotNull String playerId, @NotNull String playerName) {
        this.shopId = shopId;
        this.playerId = playerId;
        this.playerName = playerName;
    }

    /**
     * @see MarketItem#shopId()
     */
    public @NotNull String shopId() {
        return shopId;
    }

    /**
     * @see MarketItem#playerId()
     */
    public @NotNull String playerId() {
        return playerId;
    }

    /**
     * @see MarketItem#playerName()
     */
    public @NotNull String playerName() {
        return playerName;
    }

    /**
     * @see MarketItem#type()
     */
    public EnumMarketType type() {
        return type;
    }

    /**
     * @see MarketItem#createTime()
     */
    public @NotNull LocalDateTime createTime() {
        return createTime;
    }

    /**
     * @see MarketItem#outdateTime()
     */
    public @Nullable LocalDateTime outdateTime() {
        return outdateTime;
    }

    /**
     * @see MarketItem#currency()
     */
    public IEconomy currency() {
        return currency;
    }

    /**
     * @see MarketItem#currencyName()
     */
    public String currencyName() {
        return currencyName;
    }

    /**
     * @see MarketItem#price()
     */
    public Double price() {
        return price;
    }

    /**
     * @see MarketItem#amount()
     */
    public int amount() {
        return amount;
    }

    /**
     * @see MarketItem#tag()
     */
    public @NotNull String tag() {
        return tag;
    }

    /**
     * @see MarketItem#item()
     */
    public ItemStack item() {
        return item;
    }

    /**
     * @see MarketItem#params()
     */
    public @NotNull ConfigurationSection params() {
        return params;
    }

    /**
     * @see MarketItem#playerName()
     */
    public MarketItemBuilder playerName(String playerName) {
        this.playerName = playerName;
        return this;
    }

    /**
     * @see MarketItem#type()
     */
    public MarketItemBuilder type(EnumMarketType type) {
        this.type = type;
        return this;
    }

    /**
     * @see MarketItem#createTime()
     */
    public MarketItemBuilder createTime(@NotNull LocalDateTime createTime) {
        this.createTime = createTime;
        return this;
    }

    /**
     * @see MarketItem#outdateTime()
     */
    public MarketItemBuilder outdateTime(@Nullable LocalDateTime outdateTime) {
        this.outdateTime = outdateTime;
        return this;
    }

    /**
     * @see MarketItem#currency()
     */
    public MarketItemBuilder currency(@Nullable IEconomy currency, @NotNull String currencyName) {
        if (currency == null) {
            this.currencyName = currencyName;
        } else {
            this.currencyName = currency.id();
            this.currency = currency;
        }
        return this;
    }

    /**
     * @see MarketItem#currency()
     */
    public MarketItemBuilder currency(@NotNull IEconomy currency) {
        this.currencyName = currency.id();
        this.currency = currency;
        return this;
    }

    /**
     * @see MarketItem#currencyName()
     */
    public MarketItemBuilder currencyName(@NotNull String currencyName) {
        this.currencyName = currencyName;
        return this;
    }

    /**
     * @see MarketItem#price()
     */
    public MarketItemBuilder price(double price) {
        this.price = price;
        return this;
    }

    /**
     * @see MarketItem#amount()
     */
    public MarketItemBuilder amount(int amount) {
        this.amount = amount;
        return this;
    }

    /**
     * @see MarketItem#tag()
     */
    public MarketItemBuilder tag(@NotNull String tag) {
        this.tag = tag;
        return this;
    }

    /**
     * @see MarketItem#item()
     */
    public MarketItemBuilder item(ItemStack item) {
        this.item = item;
        return this;
    }

    /**
     * @see MarketItem#params()
     */
    public MarketItemBuilder params(@NotNull ConfigurationSection params) {
        this.params = params;
        return this;
    }

    /**
     * 构建商品
     */
    public MarketItem build() {
        if (type == null) throw new IllegalArgumentException("'type' must be input!");
        if (currencyName == null) throw new IllegalArgumentException("'currencyName' must be input!");
        if (price == null) throw new IllegalArgumentException("'price' must be input!");
        if (item == null) throw new IllegalArgumentException("'item' must be input!");

        if (!params.contains("original-amount")) {
            params.set("original-amount", amount);
        }

        YamlConfiguration data = new YamlConfiguration();
        data.set("player.name", playerName);
        data.set("item", item);
        data.set("params", params);

        return new MarketItem(shopId, playerId, type, createTime, outdateTime, currencyName, currency, price, amount, tag, data);
    }
}
