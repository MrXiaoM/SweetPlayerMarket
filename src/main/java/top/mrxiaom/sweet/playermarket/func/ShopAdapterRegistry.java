package top.mrxiaom.sweet.playermarket.func;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.ListPair;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.api.IShopAdapterFactory;
import top.mrxiaom.sweet.playermarket.api.IShopBuyConfirmAdapter;
import top.mrxiaom.sweet.playermarket.api.IShopSellConfirmAdapter;
import top.mrxiaom.sweet.playermarket.data.MarketItem;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@AutoRegister
public class ShopAdapterRegistry extends AbstractModule {
    private final Map<String, IShopAdapterFactory> factoryMap = new HashMap<>();

    public ShopAdapterRegistry(SweetPlayerMarket plugin) {
        super(plugin);
        register();
    }

    /**
     * 获取商品适配器工厂实例
     *
     * @param factoryId 商品适配器ID
     * @return 不存在时返回<code>null</code>
     */
    @Nullable
    public IShopAdapterFactory getById(String factoryId) {
        return factoryMap.get(factoryId);
    }

    /**
     * 根据商品实例，获取商品适配器工厂实例
     * @param marketItem 商品实例
     */
    @NotNull
    public Entry getByMarketItem(@NotNull MarketItem marketItem) {
        ConfigurationSection params = marketItem.params();
        // 检查商品适配器设置
        String factoryId = params.getString("adapter.factory-id", null);
        if (factoryId != null) {
            return new Entry(true, getById(factoryId));
        } else {
            return new Entry(false, null);
        }
    }

    /**
     * 注册商品适配器工厂，如果适配器工厂ID重复，将会覆盖已有工厂
     *
     * @param factory 商品适配器工厂实例
     */
    public void register(IShopAdapterFactory factory) {
        factoryMap.put(factory.id(), factory);
    }

    /**
     * 按ID注销商品适配器工厂
     *
     * @param factory 商品适配器工厂实例
     */
    public void unregister(IShopAdapterFactory factory) {
        unregister(factory.id());
    }

    /**
     * 按ID注销商品适配器工厂
     *
     * @param factoryId 商品适配器工厂ID
     */
    public void unregister(String factoryId) {
        factoryMap.remove(factoryId);
    }

    public static void unregisterAll(String... adapterIds) {
        ShopAdapterRegistry registry = getOrNull(ShopAdapterRegistry.class);
        if (registry != null) for (String id : adapterIds) {
            registry.unregister(id);
        }
    }

    public static void unregisterAll(Collection<String> adapterIds) {
        ShopAdapterRegistry registry = getOrNull(ShopAdapterRegistry.class);
        if (registry != null) for (String id : adapterIds) {
            registry.unregister(id);
        }
    }

    public static ShopAdapterRegistry inst() {
        return instanceOf(ShopAdapterRegistry.class);
    }

    public static class Entry {
        private final boolean hasFactoryParams;
        private final IShopAdapterFactory factory;

        public Entry(boolean hasFactoryParams, @Nullable IShopAdapterFactory factory) {
            this.hasFactoryParams = hasFactoryParams;
            this.factory = factory;
        }

        public boolean hasFactoryParams() {
            return hasFactoryParams;
        }

        @Nullable
        public IShopAdapterFactory getFactory() {
            return factory;
        }

        /**
         * 更新在 物品名称、物品Lore 中使用的变量
         * @see IShopAdapterFactory#updateReplacements(MarketItem, Player, ListPair)
         */
        public void updateReplacements(@NotNull MarketItem item, @NotNull Player player, @NotNull ListPair<String, Object> r) {
            if (factory == null) return;
            factory.updateReplacements(item, player, r);
        }

        /**
         * 对已经替换完变量的商品展示图标进行后处理
         */
        @NotNull
        public ItemStack postProcessIcon(@NotNull MarketItem item, @NotNull Player player, @NotNull ListPair<String, Object> r, @NotNull ItemStack originalItem) {
            return factory == null ? originalItem : factory.postProcessIcon(item, player, r, originalItem);
        }

        /**
         * 根据商品配置获取出售商店适配器
         * @see IShopAdapterFactory#getSellConfirmAdapter(MarketItem, Player)
         */
        @Nullable
        public IShopSellConfirmAdapter getSellConfirmAdapter(@NotNull MarketItem item, @NotNull Player player) {
            return factory == null ? null : factory.getSellConfirmAdapter(item, player);
        }
        /**
         * 根据商品配置获取收购商店适配器
         * @see IShopAdapterFactory#getBuyConfirmAdapter(MarketItem, Player)
         */
        @Nullable
        public IShopBuyConfirmAdapter getBuyConfirmAdapter(@NotNull MarketItem item, @NotNull Player player) {
            return factory == null ? null : factory.getBuyConfirmAdapter(item, player);
        }
    }
}
