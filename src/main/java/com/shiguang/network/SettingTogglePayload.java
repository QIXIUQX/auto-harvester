package com.shiguang.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import com.shiguang.AutoHarvester;

/**
 * 附加开关切换网络数据包。
 * <p>
 * 客户端发送此数据包通知服务端切换收割机的附加开关（GUI 右上角两个按钮）。
 * 包含两个字段：
 * - settingIndex: 开关索引（对应 ModScreenHandlers.SETTING_*）
 * - enabled: 是否开启
 */
public record SettingTogglePayload(int settingIndex, boolean enabled) implements CustomPacketPayload {

	/** 数据包类型标识 */
	public static final Type<SettingTogglePayload> TYPE = new Type<>(AutoHarvester.id("setting_toggle"));

	/** 网络编解码器：写入/读取 settingIndex (byte) + enabled (boolean) */
	public static final StreamCodec<FriendlyByteBuf, SettingTogglePayload> CODEC = StreamCodec.of(
			(buf, payload) -> {
				buf.writeByte(payload.settingIndex);
				buf.writeBoolean(payload.enabled);
			},
			buf -> new SettingTogglePayload(buf.readByte(), buf.readBoolean())
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
