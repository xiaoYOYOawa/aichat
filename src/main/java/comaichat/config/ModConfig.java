// File: ModConfig.java
package comaichat.config;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import comaichat.Aichat;
import net.fabricmc.loader.api.FabricLoader;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static File configFile;

    // API 配置
    public static String apiKey = "YOUR_API_KEY_HERE";
    public static String apiUrl = "https://api.openai.com/v1/chat/completions";
    public static String modelName = "gpt-3.5-turbo";
    public static double temperature = 0.7;
    public static int maxTokens = 500;

    // 指令执行配置
    public static boolean allowCommandExecution = false; // 默认关闭，需要手动开启
    public static int minPermissionLevel = 2; // 默认需要OP权限
    public static boolean enableCommandLogging = true; // 记录指令执行日志

    public static void loadConfig() {
        configFile = new File(FabricLoader.getInstance().getConfigDir().toFile(), "aichat/aichatmod.json");

        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile, StandardCharsets.UTF_8)) {
                ConfigData configData = GSON.fromJson(reader, ConfigData.class);
                if (configData != null) {
                    // API 配置
                    apiKey = configData.apiKey != null ? configData.apiKey : apiKey;
                    apiUrl = configData.apiUrl != null ? configData.apiUrl : apiUrl;
                    modelName = configData.modelName != null ? configData.modelName : modelName;
                    temperature = configData.temperature;
                    maxTokens = configData.maxTokens;

                    // 指令执行配置
                    allowCommandExecution = configData.allowCommandExecution;
                    minPermissionLevel = configData.minPermissionLevel;
                    enableCommandLogging = configData.enableCommandLogging;
                }
                Aichat.LOGGER.info("AI Chat Mod 配置已加载");
            } catch (IOException e) {
                Aichat.LOGGER.error("无法加载配置文件", e);
            }
        } else {
            saveConfig();
            Aichat.LOGGER.info("创建了默认配置文件");
        }
    }

    public static void saveConfig() {
        try {
            configFile.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(configFile, StandardCharsets.UTF_8)) {
                ConfigData configData = new ConfigData();
                GSON.toJson(configData, writer);
                Aichat.LOGGER.info("AI Chat Mod 配置已保存至: {}", configFile.getAbsolutePath());
            }
        } catch (IOException e) {
            Aichat.LOGGER.error("无法保存配置文件", e);
        }
    }

    // 配置数据类
    public static class ConfigData {
        // API 配置
        public String apiKey = ModConfig.apiKey;
        public String apiUrl = ModConfig.apiUrl;
        public String modelName = ModConfig.modelName;
        public double temperature = ModConfig.temperature;
        public int maxTokens = ModConfig.maxTokens;

        // 指令执行配置
        public boolean allowCommandExecution = ModConfig.allowCommandExecution;
        public int minPermissionLevel = ModConfig.minPermissionLevel;
        public boolean enableCommandLogging = ModConfig.enableCommandLogging;

        public ConfigData() {}
    }

    // 配置验证方法
    public static boolean setAllowCommandExecution(boolean allow) {
        allowCommandExecution = allow;
        saveConfig();
        return true;
    }

    public static boolean setMinPermissionLevel(int level) {
        if (level < 0 || level > 4) {
            return false;
        }
        minPermissionLevel = level;
        saveConfig();
        return true;
    }
}
