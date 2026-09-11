package com.shiguang;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shiguang.block.ModBlockEntities;
import com.shiguang.block.ModBlocks;
import com.shiguang.block.entity.AutoHarvesterBlockEntity;
import com.shiguang.network.CropTogglePayload;
import com.shiguang.screen.ModScreenHandlers;

/**
 * Auto Harvester 模组主入口类。
 * 负责初始化方块、方块实体、菜单类型，并注册网络通信。
 */
public class AutoHarvester implements ModInitializer {
	public static final String MOD_ID = "auto-harvester";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModBlocks.initialize();
		ModBlockEntities.initialize();
		ModScreenHandlers.initialize();

		// 注册作物切换数据包类型（客户端 -> 服务端）
		PayloadTypeRegistry.serverboundPlay().register(CropTogglePayload.TYPE, CropTogglePayload.CODEC);

		// 服务端接收作物开关数据包，同步 GUI 切换状态到方块实体
		ServerPlayNetworking.registerGlobalReceiver(CropTogglePayload.TYPE, (payload, context) -> {
			context.server().execute(() -> {
				var player = context.player();
				if (player.containerMenu instanceof com.shiguang.screen.AutoHarvesterScreenHandler handler) {
					handler.setCropEnabled(payload.cropIndex(), payload.enabled());
					if (player.level().getBlockEntity(handler.getBlockPos()) instanceof AutoHarvesterBlockEntity entity) {
						entity.setCropEnabled(payload.cropIndex(), payload.enabled());
					}
				}
			});
		});

		LOGGER.info("Auto Harvester mod loaded!");
	}

	/**
	 * 工具方法：根据路径生成模组命名空间下的 Identifier。
	 */
	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
