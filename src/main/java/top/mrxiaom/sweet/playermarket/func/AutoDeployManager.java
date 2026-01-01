package top.mrxiaom.sweet.playermarket.func;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import top.mrxiaom.pluginbase.api.IRunTask;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.ConfigUtils;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.data.deploy.AutoDeployProperty;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

@AutoRegister
public class AutoDeployManager extends AbstractModule {
    private final File configFile, dataFile;
    private boolean enable = false;
    private boolean debug = false;
    private final Map<String, AutoDeployProperty> configList = new HashMap<>();
    private IRunTask timerTask = null;
    public AutoDeployManager(SweetPlayerMarket plugin) {
        super(plugin);
        this.configFile = plugin.resolve("./auto-deploy.yml");
        this.dataFile = plugin.resolve("./auto-deploy.data.yml");
    }

    public boolean isEnable() {
        return enable;
    }

    public Set<String> keys() {
        return configList.keySet();
    }

    public AutoDeployProperty get(String id) {
        return configList.get(id);
    }

    @Override
    public void reloadConfig(MemoryConfiguration pluginCfg) {
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
        if (!configFile.exists()) {
            plugin.saveResource("auto-deploy.yml", configFile);
        }
        YamlConfiguration config = ConfigUtils.load(configFile);
        this.enable = config.getBoolean("enable", false);
        this.debug = config.getBoolean("debug", false);
        if (!enable) return;
        YamlConfiguration dataConfig = ConfigUtils.load(dataFile);

        ConfigurationSection section = config.getConfigurationSection("auto-deploy-properties");
        if (section != null) for (String key : section.getKeys(false)) {
            ConfigurationSection sec = section.getConfigurationSection(key);
            if (sec == null) continue;
            try {
                AutoDeployProperty property = new AutoDeployProperty(plugin, key, sec, dataConfig);
                this.configList.put(key, property);
            } catch (Throwable t) {
                warn("加载自动上架配置 " + key + " 时出现异常", t);
            }
        }
        info("已加载 " + configList.size() + " 个自动上架配置");
        timerTask = plugin.getScheduler().runTaskTimerAsync(this::doSchedulerCheck, 1L, config.getLong("check-period", 20));
    }

    public void doSchedulerCheck() {
        boolean save = false;
        List<AutoDeployProperty> list = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (AutoDeployProperty property : configList.values()) {
            // 不在指定时间内的定时器不运行
            if (property.isNotInTime(now)) {
                if (debug) info("[调试] 自动上架配置 " + property.getId() + " 不在允许的运行时间以内");
                continue;
            }
            AutoDeployProperty.Data data = property.getData();
            // 判定成功次数已到达指定次数的定时器不运行
            if (property.getSuccessRunRound() >= 0 && data.getSuccessRoundCount() >= property.getSuccessRunRound()) {
                if (debug) info("[调试] 自动上架配置 " + property.getId() + " 已到达允许运行次数上限");
                continue;
            }
            long currentPassRound = property.getCurrentPassRound(now);
            long lastRound = data.getLastRound();
            if (currentPassRound > lastRound) {
                save = true;
                data.setLastRound(currentPassRound);
                if (currentPassRound - lastRound != 1) {
                    if (debug) info("[调试] 自动上架配置 " + property.getId() + " 上一轮次(" + lastRound + ")与当前轮次(" + currentPassRound + ")相差大于1，服务器可能经过重启或修改配置，不执行自动上架操作");
                    continue;
                }
                long currentRoundSeconds = property.getCurrentRoundSeconds(now);
                if (currentRoundSeconds <= property.getGivingUpGap().getTotalSeconds()) {
                    // 运行定时器检查
                    if (!property.doConditionCheck(now)) {
                        if (debug) info("[调试] 自动上架配置 " + property.getId() + " 的条件检查不通过");
                        continue;
                    }
                    if (debug) info("[调试] 自动上架配置 " + property.getId() + " 计划运行上架任务");

                    // 计划运行定时器任务
                    list.add(property);
                }
            }
        }
        if (!list.isEmpty()) {
            try (Connection conn = plugin.getConnection()){
                for (AutoDeployProperty property : list) {
                    property.doDeployItem(conn, now);
                }
            } catch (SQLException e) {
                warn("执行自动上架商品时，数据库出现异常", e);
            }
        }
        if (save) saveData();
    }

    /**
     * 保存自动上架的定时器数据
     */
    public void saveData() {
        YamlConfiguration data = new YamlConfiguration();
        for (AutoDeployProperty property : configList.values()) {
            property.getData().save(data);
        }
        try {
            ConfigUtils.save(data, dataFile);
        } catch (IOException e) {
            warn(e);
        }
    }

    @Override
    public void onDisable() {
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
    }

    public static AutoDeployManager inst() {
        return instanceOf(AutoDeployManager.class);
    }
}
