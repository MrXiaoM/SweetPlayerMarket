package top.mrxiaom.sweet.playermarket.commands.arguments;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import top.mrxiaom.pluginbase.utils.arguments.Arguments;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.api.AbstractArguments;
import top.mrxiaom.sweet.playermarket.data.NoticeFlag;
import top.mrxiaom.sweet.playermarket.data.Searching;
import top.mrxiaom.sweet.playermarket.gui.GuiMyItems;

public class MeArguments extends AbstractArguments<CommandSender> {
    private static final Arguments.Builder builder = Arguments.builder()
            .addBooleanOption("notice", "-n", "--notice")
            .addBooleanOption("out-of-stock", "-o", "--only-out-of-stock");
    private final boolean notice, outOfStock;
    protected MeArguments(Arguments arguments) {
        super(arguments);
        notice = arguments.getOptionBoolean("notice");
        outOfStock = arguments.getOptionBoolean("out-of-stock");
    }

    public boolean notice() {
        return notice;
    }

    public boolean onlyOutOfStock() {
        return outOfStock;
    }

    @Override
    public boolean execute(SweetPlayerMarket plugin, CommandSender sender) {
        Player player = getPlayerOrSelf(sender, "sweet.playermarket.me.other");
        if (player == null) {
            return true;
        }
        GuiMyItems.create(player, Searching.of(false)
                .playerId(plugin.getKey(player))
                .noticeFlag(notice() ? NoticeFlag.CAN_CLAIM_ITEMS : null)
                .onlyOutOfStock(onlyOutOfStock())
        ).open();
        return true;
    }

    public static MeArguments of(String[] args) {
        return builder.build(MeArguments::new, args);
    }
}
