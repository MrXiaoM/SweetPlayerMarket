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

public class ActionDeployPrice implements IAction {
    public static final IActionProvider PROVIDER = (s) -> {
        if (s.startsWith("[price]")) {
            return parse(s.substring(7), (operation, value) -> (gui) -> gui.modifyPrice(operation, value));
        }
        return null;
    };

    private static IAction parse(String params, BiFunction<IGuiDeploy.NumberOperation, Double, Consumer<IGuiDeploy>> func) {
        if (params.startsWith("-")) {
            String s = params.substring(1);
            return Util.parseDouble(s)
                    .map(i -> new ActionDeployPrice(func.apply(IGuiDeploy.NumberOperation.MINUS, i)))
                    .orElse(null);
        }
        if (params.startsWith("+")) {
            String s = params.substring(1);
            return Util.parseDouble(s)
                    .map(i -> new ActionDeployPrice(func.apply(IGuiDeploy.NumberOperation.ADD, i)))
                    .orElse(null);
        }
        Double i = Util.parseDouble(params).orElse(null);
        if (i != null) {
            return new ActionDeployPrice(func.apply(IGuiDeploy.NumberOperation.SET, i));
        }
        return null;
    }
    private final Consumer<IGuiDeploy> consumer;
    public ActionDeployPrice(Consumer<IGuiDeploy> consumer) {
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
