package org.duollectis.client.utils.rotation;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

public class Rotations {

    private static final double BLOCK_CENTER_OFFSET = 0.5;

    public static double getYaw(BlockPos pos) {
        double player = MinecraftClient.getInstance().player.getYaw();
        double dx = pos.getX() + BLOCK_CENTER_OFFSET - MinecraftClient.getInstance().player.getX();
        double dz = pos.getZ() + BLOCK_CENTER_OFFSET - MinecraftClient.getInstance().player.getZ();
        double targetYaw = Math.toDegrees(Math.atan2(dz, dx)) - 90.0;
        return player + MathHelper.wrapDegrees((float) targetYaw - (float) player);
    }

    public static double getPitch(BlockPos pos) {
        double dx = pos.getX() + BLOCK_CENTER_OFFSET - MinecraftClient.getInstance().player.getX();
        double dy = pos.getY() + BLOCK_CENTER_OFFSET
                - (MinecraftClient.getInstance().player.getY()
                + MinecraftClient.getInstance().player.getEyeHeight(
                        MinecraftClient.getInstance().player.getPose()));
        double dz = pos.getZ() + BLOCK_CENTER_OFFSET - MinecraftClient.getInstance().player.getZ();
        double dxz = Math.sqrt(dx * dx + dz * dz);
        double player = MinecraftClient.getInstance().player.getPitch();
        double targetPitch = -Math.toDegrees(Math.atan2(dy, dxz));
        return player + MathHelper.wrapDegrees((float) targetPitch - (float) player);
    }
}
