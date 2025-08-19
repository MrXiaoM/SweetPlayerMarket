package top.mrxiaom.sweet.playermarket.func;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.AdventureUtil;
import top.mrxiaom.pluginbase.utils.ListPair;
import top.mrxiaom.pluginbase.utils.PAPI;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.data.MarketItem;

import java.util.List;

@AutoRegister
public class NoticeManager extends AbstractModule implements Listener {
    public static class Notice {
        private final String text;
        private final List<String> hover;
        private final String click;
        public Notice(String text, List<String> hover, String click) {
            this.text = text;
            this.hover = hover;
            this.click = click;
        }

        public boolean isEmpty() {
            return text.isEmpty();
        }

        public void send(Player player, @Nullable ListPair<String, Object> r) {
            String text = r == null ? this.text : Pair.replace(this.text, r);
            List<String> hover = r == null ? this.hover : Pair.replace(this.hover, r);
            Component builder = AdventureUtil.miniMessage(PAPI.setPlaceholders(player, text));
            if (!hover.isEmpty()) {
                TextComponent.Builder textBuilder = Component.text();
                List<Component> hoverList = AdventureUtil.miniMessage(hover);
                textBuilder.append(hoverList.get(0));
                for (int i = 1; i < hoverList.size(); i++) {
                    textBuilder.appendNewline();
                    textBuilder.append(hoverList.get(i));
                }
                builder = builder.hoverEvent(textBuilder.build().asHoverEvent());
            }
            if (!click.isEmpty()) {
                builder = builder.clickEvent(ClickEvent.runCommand(click));
            }
            AdventureUtil.of(player).sendMessage(builder);
        }

        public static Notice from(ConfigurationSection config, String prefix) {
            String text = config.getString(prefix + ".text", "");
            List<String> hover = config.getStringList(prefix + ".hover");
            String click = config.getString(prefix + ".click", "");
            return new Notice(text, hover, click);
        }
    }
    Notice noticeOnJoin;
    public NoticeManager(SweetPlayerMarket plugin) {
        super(plugin);
        registerEvents();
    }

    @Override
    public void reloadConfig(MemoryConfiguration config) {
        noticeOnJoin = Notice.from(config, "notice.on-join");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        if (noticeOnJoin.isEmpty()) return;
        List<MarketItem> items = plugin.getMarketplace()
                .queryItems(1, 1)
                .player(player)
                .notice(1)
                .search();
        if (!items.isEmpty()) {
            noticeOnJoin.send(player, null);
        }
    }

    public static NoticeManager inst() {
        return instanceOf(NoticeManager.class);
    }
}
