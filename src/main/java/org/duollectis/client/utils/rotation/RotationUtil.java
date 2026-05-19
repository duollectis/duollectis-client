package org.duollectis.client.utils.rotation;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Direction;

public class RotationUtil {

    private static final byte SOUTH_FLAG = 0b0001;
    private static final byte NORTH_FLAG = 0b0010;
    private static final byte EAST_FLAG = 0b0100;
    private static final byte WEST_FLAG = 0b1000;

    private static final float YAW_FULL_CIRCLE = 360.0f;
    private static final float YAW_SECTOR_SIZE = 22.5f;

    public static byte fromDirection(Direction direction) {
        return switch (direction) {
            case EAST  -> EAST_FLAG;
            case WEST  -> WEST_FLAG;
            case NORTH -> NORTH_FLAG;
            case SOUTH -> SOUTH_FLAG;
            case UP, DOWN -> -1;
        };
    }

    public static boolean isSouth(byte flags) {
        return (flags & SOUTH_FLAG) != 0;
    }

    public static boolean isNorth(byte flags) {
        return (flags & NORTH_FLAG) != 0;
    }

    public static boolean isEast(byte flags) {
        return (flags & EAST_FLAG) != 0;
    }

    public static boolean isWest(byte flags) {
        return (flags & WEST_FLAG) != 0;
    }

    public static byte getLookingSides() {
        float yaw = MinecraftClient.getInstance().player.getYaw() % YAW_FULL_CIRCLE;

        if (yaw < 0) {
            yaw += YAW_FULL_CIRCLE;
        }

        if (yaw > 0 && yaw < 1 * YAW_SECTOR_SIZE) {
            return SOUTH_FLAG;
        }

        if (yaw > 1 * YAW_SECTOR_SIZE && yaw < 3 * YAW_SECTOR_SIZE) {
            return (byte) (SOUTH_FLAG | WEST_FLAG);
        }

        if (yaw > 3 * YAW_SECTOR_SIZE && yaw < 5 * YAW_SECTOR_SIZE) {
            return WEST_FLAG;
        }

        if (yaw > 5 * YAW_SECTOR_SIZE && yaw < 7 * YAW_SECTOR_SIZE) {
            return (byte) (WEST_FLAG | NORTH_FLAG);
        }

        if (yaw > 7 * YAW_SECTOR_SIZE && yaw < 9 * YAW_SECTOR_SIZE) {
            return NORTH_FLAG;
        }

        if (yaw > 9 * YAW_SECTOR_SIZE && yaw < 11 * YAW_SECTOR_SIZE) {
            return (byte) (NORTH_FLAG | EAST_FLAG);
        }

        if (yaw > 11 * YAW_SECTOR_SIZE && yaw < 13 * YAW_SECTOR_SIZE) {
            return EAST_FLAG;
        }

        if (yaw > 13 * YAW_SECTOR_SIZE && yaw < 15 * YAW_SECTOR_SIZE) {
            return (byte) (EAST_FLAG | SOUTH_FLAG);
        }

        if (yaw > 15 * YAW_SECTOR_SIZE && yaw < YAW_FULL_CIRCLE) {
            return SOUTH_FLAG;
        }

        return 0;
    }
}
