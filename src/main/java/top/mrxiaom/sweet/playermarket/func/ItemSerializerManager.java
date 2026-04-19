package top.mrxiaom.sweet.playermarket.func;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

@AutoRegister
public class ItemSerializerManager extends AbstractModule {
    private boolean checkOnCreate;
    private boolean useNBTAPI;
    public ItemSerializerManager(SweetPlayerMarket plugin) {
        super(plugin);
    }

    @Override
    public void reloadConfig(MemoryConfiguration config) {
        this.checkOnCreate = config.getBoolean("item-serializer.check-on-create", true);
        this.useNBTAPI = config.getBoolean("item-serializer.use-nbt-api", true);
    }

    public boolean isCheckOnCreate() {
        return checkOnCreate;
    }

    public boolean isUseNBTAPI() {
        return useNBTAPI;
    }

    @Nullable
    public ItemStack getItem(@NotNull ConfigurationSection config) {
        if (config.contains("item")) {
            return config.getItemStack("item");
        }
        if (config.contains("item-nbt")) {
            String itemNbt = config.getString("item-nbt");
            if (itemNbt != null) {
                byte[] decoded = Base64.getDecoder().decode(itemNbt);
                try (ByteArrayInputStream in = new ByteArrayInputStream(decoded)) {
                    ReadWriteNBT nbt = NBT.readNBT(in);
                    return NBT.itemStackFromNBT(nbt);
                } catch (IOException e) {
                    return null;
                }
            }
        }
        return null;
    }

    public void setItem(@NotNull ConfigurationSection config, @Nullable ItemStack item) {
        if (item == null) {
            config.set("item", null);
            config.set("item-nbt", null);
            return;
        }
        if (useNBTAPI) {
            config.set("item", null);
            ReadWriteNBT nbt = NBT.itemStackToNBT(item);
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                nbt.writeCompound(out);
                config.set("item-nbt", Base64.getEncoder().encodeToString(out.toByteArray()));
            } catch (IOException e) {
                return;
            }
            return;
        }
        config.set("item", item);
    }

    public static ItemSerializerManager inst() {
        return instanceOf(ItemSerializerManager.class);
    }
}
