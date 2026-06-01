package org.duollectis.client.api.event.events.input;

import org.lwjgl.glfw.GLFW;

import java.util.EnumSet;
import java.util.Set;

/** Модификаторы клавиатуры (Shift, Ctrl, Alt, Super, CapsLock, NumLock). */
public enum InputModifier {

	SHIFT(GLFW.GLFW_MOD_SHIFT),
	CONTROL(GLFW.GLFW_MOD_CONTROL),
	ALT(GLFW.GLFW_MOD_ALT),
	SUPER(GLFW.GLFW_MOD_SUPER),
	CAPS_LOCK(GLFW.GLFW_MOD_CAPS_LOCK),
	NUM_LOCK(GLFW.GLFW_MOD_NUM_LOCK);

	private final int glfwBit;

	InputModifier(int glfwBit) {
		this.glfwBit = glfwBit;
	}

	/** Разбирает битовую маску GLFW в набор активных модификаторов. */
	public static Set<InputModifier> of(int glfwMods) {
		Set<InputModifier> result = EnumSet.noneOf(InputModifier.class);

		for (InputModifier mod : values()) {
			if ((glfwMods & mod.glfwBit) != 0) {
				result.add(mod);
			}
		}

		return result;
	}

	public boolean isActive(int glfwMods) {
		return (glfwMods & glfwBit) != 0;
	}
}
