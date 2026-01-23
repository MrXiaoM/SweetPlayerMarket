package top.mrxiaom.sweet.playermarket.actions;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.api.IAction;
import top.mrxiaom.pluginbase.api.IActionProvider;
import top.mrxiaom.pluginbase.func.GuiManager;
import top.mrxiaom.pluginbase.gui.IGuiHolder;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.sweet.playermarket.gui.api.IGuiCanGoBack;

import java.util.List;

public class ActionBack implements IAction {
    public static final ActionBack INSTANCE = new ActionBack();
    public static final IActionProvider PROVIDER = (s) -> {
        if (s.equals("[back]") || s.equals("back")) return INSTANCE;
        return null;
    };
    private ActionBack() {}

    @Override
    public void run(@Nullable Player player, @Nullable List<Pair<String, Object>> replacements) {
        IGuiHolder gui = GuiManager.inst().getOpeningGui(player);
        if (gui instanceof IGuiCanGoBack) {
            ((IGuiCanGoBack) gui).goBack();
        }
    }
}
