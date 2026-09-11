package com.shiguang.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import com.shiguang.AutoHarvester;

/**
 * 作物切换网络数据包。
 * <p>
 * 客户端发送此数据包通知服务端切换某作物的启用/禁用状态。
 * 包含两个字段：
 * - cropIndex: 作物索引（对应 ModScreenHandlers.CROP_IDS）
 * - enabled: 是否启用
 */
public record CropTogglePayload(int cropIndex, boolean enabled) implements CustomPacketPayload {

	/** 数据包类型标识 */
	public static final Type<CropTogglePayload> TYPE = new Type<>(AutoHarvester.id("crop_toggle"));

	/** 网络编解码器：写入/读取 cropIndex (byte) + enabled (boolean) */
	public static final StreamCodec<FriendlyByteBuf, CropTogglePayload> CODEC = StreamCodec.of(
			(buf, payload) -> {
				buf.writeByte(payload.cropIndex);
				buf.writeBoolean(payload.enabled);
			},
			buf -> new CropTogglePayload(buf.readByte(), buf.readBoolean())
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
