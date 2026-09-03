package top.mrxiaom.sweet.playermarket.data;

import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class ActiveItemsLimit {
    private final String id;
    private final int priority;
    private final int total;
    private final Map<EnumMarketType, Integer> byType;

    @ApiStatus.Internal
    public ActiveItemsLimit(String id, int priority, int total, Map<EnumMarketType, Integer> byType) {
        this.id = id;
        this.priority = priority;
        this.total = total;
        this.byType = byType;
    }

    public String id() {
        return id;
    }

    public int priority() {
        return priority;
    }

    public boolean hasPermission(Permissible p) {
        return p.hasPermission("sweet.playermarket.active." + id);
    }

    public int total() {
        return total;
    }

    public int getLimit(@NotNull EnumMarketType type) {
        return byType.getOrDefault(type, 0);
    }
}
