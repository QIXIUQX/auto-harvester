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
import com.shiguang.network.SettingTogglePayload;

/**
 * 自动收割机客户端 GUI 界面。
 * <p>
 * 功能：
 * - 显示所有支持作物的开关列表（可滚动，数量随 ModScreenHandlers.CROP_IDS 变化）
 * - 绿色"开"/ 红色"关" 按钮切换作物启用状态
 * - 右上角两个附加开关：满箱停收（默认开）、静音（默认关）
 * - 切换时发送 CropTogglePayload / SettingTogglePayload 数据包同步到服务端
 * - 通过 onDataChanged 回调响应服务端 DataSlot 同步
 * - 按容器界面处理：不暂停游戏（isPauseScreen = false），关闭时通知服务端关闭菜单
 */
public class AutoHarvesterScreen extends Screen implements net.minecraft.client.gui.screens.inventory.MenuAccess<AutoHarvesterScreenHandler> {

	private final AutoHarvesterScreenHandler handler;

	/** 列表滚动偏移量 */
	private int scrollOffset = 0;

	/** 每行高度（像素） */
	private static final int ROW_HEIGHT = 24;

	/** 最大可见行数 */
	private static final int MAX_VISIBLE = 6;

	/** 面板半宽 / 半高（面板以屏幕中心为基准） */
	private static final int PANEL_HALF_WIDTH = 100;
	private static final int PANEL_HALF_HEIGHT = 90;

	/** 附加开关按钮高度、间距、左右内边距 */
	private static final int SETTING_BUTTON_HEIGHT = 16;
	private static final int SETTING_BUTTON_GAP = 4;
	private static final int SETTING_BUTTON_PADDING = 8;

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
	 * 根据当前滚动偏移量、作物开关状态和附加开关状态生成对应按钮。
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

		rebuildSettingButtons();
	}

	/**
	 * 重建右上角的两个附加开关按钮（满箱停收、静音）。
	 * 按钮从面板右侧向左依次排列，宽度按文字实际宽度计算。
	 */
	private void rebuildSettingButtons() {
		int buttonY = this.height / 2 - PANEL_HALF_HEIGHT + 6;

		// 先算总宽度，以便整体右对齐
		Component[] labels = new Component[ModScreenHandlers.SETTING_COUNT];
		int[] widths = new int[ModScreenHandlers.SETTING_COUNT];
		int totalWidth = SETTING_BUTTON_GAP * (ModScreenHandlers.SETTING_COUNT - 1);

		for (int i = 0; i < ModScreenHandlers.SETTING_COUNT; i++) {
			boolean on = handler.isSettingEnabled(i);
			labels[i] = Component.literal(ModScreenHandlers.SETTING_NAMES[i] + (on ? ":开" : ":关"))
					.withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED);
			widths[i] = this.font.width(labels[i]) + SETTING_BUTTON_PADDING;
			totalWidth += widths[i];
		}

		int x = this.width / 2 + PANEL_HALF_WIDTH - SETTING_BUTTON_PADDING / 2 - totalWidth;

		for (int i = 0; i < ModScreenHandlers.SETTING_COUNT; i++) {
			final int settingIndex = i;

			var btn = net.minecraft.client.gui.components.Button.builder(
					labels[i],
					button -> {
						boolean newState = !handler.isSettingEnabled(settingIndex);
						handler.setSettingEnabled(settingIndex, newState);
						rebuildButtons();
						ClientPlayNetworking.send(new SettingTogglePayload(settingIndex, newState));
					}
			).bounds(x, buttonY, widths[i], SETTING_BUTTON_HEIGHT).build();

			this.addRenderableWidget(btn);
			x += widths[i] + SETTING_BUTTON_GAP;
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float deltaTicks) {
		int panelLeft = this.width / 2 - PANEL_HALF_WIDTH;
		int panelTop = this.height / 2 - PANEL_HALF_HEIGHT;
		int panelRight = this.width / 2 + PANEL_HALF_WIDTH;
		int panelBottom = this.height / 2 + PANEL_HALF_HEIGHT;

		// 绘制背景面板
		guiGraphics.fill(panelLeft, panelTop, panelRight, panelBottom, 0xCC1A1A2E);
		// 顶部蓝色装饰条
		guiGraphics.fill(panelLeft, panelTop, panelRight, panelTop + 2, 0xFF4488FF);
		// 底部蓝色装饰条
		guiGraphics.fill(panelLeft, panelBottom - 2, panelRight, panelBottom, 0xFF4488FF);

		// 标题文字（左对齐，右上角留给两个附加开关按钮）
		guiGraphics.text(this.font, this.title, panelLeft + 8, panelTop + 10, 0xFFFFFF, true);

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

	/**
	 * 与 {@code AbstractContainerScreen} 保持一致：容器类界面不暂停游戏。
	 * <p>
	 * {@code Screen} 默认返回 {@code true}，单人模式下会经
	 * {@code Gui.isPausing()} → {@code Minecraft.pauseGame()} 暂停整合服务器，
	 * 于是箱子开合动画、生物、以及本方块自身的 tick 全部冻结。
	 * 原版箱子等容器界面都重写此方法返回 {@code false}，这里同理。
	 */
	@Override
	public boolean isPauseScreen() {
		return false;
	}

	/**
	 * 按 ESC 关闭界面时，先通知服务端关闭容器菜单，再关闭界面。
	 * <p>
	 * {@code LocalPlayer.closeContainer()} 内部会调用
	 * {@code clientSideCloseContainer()} → {@code Gui.setScreen(null)}，
	 * 界面已经随之关闭，因此这里不能再手动 setScreen。
	 */
	@Override
	public void onClose() {
		if (this.minecraft.player != null) {
			this.minecraft.player.closeContainer();
		}
		super.onClose();
	}

	/**
	 * 界面被移除时的菜单侧清理（对应 {@code AbstractContainerScreen.removed()}）。
	 * <p>
	 * <b>这里绝对不能再调用 {@code closeContainer()}</b>：
	 * {@code Gui.setScreen()} 是在把 screen 字段置空<b>之前</b>调用 {@code removed()}，
	 * 而 {@code closeContainer()} 又会回调 {@code Gui.setScreen(null)}，
	 * 于是形成 removed → closeContainer → setScreen → removed 的无限递归，
	 * 触发 {@code StackOverflowError} 使客户端崩溃（按 ESC 关闭界面时必崩）。
	 * 关闭容器只由 {@link #onClose()} 负责。
	 */
	@Override
	public void removed() {
		if (this.minecraft.player != null) {
			this.handler.removed(this.minecraft.player);
		}
		super.removed();
	}

	@Override
	public AutoHarvesterScreenHandler getMenu() {
		return this.handler;
	}
}
