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

public class ActionSearchOutOfStock implements IAction {
    public static final ActionSearchOutOfStock INSTANCE = new ActionSearchOutOfStock();
    public static final IActionProvider PROVIDER = (s) -> {
        if (s.equals("[search:out-of-stock]") || s.equals("search:out-of-stock")) return INSTANCE;
        return null;
    };
    private ActionSearchOutOfStock() {}
    @Override
    public void run(Player player, @Nullable List<Pair<String, Object>> replacements) {
        if (player != null) {
            IGuiHolder gui = GuiManager.inst().getOpeningGui(player);
            if (gui instanceof AbstractGuiSearch.SearchGui) {
                AbstractGuiSearch.SearchGui gm = (AbstractGuiSearch.SearchGui) gui;
                gm.searching().onlyOutOfStock(!gm.searching().onlyOutOfStock());
                gm.refreshGui();
            }
        }
    }
}
