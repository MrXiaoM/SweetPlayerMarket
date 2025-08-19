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

public class ActionSearchOutdate implements IAction {
    public static final ActionSearchOutdate INSTANCE = new ActionSearchOutdate();
    public static final IActionProvider PROVIDER = (s) -> {
        if (s.equals("[search:outdate]") || s.equals("search:outdate")) return INSTANCE;
        return null;
    };
    private ActionSearchOutdate() {}
    @Override
    public void run(Player player, @Nullable List<Pair<String, Object>> replacements) {
        if (player != null) {
            IGuiHolder gui = GuiManager.inst().getOpeningGui(player);
            if (gui instanceof AbstractGuiSearch.SearchGui) {
                AbstractGuiSearch.SearchGui gm = (AbstractGuiSearch.SearchGui) gui;
                gm.searching().outdated(!gm.searching().outdated());
                gm.refreshGui();
            }
        }
    }
}
