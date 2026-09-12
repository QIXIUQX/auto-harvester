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
 * - 定义支持的 10 种作物 ID 和对应物品图标（名称走原版翻译键，见 {@link #cropNameKey(int)}）
 * - 定义 2 个附加开关（满箱停收、静音）的索引、翻译键和默认值
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

	/**
	 * 作物显示名称的翻译键。
	 * <p>
	 * 直接复用原版方块名（{@code block.minecraft.wheat} 等），
	 * 因此会随游戏语言自动切换，无需在模组语言文件里重复维护 10 种作物的名称。
	 *
	 * @param index 作物索引（对应 {@link #CROP_IDS}）
	 * @return 翻译键，例如 {@code block.minecraft.wheat}
	 */
	public static String cropNameKey(int index) {
		return "block." + CROP_IDS[index];
	}

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

	/** 附加开关名称的翻译键（GUI 按钮用，随游戏语言切换） */
	public static final String[] SETTING_NAME_KEYS = {
			"setting.auto-harvester.stop_when_full",
			"setting.auto-harvester.mute"
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
