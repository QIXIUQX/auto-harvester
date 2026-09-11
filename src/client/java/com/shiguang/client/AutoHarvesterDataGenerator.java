package com.shiguang.client;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

/**
 * 数据生成入口类。
 * 可在此注册 RecipeProvider、LootTableProvider 等数据生成器。
 * 当前为空实现。
 */
public class AutoHarvesterDataGenerator implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
	}
}
