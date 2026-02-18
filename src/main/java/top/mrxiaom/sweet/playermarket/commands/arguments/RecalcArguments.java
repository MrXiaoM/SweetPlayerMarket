package top.mrxiaom.sweet.playermarket.commands.arguments;

import org.bukkit.command.CommandSender;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.pluginbase.utils.arguments.CommandArguments;
import top.mrxiaom.sweet.playermarket.Messages;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.api.AbstractArguments;
import top.mrxiaom.sweet.playermarket.database.MarketplaceDatabase;

public class RecalcArguments extends AbstractArguments<CommandSender> {
    protected RecalcArguments(CommandArguments args) {
        super(args);
    }

    @Override
    public boolean execute(SweetPlayerMarket plugin, CommandSender sender) {
        String type = nextString("tags");
        if (type.equals("tags")) {
            Messages.Command.recalc__tags__start.tm(sender);
            plugin.getScheduler().runTaskAsync(() -> {
                MarketplaceDatabase db = plugin.getMarketplace();
                int count = db.recalculateItemsTag();
                if (count >= 0) {
                    Messages.Command.recalc__tags__success.tm(sender, Pair.of("%count%", count));
                } else {
                    Messages.Command.recalc__tags__failed.tm(sender);
                }
            });
            return true;
        }
        if (type.equals("index")) {
            Messages.Command.recalc__index__start.tm(sender);
            plugin.getScheduler().runTaskAsync(() -> {
                MarketplaceDatabase db = plugin.getMarketplace();
                int count = db.recalculateIndex();
                if (count >= 0) {
                    Messages.Command.recalc__index__success.tm(sender, Pair.of("%count%", count));
                } else {
                    Messages.Command.recalc__index__failed.tm(sender);
                }
            });
            return true;
        }
        return Messages.Command.recalc__invalid_type.tm(sender);
    }

    public static RecalcArguments of(CommandArguments args) {
        return new RecalcArguments(args);
    }
}
