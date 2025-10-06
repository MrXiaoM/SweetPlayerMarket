package top.mrxiaom.sweet.playermarket.func;

import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.api.IShopAdapterFactory;

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
}
