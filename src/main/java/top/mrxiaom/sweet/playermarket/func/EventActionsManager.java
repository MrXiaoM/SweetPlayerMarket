package top.mrxiaom.sweet.playermarket.func;

import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import top.mrxiaom.pluginbase.actions.ActionProviders;
import top.mrxiaom.pluginbase.api.IAction;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.ListPair;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.api.event.MarketConfirmBuyEvent;
import top.mrxiaom.sweet.playermarket.api.event.MarketConfirmSellEvent;
import top.mrxiaom.sweet.playermarket.api.event.MarketItemCreatedEvent;

import java.util.List;

@AutoRegister
public class EventActionsManager extends AbstractModule implements Listener {
    List<IAction> actionsAfterBuy, actionsAfterSell, actionsAfterCreate;
    public EventActionsManager(SweetPlayerMarket plugin) {
        super(plugin);
        registerEvents();
    }

    @Override
    public void reloadConfig(MemoryConfiguration config) {
        actionsAfterBuy = ActionProviders.loadActions(config, "event-actions.buy-shop-confirm");
        actionsAfterSell = ActionProviders.loadActions(config, "event-actions.sell-shop-confirm");
        actionsAfterCreate = ActionProviders.loadActions(config, "event-actions.create-shop");
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onCreate(MarketItemCreatedEvent e) {
        if (actionsAfterCreate.isEmpty()) return;
        Player player = e.getPlayer();
        ListPair<String, Object> r = e.getMarketItem().replacements(plugin.displayNames(), player);
        ActionProviders.run(plugin, player, actionsAfterCreate, r);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onConfirmSell(MarketConfirmSellEvent e) {
        if (actionsAfterSell.isEmpty()) return;
        Player player = e.getPlayer();
        ListPair<String, Object> r = e.getMarketItem().replacements(plugin.displayNames(), player);
        r.add("buy_amount", e.getBuyAmount());
        r.add("%total_item_count%", e.getTotalItemCount());
        r.add("%total_money%", String.format("%.2f", e.getTotalMoney()).replace(".00", ""));
        r.add("%total_money_raw%", e.getTotalMoney());
        ActionProviders.run(plugin, player, actionsAfterSell, r);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onConfirmBuy(MarketConfirmBuyEvent e) {
        if (actionsAfterBuy.isEmpty()) return;
        Player player = e.getPlayer();
        ListPair<String, Object> r = e.getMarketItem().replacements(plugin.displayNames(), player);
        r.add("sell_amount", e.getSellAmount());
        r.add("%total_item_count%", e.getTotalItemCount());
        r.add("%total_money%", String.format("%.2f", e.getTotalMoney()).replace(".00", ""));
        r.add("%total_money_raw%", e.getTotalMoney());
        ActionProviders.run(plugin, player, actionsAfterBuy, r);
    }
}
