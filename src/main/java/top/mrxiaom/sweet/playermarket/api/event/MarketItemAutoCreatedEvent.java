package top.mrxiaom.sweet.playermarket.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import top.mrxiaom.sweet.playermarket.data.MarketItem;
import top.mrxiaom.sweet.playermarket.data.deploy.AutoDeployProperty;

/**
 * 商品自动上架成功后触发事件
 */
public class MarketItemAutoCreatedEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final MarketItem marketItem;
    private final AutoDeployProperty property;
    public MarketItemAutoCreatedEvent(MarketItem marketItem, AutoDeployProperty property) {
        this.marketItem = marketItem;
        this.property = property;
    }

    public MarketItem getMarketItem() {
        return marketItem;
    }

    public AutoDeployProperty getProperty() {
        return property;
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
