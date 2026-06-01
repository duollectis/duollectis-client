package org.duollectis.client.api.event.events.input;

import org.lwjgl.glfw.GLFW;

/** Действие клавиши или кнопки мыши. */
public enum InputAction {

	PRESS(GLFW.GLFW_PRESS),
	RELEASE(GLFW.GLFW_RELEASE),
	REPEAT(GLFW.GLFW_REPEAT);

	private final int glfwCode;

	InputAction(int glfwCode) {
		this.glfwCode = glfwCode;
	}

	public static InputAction of(int glfwAction) {
		for (InputAction action : values()) {
			if (action.glfwCode == glfwAction) {
				return action;
			}
		}

		return RELEASE;
	}
}
