package top.mrxiaom.sweet.playermarket.data;

import org.jetbrains.annotations.ApiStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class TradeLog {
    private final int id;
    private final String shopId;
    private final String shopOwner;
    private final LocalDateTime tradeTime;
    private final UUID playerUuid;
    private final String playerName;
    private final int amount;
    private MarketItem item;
    @ApiStatus.Internal
    public TradeLog(int id, String shopId, String shopOwner, LocalDateTime tradeTime, UUID playerUuid, String playerName, int amount) {
        this.id = id;
        this.shopId = shopId;
        this.shopOwner = shopOwner;
        this.tradeTime = tradeTime;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.amount = amount;
    }

    public int id() {
        return id;
    }

    public String shopId() {
        return shopId;
    }

    public String shopOwner() {
        return shopOwner;
    }

    public LocalDateTime tradeTime() {
        return tradeTime;
    }

    public UUID playerUuid() {
        return playerUuid;
    }

    public String playerName() {
        return playerName;
    }

    public int amount() {
        return amount;
    }

    public MarketItem item() {
        return item;
    }

    public void item(MarketItem item) {
        this.item = item;
    }
}
