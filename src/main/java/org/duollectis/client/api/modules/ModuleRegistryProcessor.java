package org.duollectis.client.api.modules;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Annotation Processor времени компиляции: собирает все классы, помеченные {@link ModuleInfo},
 * и записывает их полные имена в ресурс {@code META-INF/duollectis-modules}.
 * {@link ModuleManager} читает этот файл при старте для авто-регистрации модулей.
 */
@SupportedAnnotationTypes("org.duollectis.client.api.modules.ModuleInfo")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class ModuleRegistryProcessor extends AbstractProcessor {

	private static final String REGISTRY_RESOURCE = "META-INF/duollectis-modules";

	private final Set<String> moduleClassNames = new LinkedHashSet<>();

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		for (TypeElement annotation : annotations) {
			for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
				moduleClassNames.add(((TypeElement) element).getQualifiedName().toString());
			}
		}

		if (roundEnv.processingOver() && !moduleClassNames.isEmpty()) {
			writeRegistry();
		}

		return false;
	}

	private void writeRegistry() {
		Filer filer = processingEnv.getFiler();

		try {
			FileObject resource = filer.createResource(
					StandardLocation.CLASS_OUTPUT,
					"",
					REGISTRY_RESOURCE
			);

			try (PrintWriter writer = new PrintWriter(resource.openWriter())) {
				for (String className : moduleClassNames) {
					writer.println(className);
				}
			}
		} catch (IOException e) {
			processingEnv.getMessager().printError(
					"Не удалось записать реестр модулей: " + e.getMessage()
			);
		}
	}

}
