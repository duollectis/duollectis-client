package org.duollectis.client.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Direction;

public class RotationUtil {

    private static final byte SOUTH_FLAG = 0b0001;
    private static final byte NORTH_FLAG = 0b0010;
    private static final byte EAST_FLAG = 0b0100;
    private static final byte WEST_FLAG = 0b1000;

    public static byte fromDirection(Direction direction) {
        return switch (direction) {
            case EAST -> EAST_FLAG;
            case WEST -> WEST_FLAG;
            case NORTH -> NORTH_FLAG;
            case SOUTH -> SOUTH_FLAG;
            case UP, DOWN -> -1;
        };
    }

    public static boolean isSouth(byte flags) {return (flags & SOUTH_FLAG) != 0;}

    public static boolean isNorth(byte flags) {return (flags & NORTH_FLAG) != 0;}

    public static boolean isEast(byte flags) {return (flags & EAST_FLAG) != 0;}

    public static boolean isWest(byte flags) {return (flags & WEST_FLAG) != 0;}

    public static byte getLookingSides() {
        assert MinecraftClient.getInstance().player != null;

        float yaw = MinecraftClient.getInstance().player.getYaw() % 360;

        if (yaw < 0)
            yaw = 360 + yaw;

        if (yaw > 0 && yaw < 22.5)
            return SOUTH_FLAG;
        else if (yaw > 22.5 && yaw < 67.5)
            return SOUTH_FLAG | WEST_FLAG;
        else if (yaw > 67.5 && yaw < 112.5)
            return WEST_FLAG;
        else if (yaw > 112.5 && yaw < 157.5)
            return WEST_FLAG | NORTH_FLAG;
        else if (yaw > 157.5 && yaw < 202.5)
            return NORTH_FLAG;
        else if (yaw > 202.5 && yaw < 247.5)
            return NORTH_FLAG | EAST_FLAG;
        else if (yaw > 247.5 && yaw < 292.5)
            return EAST_FLAG;
        else if (yaw > 292.5 && yaw < 337.5)
            return EAST_FLAG | SOUTH_FLAG;
        else if (yaw > 337.5 && yaw < 360)
            return SOUTH_FLAG;

        return 0;
    }
}
