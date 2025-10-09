package top.mrxiaom.sweet.playermarket.api;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.utils.ListPair;
import top.mrxiaom.sweet.playermarket.data.EnumMarketType;
import top.mrxiaom.sweet.playermarket.data.MarketItem;

public interface IShopAdapterFactory {
    /**
     * 商店适配器工厂的ID，默认为类引用路径，请保证唯一性
     */
    default String id() {
        return this.getClass().getName();
    }

    /**
     * 新建商品时，向商品的 params 添加内容
     * @param type 商品类型
     * @param params 商品 params
     */
    default void applyToParams(EnumMarketType type, ConfigurationSection params) {
        params.set("adapter.factory-id", id());
    }

    /**
     * 更新在 物品名称、物品Lore 中使用的变量
     * @param item 商品实例
     * @param player 浏览玩家
     * @param r 变量列表
     */
    void updateReplacements(@NotNull MarketItem item, @NotNull Player player, @NotNull ListPair<String, Object> r);

    /**
     * 对已经替换完变量的商品展示图标进行后处理
     * @param item 商品实例
     * @param player 浏览玩家
     * @param r 变量列表
     * @param originalItem 原始图标物品
     * @return 处理后图标物品
     */
    @NotNull
    default ItemStack postProcessIcon(@NotNull MarketItem item, @NotNull Player player, @NotNull ListPair<String, Object> r, @NotNull ItemStack originalItem) {
        return originalItem;
    }

    /**
     * 根据商品配置获取出售商店适配器
     * @param item 商品配置
     * @param player 要购买该商品的玩家
     * @return 适配器实现，返回 <code>null</code> 代表数据损坏或不支持
     */
    @Nullable
    IShopSellConfirmAdapter getSellConfirmAdapter(@NotNull MarketItem item, @NotNull Player player);
    /**
     * 根据商品配置获取收购商店适配器
     * @param item 商品配置
     * @param player 要卖出该商品的玩家
     * @return 适配器实现，返回 <code>null</code> 代表数据损坏或不支持
     */
    @Nullable
    IShopBuyConfirmAdapter getBuyConfirmAdapter(@NotNull MarketItem item, @NotNull Player player);
}
