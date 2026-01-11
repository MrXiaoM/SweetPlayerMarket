package top.mrxiaom.sweet.playermarket.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import top.mrxiaom.sweet.playermarket.data.MarketItem;
import top.mrxiaom.sweet.playermarket.economy.IEconomy;

/**
 * 收购商品被某人购买成功后触发事件
 */
public class MarketConfirmBuyEvent extends AbstractPlayerMarketEvent {
    private static final HandlerList handlers = new HandlerList();
    private final int sellAmount, totalItemCount;
    private final double totalMoney;
    private final IEconomy currency;
    public MarketConfirmBuyEvent(MarketItem marketItem, Player player, int sellAmount, int totalItemCount, double totalMoney, IEconomy currency) {
        super(marketItem, player);
        this.sellAmount = sellAmount;
        this.totalItemCount = totalItemCount;
        this.totalMoney = totalMoney;
        this.currency = currency;
    }

    /**
     * 玩家在收购商店中出售的商品份数
     */
    public int getSellAmount() {
        return sellAmount;
    }

    /**
     * 玩家出售的物品总数量
     */
    public int getTotalItemCount() {
        return totalItemCount;
    }

    /**
     * 玩家总共获得了多少金钱
     */
    public double getTotalMoney() {
        return totalMoney;
    }

    /**
     * 玩家获得的什么货币
     */
    public IEconomy getCurrency() {
        return currency;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return handlers;
    }
}
