package top.mrxiaom.sweet.playermarket.api;

import org.bukkit.entity.Player;
import top.mrxiaom.sweet.playermarket.data.MarketItem;
import top.mrxiaom.sweet.playermarket.data.MarketItemBuilder;

import java.util.function.Consumer;

/**
 * 全球市场插件 API 接口
 */
public interface MarketAPI {
    /**
     * 将商品上架到全球市场
     * @param owner 卖家
     * @param consumer MarketItemBuilder 接收器，用于构建商品信息
     * @return 提交到数据库的商品实例
     * @throws RuntimeException 访问数据库时抛出异常 (数据库异常在 <code>cause</code>)
     * @throws IllegalStateException 创建商品ID失败时抛出异常
     */
    MarketItem deploy(Player owner, Consumer<MarketItemBuilder> consumer);

    /**
     * 将商品上架到全球市场
     * @param playerId 卖家ID，通过 <code>plugin.getKey(Player)</code> 获得
     * @param playerName 卖家名字
     * @param consumer MarketItemBuilder 接收器，用于构建商品信息
     * @return 提交到数据库的商品实例
     * @throws RuntimeException 访问数据库时抛出异常 (数据库异常在 <code>cause</code>)
     * @throws IllegalStateException 创建商品ID失败时抛出异常
     */
    MarketItem deploy(String playerId, String playerName, Consumer<MarketItemBuilder> consumer);
}
