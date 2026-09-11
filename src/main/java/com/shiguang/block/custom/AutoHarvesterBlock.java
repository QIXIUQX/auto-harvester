package com.shiguang.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

import com.shiguang.block.ModBlockEntities;
import com.shiguang.block.entity.AutoHarvesterBlockEntity;

import com.mojang.serialization.MapCodec;

import org.jetbrains.annotations.Nullable;

/**
 * 自动收割机方块。
 * <p>
 * 功能：
 * - 水平朝向（面朝放置时玩家的对面方向）
 * - 右键打开作物筛选 GUI
 * - 服务端每 20 tick 执行一次收割逻辑
 * - 通过 BaseEntityBlock 绑定 BlockEntity
 */
public class AutoHarvesterBlock extends BaseEntityBlock {

	/** Codec 用于方块序列化（MC 26.2 要求） */
	public static final MapCodec<AutoHarvesterBlock> CODEC = simpleCodec(AutoHarvesterBlock::new);

	/** 水平朝向属性 */
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

	public AutoHarvesterBlock(Properties settings) {
		super(settings);
		this.registerDefaultState(this.getStateDefinition().any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	/** 定义方块状态：仅包含水平朝向 */
	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	/** 放置时朝向玩家的对面 */
	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	public BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	/** 创建方块实体 */
	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new AutoHarvesterBlockEntity(pos, state);
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	/** 右键交互：打开作物筛选 GUI */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof AutoHarvesterBlockEntity entity) {
			player.openMenu(entity);
		}
		return InteractionResult.SUCCESS;
	}

	/** 注册服务端 ticker，每 tick 调用 BlockEntity 的 tick 方法 */
	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level instanceof ServerLevel serverLevel) {
			return createTickerHelper(type, ModBlockEntities.AUTO_HARVESTER, AutoHarvesterBlockEntity::tick);
		}
		return null;
	}
}
