package top.mrxiaom.sweet.playermarket.actions;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.api.IAction;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.sweet.playermarket.data.MarketItem;
import top.mrxiaom.sweet.playermarket.utils.Utils;

import java.util.List;

public abstract class AbstractActionWithMarketItem implements IAction {
    @Override
    public void run(Player player, @Nullable List<Pair<String, Object>> replacements) {
        if (player != null) {
            Object objItem = Utils.get(replacements, "__internal__market_item");
            if (objItem instanceof MarketItem) {
                run(player, (MarketItem) objItem, replacements);
            }
        }
    }

    public abstract void run(@NotNull Player player, @NotNull MarketItem item, @NotNull List<Pair<String, Object>> replacements);
}
