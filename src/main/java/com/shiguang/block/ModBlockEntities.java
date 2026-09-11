package com.shiguang.block;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import com.shiguang.AutoHarvester;
import com.shiguang.block.custom.AutoHarvesterBlock;
import com.shiguang.block.entity.AutoHarvesterBlockEntity;

/**
 * 方块实体注册类。
 * 注册自动收割机的 BlockEntityType，用于关联方块和方块实体。
 */
public class ModBlockEntities {

	/** 自动收割机方块实体类型 */
	public static final BlockEntityType<AutoHarvesterBlockEntity> AUTO_HARVESTER =
			register("auto_harvester", AutoHarvesterBlockEntity::new, (AutoHarvesterBlock) ModBlocks.AUTO_HARVESTER);

	private static <T extends BlockEntity> BlockEntityType<T> register(
			String name,
			FabricBlockEntityTypeBuilder.Factory<? extends T> entityFactory,
			AutoHarvesterBlock... blocks
	) {
		Identifier id = AutoHarvester.id(name);
		return Registry.register(
				BuiltInRegistries.BLOCK_ENTITY_TYPE,
				id,
				FabricBlockEntityTypeBuilder.<T>create(entityFactory, blocks).build()
		);
	}

	/** 触发类加载以完成注册 */
	public static void initialize() {
	}
}
