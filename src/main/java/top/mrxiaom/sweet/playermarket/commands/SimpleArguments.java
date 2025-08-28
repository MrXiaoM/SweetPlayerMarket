package top.mrxiaom.sweet.playermarket.commands;

import org.bukkit.command.CommandSender;
import top.mrxiaom.pluginbase.utils.arguments.CommandArguments;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.api.AbstractArguments;

public class SimpleArguments extends AbstractArguments<CommandSender> {
    protected SimpleArguments(CommandArguments args) {
        super(args);
    }

    @Override
    public boolean execute(SweetPlayerMarket plugin, CommandSender sender) {
        return true;
    }

    public static SimpleArguments of(String[] args) {
        return new SimpleArguments(CommandArguments.simple(args));
    }
}
