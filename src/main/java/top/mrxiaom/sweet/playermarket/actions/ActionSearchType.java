package top.mrxiaom.sweet.playermarket.actions;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.api.IAction;
import top.mrxiaom.pluginbase.api.IActionProvider;
import top.mrxiaom.pluginbase.func.GuiManager;
import top.mrxiaom.pluginbase.gui.IGuiHolder;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.sweet.playermarket.data.EnumMarketType;
import top.mrxiaom.sweet.playermarket.gui.api.AbstractGuiSearch;

import java.util.List;

public class ActionSearchType implements IAction {
    public static final IActionProvider PROVIDER = (s) -> {
        if (s.startsWith("[search:type]")) {
            String type = s.substring(13);
            return new ActionSearchType(type);
        }
        if (s.startsWith("search:type:")) {
            String type = s.substring(12);
            return new ActionSearchType(type);
        }
        return null;
    };
    private final String type;
    public ActionSearchType(String type) {
        this.type = type;
    }

    @Override
    public void run(Player player, @Nullable List<Pair<String, Object>> replacements) {
        if (player != null) {
            IGuiHolder gui = GuiManager.inst().getOpeningGui(player);
            if (gui instanceof AbstractGuiSearch.SearchGui) {
                AbstractGuiSearch.SearchGui gm = (AbstractGuiSearch.SearchGui) gui;
                switch (type) {
                    case "buy": {
                        gm.searching().type(EnumMarketType.BUY);
                        gm.refreshGui();
                        break;
                    }
                    case "sell": {
                        gm.searching().type(EnumMarketType.SELL);
                        gm.refreshGui();
                        break;
                    }
                    case "none": {
                        gm.searching().type(null);
                        gm.refreshGui();
                        break;
                    }
                }
            }
        }
    }
}
