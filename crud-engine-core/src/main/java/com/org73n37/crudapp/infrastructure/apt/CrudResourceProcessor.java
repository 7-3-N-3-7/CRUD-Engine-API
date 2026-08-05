package com.org73n37.crudapp.infrastructure.apt;

import com.org73n37.crudapp.infrastructure.annotations.CrudResource;
import com.org73n37.crudapp.infrastructure.annotations.EntityMapping;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.Set;

/**
 * [DEVELOPER EXPERIENCE (DX) OPTIMIZATION]
 * Compile-time Annotation Processor for `@CrudResource` and `@EntityMapping`.
 * Asserts mapping alignment and validates DTO field compatibility 
 * with JPA Entities at compile-time to prevent runtime failures.
 */
@SupportedAnnotationTypes({
    "com.org73n37.crudapp.infrastructure.annotations.CrudResource",
    "com.org73n37.crudapp.infrastructure.annotations.EntityMapping"
})
public class CrudResourceProcessor extends AbstractProcessor {

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(CrudResource.class)) {
            if (element.getKind() == ElementKind.CLASS) {
                validateResource((TypeElement) element);
            }
        }
        return true;
    }

    private void validateResource(TypeElement entityElement) {
        CrudResource crudResource = entityElement.getAnnotation(CrudResource.class);
        if (crudResource == null) return;

        TypeElement dtoElement = getDtoElement(crudResource);
        if (dtoElement == null) return;

        EntityMapping entityMapping = dtoElement.getAnnotation(EntityMapping.class);
        if (entityMapping == null) {
            processingEnv.getMessager().printMessage(
                Diagnostic.Kind.ERROR,
                "DTO Record " + dtoElement.getSimpleName() + " is missing @EntityMapping annotation!",
                dtoElement
            );
            return;
        }

        TypeElement mappedEntityElement = getMappedEntityElement(entityMapping);
        if (mappedEntityElement == null) return;

        if (!mappedEntityElement.getQualifiedName().equals(entityElement.getQualifiedName())) {
            processingEnv.getMessager().printMessage(
                Diagnostic.Kind.ERROR,
                "Bidirectional mapping mismatch: DTO Record " + dtoElement.getSimpleName() + 
                " is mapped to " + mappedEntityElement.getSimpleName() + 
                " but registered on " + entityElement.getSimpleName(),
                entityElement
            );
            return;
        }

        // Validate DTO field compatibility with Entity properties
        for (Element member : dtoElement.getEnclosedElements()) {
            if (member.getKind() == ElementKind.FIELD) {
                String fieldName = member.getSimpleName().toString();
                
                // Ignore specific DTO metadata fields
                if (fieldName.equals("parentId") || fieldName.equals("grandparentId") || fieldName.equals("attributes")) {
                    continue;
                }

                if (!hasFieldInEntity(entityElement, fieldName)) {
                    processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.WARNING,
                        "DTO Field '" + fieldName + "' in " + dtoElement.getSimpleName() + 
                        " has no matching property in Entity class " + entityElement.getSimpleName(),
                        member
                    );
                }
            }
        }
    }

    private boolean hasFieldInEntity(TypeElement entityElement, String fieldName) {
        TypeElement current = entityElement;
        while (current != null && !current.getQualifiedName().toString().equals("java.lang.Object")) {
            for (Element enclosed : current.getEnclosedElements()) {
                if (enclosed.getKind() == ElementKind.FIELD && enclosed.getSimpleName().toString().equals(fieldName)) {
                    return true;
                }
            }
            TypeMirror superclass = current.getSuperclass();
            if (superclass instanceof DeclaredType) {
                current = (TypeElement) ((DeclaredType) superclass).asElement();
            } else {
                current = null;
            }
        }
        return false;
    }

    private TypeElement getDtoElement(CrudResource annotation) {
        try {
            annotation.dto(); // Throws MirroredTypeException during compilation
        } catch (MirroredTypeException mte) {
            DeclaredType classType = (DeclaredType) mte.getTypeMirror();
            return (TypeElement) classType.asElement();
        }
        return null;
    }

    private TypeElement getMappedEntityElement(EntityMapping annotation) {
        try {
            annotation.entity(); // Throws MirroredTypeException during compilation
        } catch (MirroredTypeException mte) {
            DeclaredType classType = (DeclaredType) mte.getTypeMirror();
            return (TypeElement) classType.asElement();
        }
        return null;
    }
}
