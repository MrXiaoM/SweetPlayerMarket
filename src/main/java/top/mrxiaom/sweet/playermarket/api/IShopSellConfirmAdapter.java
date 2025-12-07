package top.mrxiaom.sweet.playermarket.api;

public interface IShopSellConfirmAdapter {
    /**
     * 完成购买商品
     * @param count 购买份数
     * @return 最终购买总数量
     */
    int giveToPlayer(int count);

    /**
     * 重写下架商品后，归还玩家物品的逻辑，默认与 <code>giveToPlayer</code> 一致
     * @param count 购买数量
     */
    default void takeDownItem(int count) {
        giveToPlayer(count);
    }

    /**
     * 重写商品过期后，玩家拿回商品的逻辑，默认与 <code>giveToPlayer</code> 一致
     * @param count 购买数量
     */
    default void takeBackOutdatedItem(int count) {
        giveToPlayer(count);
    }

    /**
     * 重写玩家领取货币时，给予玩家的货币数量
     * @param originalMoney 原货币数量
     * @return 新货币数量
     */
    default double overrideRewardMoney(double originalMoney) {
        return originalMoney;
    }
}
