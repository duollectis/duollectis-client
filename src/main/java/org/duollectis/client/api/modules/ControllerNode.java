package org.duollectis.client.api.modules;

import org.duollectis.client.api.event.ListenerEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Базовый узел дерева контроллеров.
 * <p>
 * Содержит единственную реализацию логики регистрации дочерних контроллеров,
 * рекурсивного сбора листенеров и каскадного вызова жизненного цикла.
 * Наследуется как {@link Module} (корень дерева), так и {@link Controller} (узел дерева).
 */
public abstract class ControllerNode {

	private final List<Controller<?>> children = new ArrayList<>();

	/**
	 * Регистрирует дочерний контроллер: пробрасывает ему ссылку на модуль,
	 * вызывает его {@code onInit()} (чтобы он мог зарегистрировать своих детей),
	 * и добавляет в список дочерних узлов.
	 * Возвращает контроллер для удобного инлайн-присваивания в поле.
	 */
	<M extends Module> void addController(M module, Controller<M> controller) {
		controller.module = module;
		controller.onInit();
		children.add(controller);
	}

	/**
	 * Собирает листенеры самого узла и всех его потомков в плоский список.
	 * Используется {@link Module} при подписке/отписке от {@code EventBus}.
	 */
	List<ListenerEntry<?>> collectAllListeners() {
		List<ListenerEntry<?>> all = new ArrayList<>(ListenerEntry.scanSubscribeMethods(this));

		for (Controller<?> child : children) {
			all.addAll(child.collectAllListeners());
		}

		return all;
	}

	public void enable() {
		onActivate();
		children.forEach(ControllerNode::enable);
	}

	public void disable() {
		children.forEach(ControllerNode::disable);
		onDeactivate();
	}

	protected void onActivate() {}

	protected void onDeactivate() {}
}
