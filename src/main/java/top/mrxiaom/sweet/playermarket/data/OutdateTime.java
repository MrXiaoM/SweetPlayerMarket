package top.mrxiaom.sweet.playermarket.data;

import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Function;

public class OutdateTime {
    private final String id;
    private final int priority;
    private final Map<EnumMarketType, Function<LocalDateTime, LocalDateTime>> outdateTimes;

    public OutdateTime(String id, int priority, Map<EnumMarketType, Function<LocalDateTime, LocalDateTime>> outdateTimes) {
        this.id = id;
        this.priority = priority;
        this.outdateTimes = outdateTimes;
    }

    public String id() {
        return id;
    }

    public int priority() {
        return priority;
    }

    public boolean hasPermission(Permissible p) {
        return p.hasPermission("sweet.playermarket.outdate." + id);
    }

    /**
     * 通过商品类型获取下次到期时间
     * @param type 商品类型
     * @return 到期时间，null 代表永不到期
     */
    @Nullable
    public LocalDateTime get(@NotNull EnumMarketType type) {
        return get(type, LocalDateTime.now());
    }

    /**
     * 通过商品类型获取下次到期时间
     * @param type 商品类型
     * @param now 指定相对的现在时间
     * @return 到期时间，null 代表永不到期
     */
    @Nullable
    public LocalDateTime get(@NotNull EnumMarketType type, @NotNull LocalDateTime now) {
        Function<LocalDateTime, LocalDateTime> func = outdateTimes.get(type);
        if (func == null) {
            return now.plusDays(5);
        }
        return func.apply(now);
    }
}
