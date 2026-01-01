package top.mrxiaom.sweet.playermarket.func;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.func.GuiManager;
import top.mrxiaom.pluginbase.gui.IGuiHolder;
import top.mrxiaom.pluginbase.utils.*;
import top.mrxiaom.pluginbase.utils.depend.PAPI;
import top.mrxiaom.sweet.playermarket.Messages;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.data.EnumMarketType;
import top.mrxiaom.sweet.playermarket.data.MarketItem;
import top.mrxiaom.sweet.playermarket.data.NoticeFlag;
import top.mrxiaom.sweet.playermarket.database.MarketplaceDatabase;
import top.mrxiaom.sweet.playermarket.gui.api.AbstractGuiSearch;

import java.io.DataInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AutoRegister
public class NoticeManager extends AbstractModule implements Listener {
    public static class Notice {
        private final String text;
        private final List<String> hover;
        private final String click;
        public Notice(String text, List<String> hover, String click) {
            this.text = text;
            this.hover = hover;
            this.click = click;
        }

        public boolean isEmpty() {
            return text.isEmpty();
        }

        public void send(Player player, @Nullable ListPair<String, Object> r) {
            String text = r == null ? this.text : Pair.replace(this.text, r);
            List<String> hover = r == null ? this.hover : Pair.replace(this.hover, r);
            Component builder = AdventureUtil.miniMessage(PAPI.setPlaceholders(player, text));
            if (!hover.isEmpty()) {
                TextComponent.Builder textBuilder = Component.text();
                List<Component> hoverList = AdventureUtil.miniMessage(hover);
                textBuilder.append(hoverList.get(0));
                for (int i = 1; i < hoverList.size(); i++) {
                    textBuilder.appendNewline();
                    textBuilder.append(hoverList.get(i));
                }
                builder = builder.hoverEvent(textBuilder.build().asHoverEvent());
            }
            if (!click.isEmpty()) {
                builder = builder.clickEvent(ClickEvent.runCommand(click));
            }
            AdventureUtil.of(player).sendMessage(builder);
        }

        public static Notice from(ConfigurationSection config, String prefix) {
            String text = config.getString(prefix + ".text", "");
            List<String> hover = config.getStringList(prefix + ".hover");
            String click = config.getString(prefix + ".click", "");
            return new Notice(text, hover, click);
        }
    }
    private final Map<EnumMarketType, Notice> noticeByType = new HashMap<>();
    private Notice noticeOnJoin;
    private boolean updateGuiAfterCreateItem;
    public NoticeManager(SweetPlayerMarket plugin) {
        super(plugin);
        registerEvents();
        registerBungee();
    }

