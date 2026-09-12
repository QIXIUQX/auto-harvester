package com.shiguang.screen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Items;

import com.shiguang.AutoHarvester;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;

/**
 * 菜单类型和作物数据定义类。
 * <p>
 * 职责：
 * - 注册 AutoHarvester 的 MenuType（使用 Fabric ExtendedMenuType 传递 BlockPos）
 * - 定义支持的 10 种作物 ID、显示名称和对应物品图标
 * - 定义 2 个附加开关（满箱停收、静音）的索引、名称和默认值
 * - 提供作物 ID → 索引的查询方法
 */
public class ModScreenHandlers {

	/** BlockPos 的网络编解码器 */
	public static final StreamCodec<FriendlyByteBuf, BlockPos> BLOCK_POS_STREAM_CODEC = StreamCodec.of(
			(buf, pos) -> buf.writeBlockPos(pos),
			buf -> buf.readBlockPos()
	);

	/** 自动收割机菜单类型（ExtendedMenuType 支持传递 BlockPos 参数） */
	public static final MenuType<AutoHarvesterScreenHandler> AUTO_HARVESTER =
			new ExtendedMenuType<>(
					(syncId, playerInv, pos) -> new AutoHarvesterScreenHandler(syncId, playerInv, pos),
					BLOCK_POS_STREAM_CODEC
			);

	/** 支持的作物方块 ID 列表 */
	public static final String[] CROP_IDS = {
			"minecraft:wheat",
			"minecraft:carrots",
			"minecraft:potatoes",
			"minecraft:beetroots",
			"minecraft:nether_wart",
			"minecraft:torchflower",
			"minecraft:pitcher_crop",
			"minecraft:melon",
			"minecraft:pumpkin",
			"minecraft:sweet_berry_bush"
	};

	/** 作物显示名称（中文） */
	public static final String[] CROP_NAMES = {
			"小麦", "胡萝卜", "马铃薯", "甜菜根", "下界疣", "火把花", "瓶子草植株",
			"西瓜", "南瓜", "甜浆果"
	};

	/** 作物对应的物品（用于 GUI 图标显示） */
	public static final net.minecraft.world.item.Item[] CROP_ITEMS = {
			Items.WHEAT,
			Items.CARROT,
			Items.POTATO,
			Items.BEETROOT,
			Items.NETHER_WART,
			Items.TORCHFLOWER,
			Items.PITCHER_POD,
			Items.MELON,
			Items.PUMPKIN,
			Items.SWEET_BERRIES
	};

	/** 注册菜单类型 */
	public static void initialize() {
		Registry.register(BuiltInRegistries.MENU, AutoHarvester.id("auto_harvester"), AUTO_HARVESTER);
	}

	// ===== 附加开关（GUI 右上角） =====

	/** 附加开关索引：所有能检测到的容器都装不下时暂停收割 */
	public static final int SETTING_STOP_WHEN_FULL = 0;

	/** 附加开关索引：静音（关闭收获提示音） */
	public static final int SETTING_MUTE_SOUND = 1;

	/** 附加开关数量 */
	public static final int SETTING_COUNT = 2;

	/** 附加开关显示名称（中文，GUI 按钮用） */
	public static final String[] SETTING_NAMES = {
			"满箱停收", "静音"
	};

	/** 附加开关默认值：满箱停收默认开启，静音默认关闭（即默认有提示音） */
	public static final boolean[] SETTING_DEFAULTS = {
			true, false
	};

	/**
	 * 根据方块 ID 获取作物索引。
	 * @return 索引值，-1 表示不支持的作物
	 */
	public static int getCropIndex(String blockId) {
		for (int i = 0; i < CROP_IDS.length; i++) {
			if (CROP_IDS[i].equals(blockId)) return i;
		}
		return -1;
	}
}
