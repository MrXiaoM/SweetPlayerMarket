package top.mrxiaom.sweet.playermarket.actions;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.api.IAction;
import top.mrxiaom.pluginbase.api.IActionProvider;
import top.mrxiaom.pluginbase.func.GuiManager;
import top.mrxiaom.pluginbase.gui.IGuiHolder;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.playermarket.gui.api.IGuiConfirm;

import java.util.List;

public class ActionConfirmCount implements IAction {
    public static final IActionProvider PROVIDER = (s) -> {
        if (s.startsWith("[count]")) {
            return parse(s.substring(7));
        }
        if (s.startsWith("count:")) {
            return parse(s.substring(6));
        }
        return null;
    };

    private static IAction parse(String params) {
        if (params.startsWith("-")) {
            String s = params.substring(1);
            if (s.equals("max")) {
                return new ActionConfirmCount(Operator.MINUS, null);
            }
            return Util.parseInt(s)
                    .map(i -> new ActionConfirmCount(Operator.MINUS, i))
                    .orElse(null);
        }
        if (params.startsWith("+")) {
            String s = params.substring(1);
            if (s.equals("max")) {
                return new ActionConfirmCount(Operator.ADD, null);
            }
            return Util.parseInt(s)
                    .map(i -> new ActionConfirmCount(Operator.ADD, i))
                    .orElse(null);
        }
        Integer i = Util.parseInt(params).orElse(null);
        if (i != null) {
            return new ActionConfirmCount(Operator.SET, i);
        }
        return null;
    }
    public enum Operator {
        SET, ADD, MINUS
    }
    private final Operator operator;
    private final Integer count;
    public ActionConfirmCount(Operator operator, Integer count) {
        this.operator = operator;
        this.count = count;
    }

    @Override
    public void run(Player player, @Nullable List<Pair<String, Object>> replacements) {
        if (player != null) {
            IGuiHolder gui = GuiManager.inst().getOpeningGui(player);
            if (gui instanceof IGuiConfirm) {
                IGuiConfirm confirm = (IGuiConfirm) gui;
                switch (operator) {
                    case SET:
                        confirm.countSet(count);
                        break;
                    case ADD:
                        if (count == null) {
                            confirm.countAddMax();
                        } else {
                            confirm.countAdd(count);
                        }
                        break;
                    case MINUS:
                        if (count == null) {
                            confirm.countMinusMax();
                        } else {
                            confirm.countMinus(count);
                        }
                        break;
                }
            }
        }
    }
}
