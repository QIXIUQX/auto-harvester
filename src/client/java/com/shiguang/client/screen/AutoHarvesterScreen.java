package com.shiguang.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.shiguang.screen.AutoHarvesterScreenHandler;
import com.shiguang.screen.ModScreenHandlers;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import com.shiguang.network.CropTogglePayload;

/**
 * 自动收割机客户端 GUI 界面。
 * <p>
 * 功能：
 * - 显示 7 种作物的开关列表（可滚动）
 * - 绿色"开"/ 红色"关" 按钮切换作物启用状态
 * - 切换时发送 CropTogglePayload 数据包同步到服务端
 * - 通过 onDataChanged 回调响应服务端 DataSlot 同步
 */
public class AutoHarvesterScreen extends Screen implements net.minecraft.client.gui.screens.inventory.MenuAccess<AutoHarvesterScreenHandler> {

	private final AutoHarvesterScreenHandler handler;

	/** 列表滚动偏移量 */
	private int scrollOffset = 0;

	/** 每行高度（像素） */
	private static final int ROW_HEIGHT = 24;

	/** 最大可见行数 */
	private static final int MAX_VISIBLE = 6;

	/** 列表布局坐标 */
	private int listX, listY, listWidth;

	public AutoHarvesterScreen(AutoHarvesterScreenHandler handler, Inventory playerInv, Component title) {
		super(title);
		this.handler = handler;
	}

	@Override
	protected void init() {
		listWidth = 180;
		listX = (this.width - listWidth) / 2;
		listY = (this.height - MAX_VISIBLE * ROW_HEIGHT) / 2 + 10;

		// 注册数据变更回调：服务端 DataSlot 同步时刷新按钮
		this.handler.setOnDataChanged(this::rebuildButtons);
		rebuildButtons();
	}

	/**
	 * 重建所有切换按钮。
	 * 根据当前滚动偏移量和作物开关状态生成对应按钮。
	 */
	private void rebuildButtons() {
		this.clearWidgets();

		int visibleCount = Math.min(ModScreenHandlers.CROP_IDS.length, MAX_VISIBLE);

		for (int i = 0; i < visibleCount; i++) {
			int cropIndex = i + scrollOffset;
			if (cropIndex >= ModScreenHandlers.CROP_IDS.length) break;

			final int idx = cropIndex;
			int rowY = listY + i * ROW_HEIGHT;

			boolean enabled = handler.isCropEnabled(idx);
			String toggleText = enabled ? "开" : "关";
			ChatFormatting color = enabled ? ChatFormatting.GREEN : ChatFormatting.RED;

			var btn = net.minecraft.client.gui.components.Button.builder(
					Component.literal(toggleText).withStyle(color),
					button -> {
						// 切换状态：本地更新 + 发送网络包同步服务端
						boolean newState = !handler.isCropEnabled(idx);
						handler.setCropEnabled(idx, newState);
						rebuildButtons();
						ClientPlayNetworking.send(new CropTogglePayload(idx, newState));
					}
			).bounds(listX + listWidth - 40, rowY, 36, 18).build();

			this.addRenderableWidget(btn);
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float deltaTicks) {
		// 绘制背景面板
		guiGraphics.fill(this.width / 2 - 100, this.height / 2 - 90, this.width / 2 + 100, this.height / 2 + 90, 0xCC1A1A2E);
		// 顶部蓝色装饰条
		guiGraphics.fill(this.width / 2 - 100, this.height / 2 - 90, this.width / 2 + 100, this.height / 2 - 88, 0xFF4488FF);
		// 底部蓝色装饰条
		guiGraphics.fill(this.width / 2 - 100, this.height / 2 + 88, this.width / 2 + 100, this.height / 2 + 90, 0xFF4488FF);

		// 标题文字
		guiGraphics.centeredText(this.font, this.title, this.width / 2, this.height / 2 - 82, 0xFFFFFF);

		// 绘制作物列表
		int visibleCount = Math.min(ModScreenHandlers.CROP_IDS.length, MAX_VISIBLE);
		for (int i = 0; i < visibleCount; i++) {
			int cropIndex = i + scrollOffset;
			if (cropIndex >= ModScreenHandlers.CROP_IDS.length) break;

			int rowY = listY + i * ROW_HEIGHT;
			boolean enabled = handler.isCropEnabled(cropIndex);
			int textColor = enabled ? 0xFF55FF55 : 0xFFFF5555;
			String name = ModScreenHandlers.CROP_NAMES[cropIndex];

			// 行背景（半透明白色）
			guiGraphics.fill(listX, rowY, listX + listWidth, rowY + ROW_HEIGHT - 2, 0x40FFFFFF);

			// 作物名称
			guiGraphics.text(this.font, name, listX + 8, rowY + 5, textColor, true);

			// 物品图标
			var itemStack = new net.minecraft.world.item.ItemStack(ModScreenHandlers.CROP_ITEMS[cropIndex]);
			guiGraphics.item(itemStack, listX + listWidth - 60, rowY + 1);
		}

		// 滚动提示
		if (ModScreenHandlers.CROP_IDS.length > MAX_VISIBLE) {
			String scrollText = "滚动查看更多";
			guiGraphics.centeredText(this.font, scrollText, this.width / 2, listY + MAX_VISIBLE * ROW_HEIGHT + 4, 0x888888);
		}

		super.extractRenderState(guiGraphics, mouseX, mouseY, deltaTicks);
	}

	/** 鼠标滚轮事件：上下滚动列表 */
	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (verticalAmount > 0 && scrollOffset > 0) {
			scrollOffset--;
			rebuildButtons();
		} else if (verticalAmount < 0 && scrollOffset + MAX_VISIBLE < ModScreenHandlers.CROP_IDS.length) {
			scrollOffset++;
			rebuildButtons();
		}
		return true;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return true;
	}

	@Override
	public AutoHarvesterScreenHandler getMenu() {
		return this.handler;
	}
}
