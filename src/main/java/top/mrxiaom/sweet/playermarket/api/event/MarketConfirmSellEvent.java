package top.mrxiaom.sweet.playermarket.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import top.mrxiaom.sweet.playermarket.data.MarketItem;
import top.mrxiaom.sweet.playermarket.economy.IEconomy;

/**
 * 出售商品被某人购买成功后触发事件
 */
public class MarketConfirmSellEvent extends AbstractPlayerMarketEvent {
    private static final HandlerList handlers = new HandlerList();
    private final int buyAmount, totalItemCount;
    private final double totalMoney;
    private final IEconomy currency;
    public MarketConfirmSellEvent(MarketItem marketItem, Player player, int buyAmount, int totalItemCount, double totalMoney, IEconomy currency) {
        super(marketItem, player);
        this.buyAmount = buyAmount;
        this.totalItemCount = totalItemCount;
        this.totalMoney = totalMoney;
        this.currency = currency;
    }

    /**
     * 玩家在出售商店中购买的商品份数
     */
    public int getBuyAmount() {
        return buyAmount;
    }

    /**
     * 玩家购买的物品总数量
     */
    public int getTotalItemCount() {
        return totalItemCount;
    }

    /**
     * 玩家总共花费了多少金钱
     */
    public double getTotalMoney() {
        return totalMoney;
    }

    /**
     * 玩家花费的什么货币
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
