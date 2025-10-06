package top.mrxiaom.sweet.playermarket.api;

import org.jetbrains.annotations.Nullable;

public interface IShopBuyConfirmAdapter {
    /**
     * 获取玩家卖出这么多份可以获得多少货币
     * @param count 卖出份数
     */
    double getTotalMoney(int count);
    /**
     * 检查玩家是否有足够多的物品并拿走
     * @param count 卖出份数
     * @return 卖出的总数量，如果返回<code>null</code>则代表玩家没有足够多的物品<br/>
     * 返回<code>null</code>并不会有任何提示，你需要在这个方法发送提示消息
     */
    @Nullable
    Integer checkAndTake(int count);
}
