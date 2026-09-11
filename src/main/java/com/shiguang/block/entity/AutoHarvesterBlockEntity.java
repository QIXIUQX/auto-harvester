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

/**
 * 自动收割机方块实体。
 * <p>
 * 核心逻辑：
 * - 每 20 tick（1 秒）扫描前方 9×9 区域（以方块朝向为前方）
 * - 检测成熟作物（CropBlock 最大年龄 / NetherWartBlock 年龄 3）
 * - 获取掉落物 → 补种为初始状态 → 存入后方箱子（或散落）
 * - 支持 7 种作物的独立开关（通过 GUI 切换）
 * - NBT 持久化：收割计数 + 各作物开关状态
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
			level.playSound(null, blockPos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.5F, 1.0F);
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
			// 通用 age 属性检查（兜底：处理非 CropBlock 子类的作物如瓶子草植株）
			for (Property<?> prop : state.getProperties()) {
				if (prop.getName().equals("age") && prop instanceof IntegerProperty intProp) {
					if (state.getValue(intProp) < intProp.getPossibleValues().stream().mapToInt(v -> (int) v).max().orElse(0)) {
						return false;
					}
					break;
				}
			}
		}

		if (!(level instanceof ServerLevel serverLevel)) return false;

		// 获取掉落物
		java.util.List<ItemStack> drops = Block.getDrops(state, serverLevel, pos, null);

		// 火把花(index=5)和瓶子草植株(index=6)：只能破坏，不补种
		boolean isBreakOnly = (cropIndex == 5 || cropIndex == 6);

		if (isBreakOnly) {
			// 破坏：直接移除方块
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		} else {
			// 补种：将作物重置为初始状态
			if (block instanceof CropBlock cropBlock) {
				level.setBlock(pos, cropBlock.getStateForAge(0), Block.UPDATE_ALL);
			} else if (block instanceof NetherWartBlock) {
				level.setBlock(pos, Blocks.NETHER_WART.defaultBlockState().setValue(NetherWartBlock.AGE, 0), Block.UPDATE_ALL);
			}
		}

		// 将掉落物存入后方容器，或散落在地上
		if (!drops.isEmpty()) {
			Direction facing = this.getBlockState().getValue(AutoHarvesterBlock.FACING);
			BlockPos behindPos = this.worldPosition.relative(facing.getOpposite());
			Container container = findContainer(level, behindPos);

			if (container != null) {
				for (ItemStack drop : drops) {
					if (!drop.isEmpty()) insertIntoContainer(container, drop, level, pos);
				}
			} else {
				for (ItemStack drop : drops) {
					if (!drop.isEmpty()) Block.popResource(level, pos, drop);
				}
			}
		}

		return true;
	}

	/**
	 * 将物品插入容器。
	 * 优先合并已有同类物品，其次使用空槽位，剩余散落。
	 */
	private void insertIntoContainer(Container container, ItemStack stack, Level level, BlockPos pos) {
		for (int i = 0; i < container.getContainerSize(); i++) {
			ItemStack existing = container.getItem(i);
			if (existing.isEmpty()) {
				container.setItem(i, stack.copy());
				container.setChanged();
				return;
			} else if (ItemStack.isSameItemSameComponents(existing, stack) && existing.getCount() < existing.getMaxStackSize()) {
				int space = existing.getMaxStackSize() - existing.getCount();
				int toInsert = Math.min(stack.getCount(), space);
				existing.grow(toInsert);
				stack.shrink(toInsert);
				container.setChanged();
				if (stack.isEmpty()) return;
			}
		}
		// 容器已满，散落剩余物品
		if (!stack.isEmpty()) {
			Block.popResource(level, pos, stack);
		}
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
