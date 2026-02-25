package comaichat;

import comaichat.config.ModConfig;
import comaichat.command.AIChatCommand;
import comaichat.listener.ChatListener;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Aichat implements ModInitializer {
	public static final String MOD_ID = "aichat";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("初始化 AI Chat Mod (Fabric)");

		// 加载配置
		ModConfig.loadConfig();

		// 注册命令
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			AIChatCommand.register(dispatcher);
		});
		ChatListener.register();


		LOGGER.info("AI Chat Mod 初始化完成");
	}
}