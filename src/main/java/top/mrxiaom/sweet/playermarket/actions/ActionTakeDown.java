package top.mrxiaom.sweet.playermarket.actions;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import top.mrxiaom.pluginbase.api.IActionProvider;
import top.mrxiaom.pluginbase.func.GuiManager;
import top.mrxiaom.pluginbase.gui.IGuiHolder;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.sweet.playermarket.Messages;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.data.EnumMarketType;
import top.mrxiaom.sweet.playermarket.data.MarketItem;
import top.mrxiaom.sweet.playermarket.database.MarketplaceDatabase;
import top.mrxiaom.sweet.playermarket.func.NoticeManager;
import top.mrxiaom.sweet.playermarket.gui.api.AbstractGuiSearch;
import top.mrxiaom.sweet.playermarket.utils.Utils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ActionTakeDown extends AbstractActionWithMarketItem {
    public static final ActionTakeDown INSTANCE = new ActionTakeDown();
    public static final IActionProvider PROVIDER = (s) -> {
        if (s.equals("[take-down]") || s.equals("take-down")) return INSTANCE;
        return null;
    };
    private ActionTakeDown() {}

    @Override
    public void run(@NotNull Player player, @NotNull MarketItem item, @NotNull List<Pair<String, Object>> replacements) {
        IGuiHolder gui = GuiManager.inst().getOpeningGui(player);
        if (gui instanceof AbstractGuiSearch.SearchGui) {
            AbstractGuiSearch.SearchGui gm = (AbstractGuiSearch.SearchGui) gui;
            SweetPlayerMarket plugin = gm.plugin;

            try (Connection conn = plugin.getConnection()) {
                MarketplaceDatabase db = plugin.getMarketplace();
                MarketItem marketItem = db.getItem(conn, item.shopId());
                if (marketItem == null || marketItem.amount() == 0 || !marketItem.playerId().equals(plugin.getKey(player))) {
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
                ConfigurationSection params = marketItem.params();
                if (marketItem.type().equals(EnumMarketType.SELL)) {
                    int count = marketItem.amount();
                    double totalMoney = count * marketItem.price();
                    double old = params.getDouble("sell.received-currency", 0.0);
                    params.set("sell.received-currency", old + totalMoney);
                    int oldCount = params.getInt("sell.received-count", 0);
                    params.set("sell.received-count", oldCount + count);
                }
                if (marketItem.type().equals(EnumMarketType.BUY)) {
                    int count = marketItem.amount();
                    List<ItemStack> itemList = new ArrayList<>();
                    for (Object obj : params.getList("buy.received-items", new ArrayList<>())) {
                        if (obj instanceof ItemStack) {
                            itemList.add((ItemStack) obj);
                        }
                    }
                    for (int i = 0; i < count; i++) {
                        itemList.add(marketItem.item());
                    }
                    params.set("buy.received-items", itemList);
                }
                // 提交更改到数据库
                if (!db.modifyItem(conn, marketItem.toBuilder()
                        .noticeFlag(1)
                        .amount(0)
                        .params(params)
                        .build()
                )) {
                    Messages.Gui.me__take_down__submit_failed.tm(player);
                    return;
                }
            } catch (SQLException e) {
                plugin.warn("玩家 " + player.getName() + " 在下架自己的商品 " + item.shopId() + " 时出现异常", e);
                player.closeInventory();
                Messages.Gui.me__take_down__exception.tm(player);
                return;
            }
            gm.doSearch();
            gm.open();
            NoticeManager.inst().updateCreated();
            Messages.Gui.me__take_down__success.tm(player);
        }
    }
}
