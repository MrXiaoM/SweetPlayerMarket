package top.mrxiaom.sweet.playermarket.actions;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import top.mrxiaom.pluginbase.api.IActionProvider;
import top.mrxiaom.pluginbase.func.GuiManager;
import top.mrxiaom.pluginbase.gui.IGuiHolder;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.sweet.playermarket.Messages;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.data.MarketItem;
import top.mrxiaom.sweet.playermarket.data.NoticeFlag;
import top.mrxiaom.sweet.playermarket.database.MarketplaceDatabase;
import top.mrxiaom.sweet.playermarket.func.NoticeManager;
import top.mrxiaom.sweet.playermarket.gui.api.AbstractGuiSearch;
import top.mrxiaom.sweet.playermarket.utils.Utils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class ActionTakeDownByAdmin extends AbstractActionWithMarketItem {
    public static final ActionTakeDownByAdmin INSTANCE = new ActionTakeDownByAdmin();
    public static final IActionProvider PROVIDER = (s) -> {
        if (s.equals("[take-down-by-admin]") || s.equals("take-down-by-admin")) return INSTANCE;
        return null;
    };
    private ActionTakeDownByAdmin() {}

    @Override
    public void run(@NotNull Player player, @NotNull MarketItem item, @NotNull List<Pair<String, Object>> replacements) {
        if (!player.hasPermission("sweet.playermarket.admin")) return;
        IGuiHolder gui = GuiManager.inst().getOpeningGui(player);
        if (gui instanceof AbstractGuiSearch.SearchGui) {
            AbstractGuiSearch.SearchGui gm = (AbstractGuiSearch.SearchGui) gui;
            SweetPlayerMarket plugin = gm.plugin;

            NoticeManager noticeManager = NoticeManager.inst();
            try (Connection conn = plugin.getConnection()) {
                MarketplaceDatabase db = plugin.getMarketplace();
                MarketItem marketItem = db.getItem(conn, item.shopId());
                if (marketItem == null || marketItem.amount() == 0) {
                    Object i = Utils.get(replacements, "__internal__index");
                    if (i instanceof Integer) {
                        if (marketItem != null) {
                            gm.setItem((int) i, marketItem);
                        } else {
                            gm.setItem((int) i, item.toBuilder().amount(0).build());
                        }
                    }
                    Messages.Gui.me__take_down__item_not_found.tm(player);
                    return;
                }
                // 管理员下架，无需归还商品
//                if (marketItem.type().equals(EnumMarketType.SELL)) {
//                    // 归还上架所需物品，不退手续费
//                    if (!ActionTakeDown.takeDownSell(marketItem, player, marketItem.amount())) return;
//                }
//                if (marketItem.type().equals(EnumMarketType.BUY)) {
//                    // 归还上架所需货币，不退手续费
//                    if (!ActionTakeDown.takeDownBuy(marketItem, player, marketItem.amount())) return;
//                }
                ConfigurationSection params = marketItem.params();
                params.set("take-down-by", player.getName());

                // 如果玩家在线，当场提醒他；不在线就下次上线再提醒
                boolean hasNotice;
                Player owner = plugin.getPlayer(item.playerId());
                if (owner != null) {
                    hasNotice = true;
                    noticeManager.takeDownByAdminNotice(item, owner);
                } else {
                    hasNotice = false;
                }

                // 提交更改到数据库
                if (!db.modifyItem(conn, marketItem.toBuilder()
                        .noticeFlag(hasNotice ? NoticeFlag.NOTHING : NoticeFlag.TAKE_DOWN_BY_ADMIN)
                        .amount(0)
                        .params(params)
                        .build()
                )) {
                    Messages.Gui.me__take_down__submit_failed.tm(player);
                    return;
                }
            } catch (SQLException e) {
                plugin.warn("管理员 " + player.getName() + " 在下架玩家 " + item.playerName() + " 的商品 " + item.shopId() + " 时出现异常", e);
                player.closeInventory();
                Messages.Gui.me__take_down__exception.tm(player);
                return;
            }
            gm.doSearch();
            gm.open();
            noticeManager.updateCreated();
            Messages.Gui.me__take_down__success_admin.tm(player);
        }
    }
}
