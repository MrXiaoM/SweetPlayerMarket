package top.mrxiaom.sweet.playermarket.actions;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.api.IAction;
import top.mrxiaom.pluginbase.api.IActionProvider;
import top.mrxiaom.pluginbase.func.GuiManager;
import top.mrxiaom.pluginbase.gui.IGui;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.playermarket.gui.Pageable;
import top.mrxiaom.sweet.playermarket.gui.Refreshable;

import java.util.List;

public class ActionPage implements IAction {
    public static final IActionProvider PROVIDER = (s) -> {
        if (s.startsWith("[page]")) {
            int pages = Util.parseInt(s.substring(6)).orElse(0);
            if (pages != 0) {
                return new ActionPage(pages);
            }
        }
        if (s.startsWith("page:")) {
            int pages = Util.parseInt(s.substring(5)).orElse(0);
            if (pages != 0) {
                return new ActionPage(pages);
            }
        }
        return null;
    };
    private final int pages;
    public ActionPage(int pages) {
        this.pages = pages;
    }

    @Override
    public void run(Player player, @Nullable List<Pair<String, Object>> replacements) {
        if (player != null) {
            IGui gui = GuiManager.inst().getOpeningGui(player);
            if (gui instanceof Pageable) {
                Pageable p = (Pageable) gui;
                if (pages > 0) p.turnPageDown(pages);
                else p.turnPageUp(-pages);
            }
        }
    }
}
