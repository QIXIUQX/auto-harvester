package com.shiguang.client;

import net.fabricmc.api.ClientModInitializer;

import com.shiguang.client.screen.AutoHarvesterScreen;
import com.shiguang.screen.ModScreenHandlers;

import net.minecraft.client.gui.screens.MenuScreens;

/**
 * 客户端入口类。
 * 负责注册客户端专属功能，如 GUI Screen 绑定。
 */
public class AutoHarvesterClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// 将 MenuType 与 Screen 绑定，客户端打开菜单时自动创建 AutoHarvesterScreen
		MenuScreens.register(ModScreenHandlers.AUTO_HARVESTER, AutoHarvesterScreen::new);
	}
}
