package top.mrxiaom.sweet.playermarket.func;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.playermarket.Messages;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.data.ActiveItemsLimit;
import top.mrxiaom.sweet.playermarket.data.EnumMarketType;
import top.mrxiaom.sweet.playermarket.data.Searching;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

@AutoRegister
public class ActiveItemsLimitManager extends AbstractModule {
    private final List<ActiveItemsLimit> activeList = new ArrayList<>();
    private ActiveItemsLimit activeDefault;
    public ActiveItemsLimitManager(SweetPlayerMarket plugin) {
        super(plugin);
    }

    private int parseLimit(ConfigurationSection section, String path) {
        String value = section.getString(path);
        if ("infinite".equals(value)) {
            return -1;
        } else {
            Integer i = Util.parseInt(value).orElse(null);
            if (i != null && i >= 0) {
                return i;
            } else {
                warn("[config] 配置中 active-items-limit." + path + " 的值无效，使用缺省值 infinite");
                return -1;
            }
        }
    }

    @Override
    public void reloadConfig(MemoryConfiguration config) {
        activeList.clear();
        ActiveItemsLimit activeDefault = null;
        ConfigurationSection section = config.getConfigurationSection("active-items-limit");
        if (section != null) for (String groupName : section.getKeys(false)) {
            int priority = section.getInt(groupName + ".priority", 1000);
            int total = parseLimit(section, groupName + ".total");
            Map<EnumMarketType, Integer> byType = new HashMap<>();
            for (EnumMarketType type : EnumMarketType.values()) {
                String path = groupName + "." + type.name().toLowerCase();
                int limit = parseLimit(section, path);
                byType.put(type, limit);
            }
            ActiveItemsLimit limit = new ActiveItemsLimit(groupName, priority, total, byType);
            activeList.add(limit);
            if (groupName.equalsIgnoreCase("default")) {
                activeDefault = limit;
            }
        }
        activeList.sort(Comparator.comparingInt(ActiveItemsLimit::priority));
        if (activeDefault == null) {
            warn("[config] 配置中 active-items-limit.default 无效，将使用缺省值代替 (全部无限制)");
            Map<EnumMarketType, Integer> byType = new HashMap<>();
            for (EnumMarketType type : EnumMarketType.values()) {
                byType.put(type, -1);
            }
            this.activeDefault = new ActiveItemsLimit("default", 1000, -1, byType);
        } else {
            this.activeDefault = activeDefault;
        }
    }

    @ApiStatus.Internal
    public boolean shouldNotCreateItem(Connection conn, Player player, EnumMarketType type) throws SQLException {
        ActiveItemsLimit limit = get(player);

        int totalLimit = limit.total();
        int typeLimit = limit.getLimit(type);

        if (typeLimit == -1 && totalLimit == -1) return false;
        if (totalLimit == 0) {
            Messages.Limited.total__zero.tm(player);
            return true;
        }
        if (typeLimit == 0) {
            String typeName = plugin.displayNames().getMarketTypeName(type);
            Messages.Limited.by_type__zero.tm(player,
                    Pair.of("%type%", typeName));
            return true;
        }

        Searching searching = Searching.of(false);
        searching.playerId(plugin.getKey(player));
        if (totalLimit > 0) {
            if (plugin.getMarketplace().getTotalCount(conn, searching) >= totalLimit) {
                Messages.Limited.total__normal.tm(player,
                        Pair.of("%count%", totalLimit));
                return true;
            }
        }
        searching.type(type);
        if (typeLimit > 0) {
            if (plugin.getMarketplace().getTotalCount(conn, searching) >= typeLimit) {
                String typeName = plugin.displayNames().getMarketTypeName(type);
                Messages.Limited.by_type__normal.tm(player,
                        Pair.of("%type%", typeName),
                        Pair.of("%count%", typeLimit));
                return true;
            }
        }

        return false;
    }

    /**
     * 根据玩家权限获取到期时间配置
     * @param player 玩家
     */
    @NotNull
    public ActiveItemsLimit get(Player player) {
        for (ActiveItemsLimit active : activeList) {
            if (active.hasPermission(player)) {
                return active;
            }
        }
        return activeDefault;
    }

    public static ActiveItemsLimitManager inst() {
        return instanceOf(ActiveItemsLimitManager.class);
    }
}
