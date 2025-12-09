package top.mrxiaom.sweet.playermarket.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import top.mrxiaom.sweet.playermarket.data.MarketItem;

public class MarketItemCreatedEvent extends AbstractPlayerMarketEvent {
    private static final HandlerList handlers = new HandlerList();
    public MarketItemCreatedEvent(MarketItem marketItem, Player player) {
        super(marketItem, player);
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
