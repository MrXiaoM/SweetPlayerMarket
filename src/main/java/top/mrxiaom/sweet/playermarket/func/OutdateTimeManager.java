package top.mrxiaom.sweet.playermarket.func;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.data.EnumMarketType;
import top.mrxiaom.sweet.playermarket.data.OutdateTime;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;

@AutoRegister
public class OutdateTimeManager extends AbstractModule {
    List<OutdateTime> outdateTimeList = new ArrayList<>();
    OutdateTime outdateTimeDefault;
    public OutdateTimeManager(SweetPlayerMarket plugin) {
        super(plugin);
    }

    @Override
    public void reloadConfig(MemoryConfiguration config) {
        outdateTimeList.clear();
        OutdateTime outdateTimeDefault = null;
        ConfigurationSection section = config.getConfigurationSection("outdate-time");
        if (section != null) for (String groupName : section.getKeys(false)) {
            int priority = section.getInt(groupName + ".priority", 1000);
            Map<EnumMarketType, Function<LocalDateTime, LocalDateTime>> outdateTimes = new HashMap<>();
            for (EnumMarketType type : EnumMarketType.values()) {
                String path = groupName + "." + type.name().toLowerCase();
                Function<LocalDateTime, LocalDateTime> func = parseTimeOverride(section.getString(path));
                if (func != null) {
                    outdateTimes.put(type, func);
                } else {
                    warn("[config] 配置中 outdate-time." + path + " 的值无效，使用缺省值 5d");
                    outdateTimes.put(type, now -> now.plusDays(5));
                }
            }
            OutdateTime time = new OutdateTime(groupName, priority, outdateTimes);
            outdateTimeList.add(time);
            if (groupName.equalsIgnoreCase("default")) {
                outdateTimeDefault = time;
            }
        }
        outdateTimeList.sort(Comparator.comparingInt(OutdateTime::priority));
        if (outdateTimeDefault == null) {
            warn("[config] 配置中 outdate-time.default 无效，将使用缺省值代替");
            Map<EnumMarketType, Function<LocalDateTime, LocalDateTime>> outdateTimes = new HashMap<>();
            for (EnumMarketType type : EnumMarketType.values()) {
                outdateTimes.put(type, now -> now.plusDays(5));
            }
            this.outdateTimeDefault = new OutdateTime("default", 1000, outdateTimes);
        } else {
            this.outdateTimeDefault = outdateTimeDefault;
        }
    }

    /**
     * 根据玩家权限获取到期时间配置
     * @param player 玩家
     */
    @NotNull
    public OutdateTime get(Player player) {
        for (OutdateTime outdateTime : outdateTimeList) {
            if (outdateTime.hasPermission(player)) {
                return outdateTime;
            }
        }
        return outdateTimeDefault;
    }

    @Nullable
    private static Function<LocalDateTime, LocalDateTime> parseTimeOverride(@Nullable String str) {
        if (str == null) return null;
        if (str.equalsIgnoreCase("infinite")) return now -> null;
        int addMonth = 0, addDay = 0, addHour = 0, addMinute = 0, addSecond = 0;
        StringBuilder sb = new StringBuilder();
        char[] chars = str.toCharArray();
        for (char ch : chars) {
            if (ch >= '0' && ch <= '9') {
                sb.append(ch);
                continue;
            }
            Integer value = Util.parseInt(sb.toString()).orElse(null);
            sb = new StringBuilder();
            if (value != null) switch (ch) {
                case 'M':
                    addMonth += value;
                    break;
                case 'd':
                    addDay += value;
                    break;
                case 'h':
                    addHour += value;
                    break;
                case 'm':
                    addMinute += value;
                    break;
                case 's':
                    addSecond += value;
                    break;
            }
        }
        int month = addMonth, day = addDay, hour = addHour, minute = addMinute, second = addSecond;
        if (month + day + hour + minute + second == 0) return null;
        return now -> {
            LocalDateTime time = now;
            if (month > 0) time = time.plusMonths(month);
            if (day > 0) time = time.plusDays(day);
            if (hour > 0) time = time.plusHours(hour);
            if (minute > 0) time = time.plusMinutes(minute);
            if (second > 0) time = time.plusSeconds(second);
            return time;
        };
    }

    public static OutdateTimeManager inst() {
        return instanceOf(OutdateTimeManager.class);
    }
}
