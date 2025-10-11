package top.mrxiaom.sweet.playermarket.actions;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import top.mrxiaom.pluginbase.api.IActionProvider;
import top.mrxiaom.pluginbase.func.GuiManager;
import top.mrxiaom.pluginbase.gui.IGuiHolder;
import top.mrxiaom.pluginbase.utils.ItemStackUtil;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.sweet.playermarket.Messages;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.api.IShopSellConfirmAdapter;
import top.mrxiaom.sweet.playermarket.data.EnumMarketType;
import top.mrxiaom.sweet.playermarket.data.MarketItem;
import top.mrxiaom.sweet.playermarket.database.MarketplaceDatabase;
import top.mrxiaom.sweet.playermarket.economy.IEconomy;
import top.mrxiaom.sweet.playermarket.func.NoticeManager;
import top.mrxiaom.sweet.playermarket.func.ShopAdapterRegistry;
import top.mrxiaom.sweet.playermarket.gui.api.AbstractGuiSearch;
import top.mrxiaom.sweet.playermarket.utils.Utils;

import java.sql.Connection;
import java.sql.SQLException;
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
                if (marketItem.type().equals(EnumMarketType.SELL)) {
                    // 归还上架所需物品，不退手续费
                    if (!takeDownSell(marketItem, player, marketItem.amount())) return;
                }
                if (marketItem.type().equals(EnumMarketType.BUY)) {
                    // 归还上架所需货币，不退手续费
                    if (!takeDownBuy(marketItem, player, marketItem.amount())) return;
                }
                // 提交更改到数据库
                if (!db.modifyItem(conn, marketItem.toBuilder()
                        .noticeFlag(0)
                        .amount(0)
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

    protected static boolean takeDownSell(MarketItem marketItem, Player player, int count) {
        ShopAdapterRegistry.Entry entry = ShopAdapterRegistry.inst().getByMarketItem(marketItem);
        if (entry.hasFactoryParams()) {
            // 如果有商品适配器，则按适配器的实现来给予玩家物品
            IShopSellConfirmAdapter shopAdapter = entry.getSellConfirmAdapter(marketItem, player);
            if (shopAdapter == null) {
                Messages.Gui.sell__adapter_not_found.tm(player);
                return false;
            }
            shopAdapter.giveToPlayer(count);
        } else {
            // 如果没有商品适配器，直接给予玩家物品
            for (int i = 0; i < count; i++) {
                ItemStackUtil.giveItemToPlayer(player, marketItem.item());
            }
        }
        return true;
    }

    protected static boolean takeDownBuy(MarketItem marketItem, Player player, int count) {
        IEconomy currency = marketItem.currency();
        if (currency == null) {
            String currencyName = SweetPlayerMarket.getInstance().displayNames().getCurrencyName(marketItem.currencyName());
            Messages.Gui.common__currency_not_found.tm(player, Pair.of("%currency%", currencyName));
            return false;
        }
        currency.giveMoney(player, marketItem.price() * count);
        return true;
    }
}
