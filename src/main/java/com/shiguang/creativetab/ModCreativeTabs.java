package com.shiguang.creativetab;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import com.shiguang.AutoHarvester;
import com.shiguang.block.ModBlocks;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;

/**
 * 创造模式物品栏分类（菜单）注册类。
 * <p>
 * 本模组的所有物品只出现在自己的分类中，不再混入原版分类。
 * <p>
 * 说明：Fabric 会自动为模组分类做分页与排版（见 CreativeModeTabsMixin），
 * 因此这里不需要手动指定行列位置。
 */
public class ModCreativeTabs {

	/** 本模组分类的注册键：auto-harvester:main */
	public static final ResourceKey<CreativeModeTab> MAIN = ResourceKey.create(
			Registries.CREATIVE_MODE_TAB,
			AutoHarvester.id("main"));

	/**
	 * 注册模组自己的创造模式分类。
	 * <p>
	 * 标题翻译键为 {@code itemGroup.auto-harvester.main}，
	 * 对应 {@code assets/auto-harvester/lang/} 下的语言文件。
	 */
	public static void initialize() {
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, MAIN,
				FabricCreativeModeTab.builder()
						.title(Component.translatable("itemGroup.auto-harvester.main"))
						.icon(() -> new ItemStack(ModBlocks.AUTO_HARVESTER))
						.displayItems((context, output) -> output.accept(ModBlocks.AUTO_HARVESTER))
						.build());
	}
}
