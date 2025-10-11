package top.mrxiaom.sweet.playermarket.actions;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.api.IAction;
import top.mrxiaom.pluginbase.api.IActionProvider;
import top.mrxiaom.pluginbase.func.GuiManager;
import top.mrxiaom.pluginbase.gui.IGuiHolder;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.sweet.playermarket.data.MarketItem;
import top.mrxiaom.sweet.playermarket.gui.GuiConfirmBuy;
import top.mrxiaom.sweet.playermarket.gui.GuiConfirmSell;
import top.mrxiaom.sweet.playermarket.gui.GuiMarketplace;

import java.util.List;

public class ActionOpenConfirmGui implements IAction {
    public static final ActionOpenConfirmGui INSTANCE = new ActionOpenConfirmGui();
    public static final IActionProvider PROVIDER = (s) -> {
        return s.equals("[confirm]") || s.equals("confirm") ? INSTANCE : null;
    };
    private ActionOpenConfirmGui() {
    }

    @Override
    public void run(Player player, @Nullable List<Pair<String, Object>> replacements) {
        if (player != null) {
            Object objItem = replacements == null ? null : replacements.stream()
                    .filter(it -> it.key().equals("__internal__market_item"))
                    .findAny().map(Pair::value).orElse(null);
            IGuiHolder gui = objItem == null ? null : GuiManager.inst().getOpeningGui(player);
            if (gui instanceof GuiMarketplace.Impl && objItem instanceof MarketItem) {
                GuiMarketplace.Impl gm = (GuiMarketplace.Impl) gui;
                MarketItem item = (MarketItem) objItem;
                switch (item.type()) {
                    case SELL:
                        GuiConfirmSell.create(player, gm, item).open();
                        break;
                    case BUY:
                        GuiConfirmBuy.create(player, gm, item).open();
                        break;
                }
            }
        }
    }
}
