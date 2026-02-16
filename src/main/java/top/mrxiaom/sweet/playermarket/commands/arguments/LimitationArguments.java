package top.mrxiaom.sweet.playermarket.commands.arguments;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import top.mrxiaom.pluginbase.utils.AdventureUtil;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.pluginbase.utils.arguments.CommandArguments;
import top.mrxiaom.sweet.playermarket.Messages;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.api.AbstractArguments;
import top.mrxiaom.sweet.playermarket.data.EnumMarketType;
import top.mrxiaom.sweet.playermarket.data.limitation.BaseLimitation;
import top.mrxiaom.sweet.playermarket.data.limitation.CreateCost;
import top.mrxiaom.sweet.playermarket.func.LimitationManager;

import java.util.ArrayList;
import java.util.List;

public class LimitationArguments extends AbstractArguments<Player> {
    protected LimitationArguments(CommandArguments args) {
        super(args);
    }

    @Override
    public boolean execute(SweetPlayerMarket plugin, Player sender) {
        // noinspection deprecation
        ItemStack item = sender.getItemInHand();
        // noinspection ConstantValue
        if (item == null || item.getAmount() == 0 || item.getType().equals(Material.AIR)) {
            return Messages.Command.limitation__no_item.tm(sender);
        }
        BaseLimitation limit = LimitationManager.inst().getLimitByItem(item);
        for (String s : limit.getDescription()) {
            AdventureUtil.sendMessage(sender, s);
        }
        List<String> typeStr = new ArrayList<>();
        for (EnumMarketType type : EnumMarketType.values()) {
            String name = plugin.displayNames().getMarketTypeName(type);
            if (limit.canUseMarketType(type)) {
                typeStr.add(name);
                CreateCost createCost = limit.getCreateCost(type);
                if (createCost != null) {
                    Messages.Command.limitation__create_cost.tm(sender, Pair.of("%type%", name));
                }
            }
        }
        Messages.Command.limitation__can_use_type.tm(sender, Pair.of("%types%", typeStr.isEmpty() ? "NONE" : String.join("&r, ", typeStr)));
        return true;
    }

    public static LimitationArguments of(CommandArguments args) {
        return new LimitationArguments(args);
    }
}
