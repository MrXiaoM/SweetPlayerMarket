package top.mrxiaom.sweet.playermarket.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.sweet.playermarket.economy.IEconomy;

/**
 * 经济接口处理器
 */
public interface IEconomyResolver {
    /**
     * 处理优先级，数值越低越先处理，默认 <code>1000</code>
     */
    default int priority() {
        return 1000;
    }

    /**
     * 从字符串读取经济接口实现
     * @param str 输入的字符串
     * @return 经济接口实现，建议返回唯一的实例，而不是每次返回都新建一个。如果返回 <code>null</code>，代表输入的字符串不匹配该经济接口，插件会向其它的经济接口处理器寻找经济接口实现
     */
    @Nullable IEconomy parse(@NotNull String str);

    /**
     * 从字符串读取经济接口名字，同 <code>parse</code>
     * @param str 输入的字符串
     * @return 经济接口名字，如果返回 <code>null</code>，代表输入的字符串不匹配该经济接口，插件会向其它的经济接口处理器寻找经济接口名字
     */
    @Nullable String parseName(String str);

    /**
     * 从经济接口实现读取经济接口名字
     * @param economy 经济接口实现
     * @return 经济接口名字，如果返回 <code>null</code>，代表输入的字符串不匹配该经济接口，插件会向其它的经济接口处理器寻找经济接口名字
     */
    @Nullable String getName(IEconomy economy);
}