    @Override
    public void reloadConfig(MemoryConfiguration config) {
        updateGuiAfterCreateItem = config.getBoolean("update-gui-after-create-item", true);
        noticeOnJoin = Notice.from(config, "notice.on-join");
        noticeByType.clear();
        for (EnumMarketType type : EnumMarketType.values()) {
            String key = type.name().toLowerCase().replace("_", "-");
            noticeByType.put(type, Notice.from(config, "notice." + key));
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        if (noticeOnJoin.isEmpty()) return;
        plugin.getScheduler().runTaskAsync(() -> {
            MarketplaceDatabase marketplace = plugin.getMarketplace();
            List<MarketItem> items = marketplace
                    .queryItems(1, 1)
                    .player(player)
                    .noticeFlag(NoticeFlag.NOTHING, true)
                    .search();
            int noticeClaim = 0;
            List<MarketItem> noticeTakedown = new ArrayList<>();
            for (MarketItem item : items) {
                if (item.noticeFlag() == NoticeFlag.CAN_CLAIM_ITEMS.getIntValue()) {
                    noticeClaim++;
                }
                // 如果物品已下架，修改提醒标记为空
                if (item.noticeFlag() == NoticeFlag.TAKE_DOWN_BY_ADMIN.getIntValue()) {
                    noticeTakedown.add(item.toBuilder()
                            .noticeFlag(NoticeFlag.NOTHING)
                            .build());
                }
            }
            // 提醒玩家有物品可领取
            if (noticeClaim > 0) {
                noticeOnJoin.send(player, null);
            }
            if (!noticeTakedown.isEmpty()) {
                // 发送商品被管理员下架提醒
                for (MarketItem item : noticeTakedown) {
                    takeDownByAdminNotice(item, player);
                }
                // 提交已下架物品的提醒标记修改到数据库
                try (Connection conn = plugin.getConnection()) {
                    for (MarketItem item : noticeTakedown) {
                        marketplace.modifyItem(conn, item);
                    }
                } catch (SQLException ex) {
                    warn(ex);
                }
            }
        });
    }

    /**
     * 当玩家的商品被管理员下架时，立即或下次上线时提醒
     * @param item 商品
     * @param player 在线玩家
     */
    public void takeDownByAdminNotice(MarketItem item, Player player) {
        ListPair<String, Object> r = new ListPair<>();
        r.add("%item%", plugin.displayNames().getDisplayName(item.item(), player));
        r.add("%admin_name%", item.params().getString("take-down-by", Messages.Notice.takedown_by_admin__unknown_admin.str()));
        AbstractGuiSearch.applyMarketItemPlaceholders(plugin, item, r);
        Messages.Notice.takedown_by_admin__content.tm(player, r);
    }

    /**
     * 玩家已下单时，向店主发送提示
     */
    public void confirmNotice(MarketItem item) {
        String key = item.playerId();
        if (key.equals("#server#")) return;
        Player online = plugin.getPlayer(key);
        if (online != null) {
            confirmNotice(item, online);
            return;
        }
        Bytes.sendByWhoeverOrNot("BungeeCord", Bytes.build(out -> {
            out.writeLong(System.currentTimeMillis() + 3000L);
            out.writeUTF(item.shopId());
            out.writeUTF(key);
        }, "Forward", "ALL", "SweetPlayerMarket_Notice"));
    }

    public void confirmNotice(MarketItem item, Player owner) {
        Notice notice = noticeByType.get(item.type());
        if (notice == null || notice.isEmpty()) return;
        ListPair<String, Object> r = new ListPair<>();
        r.add("%item%", plugin.displayNames().getDisplayName(item.item(), owner));
        r.add("%currency%", plugin.displayNames().getCurrencyName(item.currencyName()));
        notice.send(owner, r);
    }

    public void updateCreated() {
        if (updateGuiAfterCreateItem) {
            doSearchGuiUpdate();
            Bytes.sendByWhoeverOrNot("BungeeCord", Bytes.build(out -> {
                out.writeLong(System.currentTimeMillis() + 3000L);
            }, "Forward", "ALL", "SweetPlayerMarket_Update"));
        }
    }

    public void doSearchGuiUpdate() {
        if (updateGuiAfterCreateItem) {
            GuiManager manager = GuiManager.inst();
            for (Player player : Bukkit.getOnlinePlayers()) {
                IGuiHolder gui = manager.getOpeningGui(player);
                if (gui instanceof AbstractGuiSearch.SearchGui) {
                    ((AbstractGuiSearch.SearchGui) gui).refreshGui();
                }
            }
        }
    }

    @Override
    public void receiveBungee(String subChannel, DataInputStream in) throws IOException {
        if (subChannel.equals("SweetPlayerMarket_Notice")) {
            if (System.currentTimeMillis() > in.readLong()) return;
            String shopId = in.readUTF();
            String playerId = in.readUTF();
            Player online = plugin.getPlayer(playerId);
            if (online != null) plugin.getScheduler().runTaskAsync(() -> {
                MarketItem item = plugin.getMarketplace().getItem(shopId);
                if (item != null && item.noticeFlag() == 1) {
                    confirmNotice(item, online);
                }
            });
        }
        if (subChannel.equals("SweetPlayerMarket_Update")) {
            if (System.currentTimeMillis() > in.readLong()) return;
            doSearchGuiUpdate();
        }
    }

    public static NoticeManager inst() {
        return instanceOf(NoticeManager.class);
    }
}
