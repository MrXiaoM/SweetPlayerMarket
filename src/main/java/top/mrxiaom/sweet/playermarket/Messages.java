package top.mrxiaom.sweet.playermarket;

import top.mrxiaom.pluginbase.func.language.Language;
import top.mrxiaom.pluginbase.func.language.Message;

import static top.mrxiaom.pluginbase.func.language.LanguageFieldAutoHolder.field;

@Language(prefix="messages.")
public class Messages {

    public static final Message player__not_online = field("&e玩家不在线 (或不存在)");
    public static final Message player__only = field("只有玩家可以执行该命令");

    @Language(prefix="messages.command.")
    public static class Command {
        public static final Message no_permission = field("&c你没有执行该命令的权限");

        public static final Message create__no_item = field("&e请手持你要上架的物品");
        public static final Message create__no_type_found = field("&e请输入正确的商品类型");
        public static final Message create__no_price_valid = field("&e请输入正确的价格");
        public static final Message create__no_currency_default = field("&e找不到默认货币类型，请联系服务器管理员");
        public static final Message create__no_currency_found = field("&e请输入正确的货币类型");
        public static final Message create__no_currency_permission = field("&e你没有使用该货币上架商品的权限");
        public static final Message create__no_item_count_valid = field("&e请输入正确的单个商品的物品数量");
        public static final Message create__no_item_count_valid_stack = field("&e请输入正确的单个商品的物品数量，你输入的数量超出了堆叠限制");
        public static final Message create__no_item_count_valid_held = field("&e请输入正确的单个商品的物品数量，你输入的数量超出了手持物品数量");
        public static final Message create__no_amount_valid = field("&e请输入正确的商品总份数");
        public static final Message create__limitation__type_not_allow = field("&e在上架该物品时，禁止上架到这个类型的商店");
        public static final Message create__limitation__currency_not_allow = field("&e在上架该物品时，禁止使用%currency%上架");
        public static final Message create__limitation__create_cost_failed = field("&e你的%currency%不足，需要支付 %money% %currency% 的上架手续费");
        public static final Message create__sell__no_enough_items = field("&e你没有足够的物品来上架商品");
        public static final Message create__buy__no_enough_currency = field("&e你没有足够的货币来上架商品");
        public static final Message create__success = field("&a你已成功上架&e <item>%item%</item>&r&a 到全球市场!");
        public static final Message create__failed_db = field("&e无法创建新的商品 ID，请稍后重试");
        public static final Message create__failed = field("&e商品上架失败，请联系服务器管理员");

        public static final Message recalc__start = field("&a开始执行商品标签重新计算操作，请稍等…");
        public static final Message recalc__success = field("&a重新计算完成，有 %count% 个商品的标签发生变动");
        public static final Message recalc__failed = field("&e执行重新计算时出现一个错误，请查阅控制台日志");

        public static final Message reload__database = field("&a已重载 database.yml 并重新连接数据库");
        public static final Message reload__success = field("&a配置文件已重载");
    }

    @Language(prefix="messages.tab-complete.")
    public static class TabComplete {
        public static final Message create__price = field("<价格>");
        public static final Message create__item_count = field("[单份商品的物品数量]");
        public static final Message create__amount = field("[总份数]");
    }

    @Language(prefix="messages.gui.")
    public static class Gui {
        public static final Message common__item_not_found = field("&e来晚了，该商品已下架");
        public static final Message common__currency_not_found = field("&e在该子服不支持使用%currency%货币");
        public static final Message common__yes = field("&a是");
        public static final Message common__no = field("&c否");

        public static final Message sell__amount_not_enough = field("&e商品库存不足，减少一点购买数量吧~");
        public static final Message sell__currency_not_enough = field("&e你没有足够的%currency%");
        public static final Message sell__submit_failed = field("&e数据库更改提交失败，可能该商品已下架");
        public static final Message sell__exception = field("&e出现错误，已打印日志到控制台，请联系服务器管理员");
        public static final Message sell__success = field("&a你已成功购买&e <item>%item%</item>&r&e x%total_count%&a，花费&e %money% %currency%");

        public static final Message buy__amount_not_enough = field("&e商品库存空间不足，减少一点卖出数量吧~");
        public static final Message buy__item_not_enough = field("&e你没有足够的物品来卖出");
        public static final Message buy__submit_failed = field("&e数据库更改提交失败，可能该商品已下架");
        public static final Message buy__exception = field("&e出现错误，已打印日志到控制台，请联系服务器管理员");
        public static final Message buy__success = field("&a你已成功卖出&e <item>%item%</item>&r&e x%total_count%&a，获得&e %money% %currency%");

        public static final Message me__claim__exception = field("&e出现错误，已打印日志到控制台，请联系服务器管理员");
        public static final Message me__claim__plugin_too_old = field("&e这个子服的插件太老了，无法领取这个类型的商品");
        public static final Message me__claim__buy__success = field("&a你已成功领取&e <item>%item%</item>&r&e x%total_count%");
        public static final Message me__claim__sell__success = field("&a你已成功领取&e %money% %currency%");
    }

}
