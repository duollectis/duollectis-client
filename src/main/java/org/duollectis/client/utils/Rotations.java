//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.duollectis.client.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

public class Rotations {

    public static double getYaw(BlockPos pos) {
        return MinecraftClient.getInstance().player.getYaw() + MathHelper.wrapDegrees((float)Math.toDegrees(Math.atan2((double)pos.getZ() + (double)0.5F - MinecraftClient.getInstance().player.getZ(), (double)pos.getX() + (double)0.5F - MinecraftClient.getInstance().player.getX())) - 90.0F - MinecraftClient.getInstance().player.getYaw());
    }

    public static double getPitch(BlockPos pos) {
        double diffX = (double)pos.getX() + (double)0.5F - MinecraftClient.getInstance().player.getX();
        double diffY = (double)pos.getY() + (double)0.5F - (MinecraftClient.getInstance().player.getY() + (double)MinecraftClient.getInstance().player.getEyeHeight(MinecraftClient.getInstance().player.getPose()));
        double diffZ = (double)pos.getZ() + (double)0.5F - MinecraftClient.getInstance().player.getZ();
        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
        return MinecraftClient.getInstance().player.getPitch() + MathHelper.wrapDegrees((float)(-Math.toDegrees(Math.atan2(diffY, diffXZ))) - MinecraftClient.getInstance().player.getPitch());
    }
}
