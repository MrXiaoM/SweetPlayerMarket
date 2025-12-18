package top.mrxiaom.sweet.playermarket.actions;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.api.IAction;
import top.mrxiaom.pluginbase.api.IActionProvider;
import top.mrxiaom.pluginbase.func.GuiManager;
import top.mrxiaom.pluginbase.gui.IGuiHolder;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.playermarket.gui.api.IGuiDeploy;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class ActionDeployCount implements IAction {
    public static final IActionProvider PROVIDER = (s) -> {
        if (s.startsWith("[count:amount]")) {
            return parse(s.substring(14), (operation, value) -> (gui) -> gui.modifyAmount(operation, value));
        }
        if (s.startsWith("[count:item]")) {
            return parse(s.substring(12), (operation, value) -> (gui) -> gui.modifyItemCount(operation, value));
        }
        return null;
    };

    private static IAction parse(String params, BiFunction<IGuiDeploy.NumberOperation, Integer, Consumer<IGuiDeploy>> func) {
        if (params.startsWith("-")) {
            String s = params.substring(1);
            return Util.parseInt(s)
                    .map(i -> new ActionDeployCount(func.apply(IGuiDeploy.NumberOperation.MINUS, i)))
                    .orElse(null);
        }
        if (params.startsWith("+")) {
            String s = params.substring(1);
            return Util.parseInt(s)
                    .map(i -> new ActionDeployCount(func.apply(IGuiDeploy.NumberOperation.ADD, i)))
                    .orElse(null);
        }
        Integer i = Util.parseInt(params).orElse(null);
        if (i != null) {
            return new ActionDeployCount(func.apply(IGuiDeploy.NumberOperation.SET, i));
        }
        return null;
    }
    private final Consumer<IGuiDeploy> consumer;
    public ActionDeployCount(Consumer<IGuiDeploy> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void run(Player player, @Nullable List<Pair<String, Object>> replacements) {
        if (player != null) {
            IGuiHolder gui = GuiManager.inst().getOpeningGui(player);
            if (gui instanceof IGuiDeploy) {
                consumer.accept((IGuiDeploy) gui);
            }
        }
    }
}
