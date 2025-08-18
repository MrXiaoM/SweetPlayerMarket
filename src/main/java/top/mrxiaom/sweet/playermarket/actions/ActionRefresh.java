package top.mrxiaom.sweet.playermarket.actions;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.api.IAction;
import top.mrxiaom.pluginbase.api.IActionProvider;
import top.mrxiaom.pluginbase.func.GuiManager;
import top.mrxiaom.pluginbase.gui.IGui;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.sweet.playermarket.gui.api.IGuiRefreshable;

import java.util.List;

public class ActionRefresh implements IAction {
    public static final ActionRefresh INSTANCE = new ActionRefresh();
    public static final IActionProvider PROVIDER = (s) -> {
        if (s.equals("[refresh]") || s.equals("refresh")) return INSTANCE;
        return null;
    };
    private ActionRefresh() {}
    @Override
    public void run(Player player, @Nullable List<Pair<String, Object>> replacements) {
        if (player != null) {
            IGui gui = GuiManager.inst().getOpeningGui(player);
            if (gui instanceof IGuiRefreshable) {
                ((IGuiRefreshable) gui).refreshGui();
            }
        }
    }
}
