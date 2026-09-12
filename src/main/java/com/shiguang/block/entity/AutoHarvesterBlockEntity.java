package com.shiguang.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

import com.shiguang.block.ModBlockEntities;
import com.shiguang.block.custom.AutoHarvesterBlock;
import com.shiguang.screen.AutoHarvesterScreenHandler;
import com.shiguang.screen.ModScreenHandlers;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.registries.BuiltInRegistries;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 自动收割机方块实体。
 * <p>
 * 核心逻辑：
 * - 每 20 tick（1 秒）扫描前方 9×9 区域（以方块朝向为前方）
 * - 成熟判定：CropBlock 取最大年龄、NetherWartBlock 取年龄 3，
 *   其余带 age 属性的作物按各自最大年龄判定（甜浆果丛 age ≥ 2 即可采摘）
 * - 获取掉落物 → 补种为初始状态（火把花、瓶子草植株、西瓜、南瓜只破坏不补种；甜浆果丛采摘后重置为 age=1）
 *   → 依次存入后方、左侧、右侧的容器（装满后自动转下一个；三个方向都没有容器或全部装满时散落在地上）
 * - 支持 10 种作物的独立开关（通过 GUI 切换，数量由 ModScreenHandlers.CROP_IDS 决定）
 * - 支持 2 个附加开关（通过 GUI 右上角切换）：
 *   满箱停收（默认开）——所有能检测到的容器都装不下本次收获时暂停收割，作物保持成熟；
 *   静音（默认关）——开启后不再播放收获提示音
 * - NBT 持久化：收割计数 + 各作物开关状态 + 各附加开关状态
 */
public class AutoHarvesterBlockEntity extends BlockEntity implements ExtendedMenuProvider<BlockPos> {

	/** 收割范围：前方 4 格（共 9×9 = 81 格） */
	private static final int HARVEST_RANGE = 4;

	/** 执行间隔：每 20 tick 扫描一次 */
	private static final int TICK_INTERVAL = 20;

	/** 累计收割次数 */
	private int harvestCount = 0;

	/** tick 计数器 */
	private int tickCounter = 0;

	/** 各作物是否启用收割（索引对应 ModScreenHandlers.CROP_IDS） */
	private boolean[] cropEnabled = new boolean[ModScreenHandlers.CROP_IDS.length];

	/** 附加开关状态（索引对应 ModScreenHandlers.SETTING_*） */
	private boolean[] settings = ModScreenHandlers.SETTING_DEFAULTS.clone();

	{
		// 默认全部开启
		for (int i = 0; i < cropEnabled.length; i++) {
			cropEnabled[i] = true;
		}
	}

