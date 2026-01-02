package me.chrr.tapestry.gradle.ap;

import me.chrr.tapestry.gradle.annotation.FabricEntrypoint;
import me.chrr.tapestry.gradle.annotation.PlatformImplementation;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.util.Set;

@NullMarked
public class TapestryAnnotationProcessor extends AbstractProcessor {
    private @Nullable FileObject entrypoints;
    private @Nullable FileObject platforms;

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(
                FabricEntrypoint.class.getName(),
                PlatformImplementation.class.getName()
        );
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);

        try {
            entrypoints = env.getFiler().createResource(StandardLocation.SOURCE_OUTPUT, "tapestry", "entrypoints.txt");
            platforms = env.getFiler().createResource(StandardLocation.SOURCE_OUTPUT, "tapestry", "platforms.txt");
        } catch (IOException e) {
            throw new IllegalStateException("Could not create output resources", e);
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        if (env.processingOver())
            return true;

        if (entrypoints == null || platforms == null)
            throw new IllegalStateException();

        try (Writer writer = entrypoints.openWriter()) {
            for (Element element : env.getElementsAnnotatedWith(FabricEntrypoint.class)) {
                FabricEntrypoint annotation = element.getAnnotation(FabricEntrypoint.class);
                String receiver = switch (element.getKind()) {
                    case CLASS -> element.toString();
                    case METHOD, FIELD -> element.getEnclosingElement().toString() + "::" + element.getSimpleName();
                    default ->
                            throw new IllegalArgumentException("@FabricEntrypoint can only be used on classes, methods and fields");
                };

                assert annotation != null;
                writer.append(annotation.value()).append(";").append(receiver).append("\n");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write entrypoints", e);
        }

        try (Writer writer = platforms.openWriter()) {
            for (Element element : env.getElementsAnnotatedWith(PlatformImplementation.class)) {
                if (element.getKind() != ElementKind.CLASS)
                    throw new IllegalArgumentException("@PlatformImplementation can only be used on classes");

                PlatformImplementation annotation = element.getAnnotation(PlatformImplementation.class);
                String receiver = element.toString();

                assert annotation != null;
                writer.append(getReceiver(annotation)).append(";").append(receiver).append("\n");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write platform implementations", e);
        }

        return true;
    }

    private static String getReceiver(PlatformImplementation annotation) {
        try {
            return annotation.value().toString();
        } catch (MirroredTypeException exception) {
            return exception.getTypeMirror().toString();
        }
    }
}
