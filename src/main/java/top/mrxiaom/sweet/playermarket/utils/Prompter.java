package top.mrxiaom.sweet.playermarket.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;

import java.util.function.Consumer;

@ApiStatus.Internal
public class Prompter implements Listener {
    public static void chat(
            @NotNull Player player,
            @Nullable Consumer<String> onSuccess,
            @Nullable Runnable onFailed
    ) {
        chat(player, "cancel", onSuccess, onFailed);
    }

    public static void chat(
            @NotNull Player player,
            @NotNull String cancelWord,
            @Nullable Consumer<String> onSuccess,
            @Nullable Runnable onFailed
    ) {
        SweetPlayerMarket plugin = SweetPlayerMarket.getInstance();
        Bukkit.getPluginManager().registerEvents(new Chat(player, cancelWord, onSuccess, onFailed), plugin);
    }

    public static class Chat implements Listener {
        private final @NotNull Player player;
        private final @NotNull String cancelWord;
        private final @Nullable Consumer<String> onSuccess;
        private final @Nullable Runnable onFailed;
        private Chat(@NotNull Player player, @NotNull String cancelWord, @Nullable Consumer<String> onSuccess, @Nullable Runnable onFailed) {
            this.player = player;
            this.cancelWord = cancelWord;
            this.onSuccess = onSuccess;
            this.onFailed = onFailed;
        }

        public boolean isSamePlayer(HumanEntity player) {
            return player instanceof Player && this.player.getUniqueId().equals(player.getUniqueId());
        }

        private boolean check(HumanEntity player) {
            if (isSamePlayer(player)) {
                HandlerList.unregisterAll(this);
                return true;
            } else {
                return false;
            }
        }

        @EventHandler
        public void onQuit(PlayerQuitEvent e) {
            check(e.getPlayer());
        }

        @EventHandler
        public void onOpenInventory(InventoryOpenEvent e) {
            check(e.getPlayer());
        }

        @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
        public void onChat(AsyncPlayerChatEvent e) {
            if (check(e.getPlayer())) {
                e.setCancelled(true);
                String message = e.getMessage().trim();
                if (message.equals(cancelWord)) {
                    if (onFailed != null) onFailed.run();
                } else {
                    if (onSuccess != null) onSuccess.accept(message);
                }
            }
        }
    }
}
