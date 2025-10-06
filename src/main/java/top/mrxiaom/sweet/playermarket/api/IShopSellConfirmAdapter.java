package top.mrxiaom.sweet.playermarket.api;

public interface IShopSellConfirmAdapter {
    /**
     * 完成购买商品
     * @param count 购买份数
     * @return 最终购买总数量
     */
    int giveToPlayer(int count);
}
