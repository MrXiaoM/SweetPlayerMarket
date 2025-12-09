package top.mrxiaom.sweet.playermarket.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEvent;
import top.mrxiaom.sweet.playermarket.data.MarketItem;

public abstract class AbstractPlayerMarketEvent extends PlayerEvent {
    private final MarketItem marketItem;
    public AbstractPlayerMarketEvent(MarketItem marketItem, Player player) {
        super(player);
        this.marketItem = marketItem;
    }

    public MarketItem getMarketItem() {
        return marketItem;
    }
}