	public AutoHarvesterBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.AUTO_HARVESTER, pos, state);
	}

	public int getHarvestCount() {
		return this.harvestCount;
	}

	public boolean isCropEnabled(int index) {
		if (index < 0 || index >= cropEnabled.length) return true;
		return cropEnabled[index];
	}

	public void setCropEnabled(int index, boolean enabled) {
		if (index >= 0 && index < cropEnabled.length) {
			cropEnabled[index] = enabled;
			this.setChanged();
		}
	}

	public boolean[] getCropEnabledArray() {
		return cropEnabled.clone();
	}

	public void setCropEnabledArray(boolean[] array) {
		this.cropEnabled = array.clone();
		this.setChanged();
	}

	public boolean isSettingEnabled(int index) {
		if (index < 0 || index >= settings.length) return false;
		return settings[index];
	}

	public void setSettingEnabled(int index, boolean enabled) {
		if (index >= 0 && index < settings.length) {
			settings[index] = enabled;
			this.setChanged();
		}
	}

	/** GUI 标题 */
	@Override
	public Component getDisplayName() {
		return Component.literal("自动收割机");
	}

	/**
	 * 创建菜单（GUI）。
	 * 将当前作物开关状态同步到 ScreenHandler。
	 */
	@Override
	public AbstractContainerMenu createMenu(int syncId, Inventory playerInv, Player player) {
		AutoHarvesterScreenHandler handler = new AutoHarvesterScreenHandler(syncId, playerInv, this.worldPosition);
		for (int i = 0; i < cropEnabled.length; i++) {
			handler.setCropEnabled(i, cropEnabled[i]);
		}
		for (int i = 0; i < settings.length; i++) {
			handler.setSettingEnabled(i, settings[i]);
		}
		return handler;
	}

	/** 向客户端发送方块位置数据（ExtendedMenuProvider 要求） */
	@Override
	public BlockPos getScreenOpeningData(ServerPlayer player) {
		return this.worldPosition;
	}

	/**
	 * 静态 tick 方法，由游戏引擎每 tick 调用。
	 * 每 20 tick 执行一次完整的收割扫描。
	 */
	public static void tick(Level level, BlockPos blockPos, BlockState blockState, AutoHarvesterBlockEntity entity) {
		if (level.isClientSide()) return;

		entity.tickCounter++;
		if (entity.tickCounter < TICK_INTERVAL) return;
		entity.tickCounter = 0;

		Direction facing = blockState.getValue(AutoHarvesterBlock.FACING);
		int harvested = 0;

		// 扫描前方 9×9 区域
		for (int x = -HARVEST_RANGE; x <= HARVEST_RANGE; x++) {
			for (int z = 0; z <= HARVEST_RANGE * 2; z++) {
				BlockPos targetPos = entity.getHarvestPos(facing, x, z);
				harvested += entity.tryHarvest(level, targetPos) ? 1 : 0;
			}
		}

		if (harvested > 0) {
			entity.harvestCount += harvested;
			// 静音开关开启时不播放提示音
			if (!entity.isSettingEnabled(ModScreenHandlers.SETTING_MUTE_SOUND)) {
				level.playSound(null, blockPos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.5F, 1.0F);
			}
			entity.setChanged();
		}
	}

	/**
	 * 根据朝向将本地坐标转换为世界坐标。
	 * 前方（z=0）是紧挨方块的那一行，z 越大越远。
	 */
	private BlockPos getHarvestPos(Direction facing, int localX, int localZ) {
		BlockPos base = this.worldPosition.relative(facing);
		return switch (facing) {
			case NORTH -> base.offset(localX, 0, -localZ);
			case SOUTH -> base.offset(-localX, 0, localZ);
			case WEST -> base.offset(-localZ, 0, localX);
			case EAST -> base.offset(localZ, 0, localX);
			default -> base;
		};
	}

	/**
	 * 尝试收割指定位置的作物。
	 * @return 是否成功收割
	 */
	private boolean tryHarvest(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		Block block = state.getBlock();

		// 检查是否是支持的作物类型
		String blockId = BuiltInRegistries.BLOCK.getKey(block).toString();
		int cropIndex = ModScreenHandlers.getCropIndex(blockId);

		// 不是支持的作物
		if (cropIndex < 0) return false;

		// 该作物未启用
		if (!isCropEnabled(cropIndex)) return false;

		// 检查是否成熟
		if (block instanceof CropBlock cropBlock) {
			if (!cropBlock.isMaxAge(state)) return false;
		} else if (block instanceof NetherWartBlock) {
			int age = state.getValue(NetherWartBlock.AGE);
			if (age < 3) return false;
		} else {
			// 通用 age 属性检查（兜底：处理非 CropBlock 子类的作物如瓶子草植株、甜浆果丛）
			for (Property<?> prop : state.getProperties()) {
				if (prop.getName().equals("age") && prop instanceof IntegerProperty intProp) {
					int age = state.getValue(intProp);
					// 甜浆果丛：age >= 2 即可采摘（index=9）
					if (cropIndex == 9) {
						if (age < 2) return false;
					} else {
						if (age < intProp.getPossibleValues().stream().mapToInt(v -> (int) v).max().orElse(0)) {
							return false;
						}
					}
					break;
				}
			}
		}

		if (!(level instanceof ServerLevel serverLevel)) return false;

		// 获取掉落物
		List<ItemStack> drops = Block.getDrops(state, serverLevel, pos, null);

		// 先找出后方/左侧/右侧的容器，再决定是否收割
		Direction facing = this.getBlockState().getValue(AutoHarvesterBlock.FACING);
		List<Container> containers = findContainers(level, facing);

		// 满箱停收：存在容器、但所有容器都装不下本次收获中的任何一件物品时，
		// 跳过本次收割（作物保持成熟状态，等腾出空间后下次扫描再收）
		if (isSettingEnabled(ModScreenHandlers.SETTING_STOP_WHEN_FULL)
				&& !containers.isEmpty()
				&& !drops.isEmpty()
				&& !canAcceptAny(containers, drops)) {
			return false;
		}

		// 火把花(index=5)、瓶子草植株(index=6)、西瓜(index=7)、南瓜(index=8)：只能破坏，不补种
		boolean isBreakOnly = (cropIndex == 5 || cropIndex == 6 || cropIndex == 7 || cropIndex == 8);

		if (isBreakOnly) {
			// 破坏：直接移除方块
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		} else if (cropIndex == 9 && block instanceof SweetBerryBushBlock) {
			// 甜浆果丛：采摘后重置为 age=1（保留植株，可继续生长）
			level.setBlock(pos, state.setValue(SweetBerryBushBlock.AGE, 1), Block.UPDATE_ALL);
		} else {
			// 补种：将作物重置为初始状态
			if (block instanceof CropBlock cropBlock) {
				level.setBlock(pos, cropBlock.getStateForAge(0), Block.UPDATE_ALL);
			} else if (block instanceof NetherWartBlock) {
				level.setBlock(pos, Blocks.NETHER_WART.defaultBlockState().setValue(NetherWartBlock.AGE, 0), Block.UPDATE_ALL);
			}
		}

		// 将掉落物存入后方/左侧/右侧的容器；三个方向都没有容器或全部装满时散落在地上
		if (!drops.isEmpty()) {
			for (ItemStack drop : drops) {
				if (drop.isEmpty()) continue;

				// 依次尝试每个容器，前一个装不下时自动转下一个
				for (Container container : containers) {
					insertIntoContainer(container, drop);
					if (drop.isEmpty()) break;
				}

				// 所有容器都放不下，散落在地上
				if (!drop.isEmpty()) {
					Block.popResource(level, pos, drop);
				}
			}
		}

		return true;
	}

	/**
	 * 查找方块后方、左侧、右侧三个方向的容器。
	 * <p>
	 * 三个方向互相独立：只放一个、放两个或三个都放都会被识别，
	 * 返回顺序即写入优先级：后方 → 左侧 → 右侧。
	 * 左/右以方块自身朝向为基准（后方为 facing 反方向，左右为与其垂直的两个水平方向）。
	 */
	private List<Container> findContainers(Level level, Direction facing) {
		List<Container> containers = new ArrayList<>(3);

		for (Direction side : new Direction[] {
				facing.getOpposite(), facing.getCounterClockWise(), facing.getClockWise()
		}) {
			Container container = findContainer(level, this.worldPosition.relative(side));
			if (container != null) containers.add(container);
		}

		return containers;
	}

	/**
	 * 将物品尽量塞入指定容器：先堆叠到已有的同类物品上，再占用空槽位。
	 * 装不下的部分保留在 stack 中，由调用方交给下一个容器或散落在地上。
	 */
	private void insertIntoContainer(Container container, ItemStack stack) {
		// 1. 优先合并到已有的同类物品上
		for (int i = 0; i < container.getContainerSize() && !stack.isEmpty(); i++) {
			ItemStack existing = container.getItem(i);
			if (existing.isEmpty() || !ItemStack.isSameItemSameComponents(existing, stack)) continue;

			int space = existing.getMaxStackSize() - existing.getCount();
			if (space <= 0) continue;

			int toInsert = Math.min(stack.getCount(), space);
			existing.grow(toInsert);
			stack.shrink(toInsert);
			container.setChanged();
		}

		// 2. 再使用空槽位
		for (int i = 0; i < container.getContainerSize() && !stack.isEmpty(); i++) {
			if (container.getItem(i).isEmpty()) {
				container.setItem(i, stack.copy());
				stack.setCount(0);
				container.setChanged();
			}
		}
	}

	/**
	 * 判断这组容器是否还能装下本次收获中的任意一件物品。
	 * <p>
	 * 判定口径与 {@link #insertIntoContainer} 完全一致（同类物品可堆叠，或有空槽位），
	 * 因此「返回 false」等价于「真的一个都放不进去」。
	 */
	private static boolean canAcceptAny(List<Container> containers, List<ItemStack> drops) {
		for (Container container : containers) {
			for (ItemStack drop : drops) {
				if (!drop.isEmpty() && canAccept(container, drop)) return true;
			}
		}
		return false;
	}

	/** 单个容器能否再放下该物品 */
	private static boolean canAccept(Container container, ItemStack stack) {
		for (int i = 0; i < container.getContainerSize(); i++) {
			ItemStack existing = container.getItem(i);
			if (existing.isEmpty()) return true;
			if (ItemStack.isSameItemSameComponents(existing, stack)
					&& existing.getCount() < existing.getMaxStackSize()) {
				return true;
			}
		}
		return false;
	}

	/** 查找指定位置的容器，大箱子会返回完整的54格容器 */
	@Nullable
	private Container findContainer(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		Block block = state.getBlock();
		if (block instanceof ChestBlock chestBlock) {
			return ChestBlock.getContainer(chestBlock, state, level, pos, true);
		}
		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (blockEntity instanceof Container container) return container;
		return null;
	}

	// ===== NBT 持久化 =====

	@Override
	protected void saveAdditional(ValueOutput output) {
		output.putInt("harvest_count", this.harvestCount);
		for (int i = 0; i < cropEnabled.length; i++) {
			output.putBoolean("crop_" + i, cropEnabled[i]);
		}
		for (int i = 0; i < settings.length; i++) {
			output.putBoolean("setting_" + i, settings[i]);
		}
		super.saveAdditional(output);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		this.harvestCount = input.getIntOr("harvest_count", 0);
		boolean[] defaults = createDefaultEnabled();
		for (int i = 0; i < defaults.length; i++) {
			cropEnabled[i] = input.getBooleanOr("crop_" + i, defaults[i]);
		}
		boolean[] settingDefaults = ModScreenHandlers.SETTING_DEFAULTS;
		for (int i = 0; i < settings.length; i++) {
			settings[i] = input.getBooleanOr("setting_" + i, settingDefaults[i]);
		}
	}

	/** 创建默认全部开启的数组 */
	private static boolean[] createDefaultEnabled() {
		boolean[] defaults = new boolean[ModScreenHandlers.CROP_IDS.length];
		for (int i = 0; i < defaults.length; i++) defaults[i] = true;
		return defaults;
	}

	// ===== 网络同步 =====

	@Override
	public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registryLookup) {
		return saveWithoutMetadata(registryLookup);
	}

	@Nullable
	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	/** 数据变更时通知客户端同步 */
	@Override
	public void setChanged() {
		super.setChanged();
		if (this.level != null) {
			BlockState state = getBlockState();
			this.level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_ALL);
		}
	}
}
