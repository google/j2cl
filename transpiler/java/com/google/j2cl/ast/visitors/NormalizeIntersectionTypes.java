
package com.google.j2cl.ast.visitors;

import static java.util.stream.Collectors.joining;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.Type;
import com.google.j2cl.ast.Type.Kind;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptor.DescriptorFactory;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.Visibility;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * To represent intersection type in the closure type system we need to synthesize a class that has
 * the correct super and interface types.
 */
public final class NormalizeIntersectionTypes extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    Map<TypeDescriptor, TypeDescriptor> normalizedTypesByIntersectionType = Maps.newLinkedHashMap();
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteTypeDescriptor(final TypeDescriptor typeDescriptor) {
            if (!typeDescriptor.isIntersection()) {
              return typeDescriptor;
            }

            TypeDescriptor normalizedType = normalizedTypesByIntersectionType.get(typeDescriptor);
            if (normalizedType == null) {
              TypeDescriptor enclosingTypeDescriptor = getCurrentType().getDescriptor();
              normalizedType =
                  createIntersectionType(
                      typeDescriptor,
                      enclosingTypeDescriptor,
                      normalizedTypesByIntersectionType.size());
              normalizedTypesByIntersectionType.put(typeDescriptor, normalizedType);
            }

            return normalizedType;
          }
        });

    // Synthesize classes for intersection types.
    for (TypeDescriptor typeDescriptor : normalizedTypesByIntersectionType.values()) {
      Type syntheticType = new Type(Kind.CLASS, Visibility.PACKAGE_PRIVATE, typeDescriptor);
      syntheticType.setAbstract(true);
      compilationUnit.addType(syntheticType);
    }
  }

  private TypeDescriptor createIntersectionType(
      TypeDescriptor typeDescriptor, TypeDescriptor enclosingClassTypeDescriptor, int uniqueId) {

    // Here we synthesize a class name of the form:
    // 00<binaryClassname of first intersected type>
    // The extra 0 removes the possibility of a name conflict with a lambda type with a
    // potentially equal method name.  TODO: Centralize the anonymous class counter.
    String simpleName =
        "0" + uniqueId + typeDescriptor.getIntersectedTypeDescriptors().get(0).getBinaryClassName();
    List<String> classComponents =
        Lists.newArrayList(enclosingClassTypeDescriptor.getClassComponents());
    classComponents.add(simpleName);

    List<String> packageComponents =
        Lists.newArrayList(enclosingClassTypeDescriptor.getPackageComponents());
    String packageName = Joiner.on(".").join(packageComponents);

    return TypeDescriptor.Builder.from(typeDescriptor)
        .setBinaryName(enclosingClassTypeDescriptor.getBinaryName() + "$" + simpleName)
        .setEnclosingTypeDescriptorFactory(
            new DescriptorFactory<TypeDescriptor>() {
              @Override
              public TypeDescriptor create(TypeDescriptor selfTypeDescriptor) {
                return enclosingClassTypeDescriptor;
              }
            })
        .setClassComponents(classComponents)
        .setPackageComponents(packageComponents)
        .setPackageName(packageName)
        .setSimpleName(simpleName)
        .setSourceName(
            Stream.concat(packageComponents.stream(), classComponents.stream())
                .collect(joining(".")))
        // It would be nice to compute this is the JdtUtils.createIntersection however,
        // since factories are copied before the TypeDescriptor is modified and interned
        // with a Builder.from it runs the factory in the context before this
        // properly named descriptor.
        .setRawTypeDescriptorFactory(
            new DescriptorFactory<TypeDescriptor>() {
              @Override
              public TypeDescriptor create(TypeDescriptor selfTypeDescriptor) {
                return TypeDescriptors.replaceTypeArgumentDescriptors(
                    selfTypeDescriptor, Collections.<TypeDescriptor>emptyList());
              }
            })
        .build();
  }
}
