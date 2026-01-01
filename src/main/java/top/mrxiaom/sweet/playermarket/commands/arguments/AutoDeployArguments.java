package top.mrxiaom.sweet.playermarket.commands.arguments;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import top.mrxiaom.pluginbase.utils.arguments.CommandArguments;
import top.mrxiaom.sweet.playermarket.Messages;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.api.AbstractArguments;
import top.mrxiaom.sweet.playermarket.data.deploy.AutoDeployProperty;
import top.mrxiaom.sweet.playermarket.data.deploy.DeployContext;
import top.mrxiaom.sweet.playermarket.func.AutoDeployManager;

import java.time.LocalDateTime;

import static top.mrxiaom.pluginbase.func.AbstractPluginHolder.t;

public class AutoDeployArguments extends AbstractArguments<CommandSender> {
    protected AutoDeployArguments(CommandArguments arguments) {
        super(arguments);
    }

    @Override
    public boolean execute(SweetPlayerMarket plugin, CommandSender sender) {
        String propertyName = nextString();
        if (propertyName == null) {
            return t(sender, "&c请输入自动上架配置名");
        }
        AutoDeployManager manager = AutoDeployManager.inst();
        AutoDeployProperty property = manager.get(propertyName);
        if (property == null) {
            return t(sender, "&c找不到自动上架配置 &e" + propertyName);
        }
        String operation = nextString();
        if ("print".equalsIgnoreCase(operation)) {
            property.print(sender);
            return true;
        }
        if ("condition".equalsIgnoreCase(operation)) {
            if (property.getSuccessRunRound() >= 0 && property.getData().getSuccessRoundCount() >= property.getSuccessRunRound()) {
                return t(sender, "&e自动上架配置 &b" + property.getId() + " &e已到达允许运行次数上限");
            }
            int reason = property.doConditionCheckWithReason(LocalDateTime.now());
            switch (reason) {
                case 0:
                    return t(sender, "&a自动上架配置 &e" + property.getId() + "&a 条件测试通过");
                case 1:
                    return t(sender, "&e自动上架配置 &b" + property.getId() + "&e 表达式条件测试不通过");
                case 2:
                    return t(sender, "&e自动上架配置 &b" + property.getId() + "&e 概率条件测试不通过");
                case 3:
                    return t(sender, "&e自动上架配置 &b" + property.getId() + "&e 星期条件测试不通过");
                case 4:
                    return t(sender, "&e自动上架配置 &b" + property.getId() + "&e 月份条件测试不通过");
            }
            return t(sender, "&e自动上架配置 &b" + property.getId() + "&e 条件测试不通过，未知的测试结果代码 &b" + reason);
        }
        if ("test".equalsIgnoreCase(operation)) {
            try {
                DeployContext ctx = property.createContext(LocalDateTime.now());
                t(sender, "&a&l模拟上架商品",
                        "&f  店主名字: &e" + (ctx.getServerCustomName().isEmpty() ? Messages.server_owner_name.str() : ctx.getServerCustomName()),
                        "&f  类型: &e" + plugin.displayNames().getMarketTypeName(ctx.getType()),
                        "&f  单价: &e" + String.format("%.2f", ctx.getPrice()) + " " + plugin.displayNames().getCurrencyName(ctx.getCurrencyType()),
                        "&f  总份数: &e" + ctx.getAmount(),
                        "&f  标签: &e" + (ctx.getTag().isEmpty() ? "(默认)" : ctx.getTag()),
                        "&f  到期时间: &e" + ctx.getOutdateTime().format(AutoDeployProperty.DATE_TIME_FORMAT),
                        "&f  物品名: &e" + plugin.displayNames().getDisplayName(ctx.getItem(), sender instanceof Player ? (Player) sender : null),
                        "");
            } catch (Throwable t) {
                if (!(sender instanceof ConsoleCommandSender)) {
                    t(sender, "测试模拟上架 " + property.getId() + " 时出现异常，详见控制台日志: " + t.getMessage());
                }
                plugin.warn("测试模拟上架 " + property.getId() + " 时出现异常", t);
            }
            return true;
        }
        return t(sender, "&e无效的操作，仅支持&b print, condition, test",
                "&e该命令属于测试命令，不要要求添加本地化支持");
    }

    public static AutoDeployArguments of(CommandArguments args) {
        return new AutoDeployArguments(args);
    }
}
