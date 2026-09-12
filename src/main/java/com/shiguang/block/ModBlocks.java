package com.shiguang.block;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import com.shiguang.AutoHarvester;
import com.shiguang.block.custom.AutoHarvesterBlock;

import java.util.function.Function;

/**
 * 方块注册类。
 * 负责注册自动收割机方块及其对应物品。
 * <p>
 * 物品的创造模式分类由 {@link com.shiguang.creativetab.ModCreativeTabs} 统一管理，
 * 本类不再把方块塞进原版分类。
 */
public class ModBlocks {

	/**
	 * 通用方块注册方法。
	 * 同时注册方块和对应的 BlockItem。
	 */
	private static Block register(String name, Function<BlockBehaviour.Properties, Block> blockFactory, BlockBehaviour.Properties properties) {
		Identifier id = AutoHarvester.id(name);
		Block block = blockFactory.apply(properties);
		Registry.register(BuiltInRegistries.BLOCK, id, block);

		BlockItem blockItem = new BlockItem(block, new Item.Properties()
				.useBlockDescriptionPrefix()
				.setId(ResourceKey.create(BuiltInRegistries.ITEM.key(), id)));
		Registry.register(BuiltInRegistries.ITEM, id, blockItem);

		return block;
	}

	/** 自动收割机方块：金属音效，硬度 4.0，爆炸抗性 6.0 */
	public static final Block AUTO_HARVESTER = register(
			"auto_harvester",
			AutoHarvesterBlock::new,
			BlockBehaviour.Properties.of()
					.setId(ResourceKey.create(BuiltInRegistries.BLOCK.key(), AutoHarvester.id("auto_harvester")))
					.sound(SoundType.METAL)
					.strength(4.0F, 6.0F)
					.requiresCorrectToolForDrops()
	);
}
