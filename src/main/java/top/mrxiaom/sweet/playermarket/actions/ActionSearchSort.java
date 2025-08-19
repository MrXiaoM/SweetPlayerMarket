package top.mrxiaom.sweet.playermarket.actions;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.api.IAction;
import top.mrxiaom.pluginbase.api.IActionProvider;
import top.mrxiaom.pluginbase.func.GuiManager;
import top.mrxiaom.pluginbase.gui.IGuiHolder;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.sweet.playermarket.gui.api.AbstractGuiSearch;

import java.util.List;

public class ActionSearchSort implements IAction {
    public static final IActionProvider PROVIDER = (s) -> {
        if (s.startsWith("[search:sort]")) {
            return new ActionSearchSort(s.substring(13));
        }
        if (s.startsWith("search:sort:")) {
            return new ActionSearchSort(s.substring(12));
        }
        return null;
    };
    private final String str;
    public ActionSearchSort(String str) {
        this.str = str;
    }

    @Override
    public void run(Player player, @Nullable List<Pair<String, Object>> replacements) {
        if (player != null) {
            IGuiHolder gui = GuiManager.inst().getOpeningGui(player);
            if (gui instanceof AbstractGuiSearch.SearchGui) {
                AbstractGuiSearch.SearchGui gm = (AbstractGuiSearch.SearchGui) gui;
                switch (str) {
                    case "column": {
                        gm.switchOrderColumn();
                        gm.refreshGui();
                        break;
                    }
                    case "type": {
                        gm.switchOrderSortType();
                        gm.refreshGui();
                        break;
                    }
                    // TODO: 支持指定 column 和 sort
                }
            }
        }
    }
}
