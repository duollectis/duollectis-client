package org.duollectis.client.api.modules;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация-метаданные модуля: задаёт уникальный kebab-case идентификатор и категорию.
 * Обрабатывается {@link ModuleRegistryProcessor} во время компиляции для генерации реестра.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ModuleInfo {

	String value();
	Category category() default Category.NONE;
}
