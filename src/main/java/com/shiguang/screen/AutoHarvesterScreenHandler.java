package com.shiguang.screen;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;

/**
 * 自动收割机菜单处理器。
 * <p>
 * 职责：
 * - 管理 7 个作物开关的 DataSlot（客户端/服务端自动同步）
 * - 持有方块位置（blockPos）用于定位对应的 BlockEntity
 * - 提供 onDataChanged 回调，GUI 切换时触发界面刷新
 */
public class AutoHarvesterScreenHandler extends AbstractContainerMenu {

	/** 各作物是否启用 */
	private final boolean[] cropEnabled = new boolean[ModScreenHandlers.CROP_IDS.length];

	/** 关联的方块位置 */
	private final BlockPos blockPos;

	/** 数据变更回调（由 Screen 设置，DataSlot.set() 时触发） */
	private Runnable onDataChanged;

	public AutoHarvesterScreenHandler(int syncId, Inventory playerInv, BlockPos blockPos) {
		super(ModScreenHandlers.AUTO_HARVESTER, syncId);
		this.blockPos = blockPos;

		// 为每种作物创建 DataSlot，实现客户端/服务端自动同步
		for (int i = 0; i < ModScreenHandlers.CROP_IDS.length; i++) {
			final int index = i;
			DataSlot slot = new DataSlot() {
				@Override
				public int get() {
					return cropEnabled[index] ? 1 : 0;
				}

				@Override
				public void set(int value) {
					cropEnabled[index] = value != 0;
					if (onDataChanged != null) onDataChanged.run();
				}
			};
			this.addDataSlot(slot);
		}
	}

	/** 设置数据变更回调（Screen 在 init 时调用） */
	public void setOnDataChanged(Runnable callback) {
		this.onDataChanged = callback;
	}

	public BlockPos getBlockPos() {
		return blockPos;
	}

	public boolean isCropEnabled(int index) {
		if (index < 0 || index >= cropEnabled.length) return true;
		return cropEnabled[index];
	}

	public void setCropEnabled(int index, boolean enabled) {
		if (index >= 0 && index < cropEnabled.length) {
			cropEnabled[index] = enabled;
		}
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}
}
