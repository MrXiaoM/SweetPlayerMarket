package top.mrxiaom.sweet.playermarket.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import top.mrxiaom.sweet.playermarket.data.MarketItem;

public class MarketItemBeforeCreateEvent extends AbstractPlayerMarketEvent implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;
    public MarketItemBeforeCreateEvent(MarketItem marketItem, Player player) {
        super(marketItem, player);
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
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
