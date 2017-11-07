/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.j2cl.ast.visitors;

import com.google.j2cl.ast.AbstractVisitor;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.DeclaredTypeDescriptor;
import com.google.j2cl.ast.ManglingNameUtils;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.Type;
import com.google.j2cl.ast.TypeDeclaration;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Creates unimplemented methods in abstract class.
 *
 * <p>An abstract class in Java may have unimplemented methods that are declared by its super
 * interfaces. However, in JavaScript, it requires a class tagged with 'implements' to implement all
 * of the methods defined by the interface. This class synthesizes the unimplemented methods in an
 * abstract class.
 *
 * <p>The general idea is to assume that your parent class already has a complete set of methods
 * that fulfill all JS method signatures required by the interfaces it implements.
 *
 * <p>So to figure out what abstract stub methods need to be created here one only needs to figure
 * out what JS method signatures have become newly required on this type beyond what has already
 * been fulfilled in the super type.
 */
public class UnimplementedMethodsCreator extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractVisitor() {
          @Override
          public void exitType(Type type) {
            // Only stub methods on abstract classes.
            if (!type.isAbstract()) {
              return;
            }

            /**
             * Record the complete set of JS method signatures that have already be fulfilled (must
             * have, somehow) on the super type(s).
             *
             * <p>This is done by collecting the JS method signatures of all declared methods on the
             * super type as well as all methods required by implemented interfaces (from both
             * immediate interfaces and their super interfaces), and then repeating that process all
             * the way up the super type chain.
             *
             * <p>Lastly the declared methods of the current type are recorded as well, since they
             * are obviously already fulfilled.
             */
            Set<String> filledSignatures = new HashSet<>();
            if (type.getDeclaration().getSuperTypeDescriptor() != null) {
              TypeDeclaration superTypeDeclaration = type.getDeclaration();
              do {
                superTypeDeclaration =
                    superTypeDeclaration.getSuperTypeDescriptor().getTypeDeclaration();

                filledSignatures.addAll(
                    getJsSignaturesInImmediateInterfacesTransitive(superTypeDeclaration));
                filledSignatures.addAll(getDeclaredMethodSignatures(superTypeDeclaration));
              } while (superTypeDeclaration.getSuperTypeDescriptor() != null);
            }
            filledSignatures.addAll(getDeclaredMethodSignatures(type.getDeclaration()));
            /**
             * Bridge method passes may or may not have seen fit to fill yet-more signatures on the
             * current type (yes even if the current type is abstract, if it contains concrete
             * methods there may be some bridges created there). Rather than duplicate their same
             * logic here, collect the filled signatures from the methods in the Type right now (not
             * the TypeDescriptor).
             */
            for (Method method : type.getMethods()) {
              filledSignatures.add(ManglingNameUtils.getMangledName(method.getDescriptor()));
            }

            /**
             * Figure out what JS method signatures have become newly required on this type beyond
             * what has already been fulfilled in the super type. And create an abstract stub for
             * each one.
             *
             * <p>This is done by examining the JS method signatures of all methods required by
             * implemented interfaces (from both immediate interfaces and their super interfaces).
             */
            for (DeclaredTypeDescriptor interfaceTypeDescriptor :
                getImmediateInterfacesAndTheirSupers(type.getDeclaration())) {
              for (MethodDescriptor methodDescriptor :
                  interfaceTypeDescriptor.getDeclaredMethodDescriptors()) {
                if (methodDescriptor.isStatic()) {
                  continue;
                }
                if (!filledSignatures.add(ManglingNameUtils.getMangledName(methodDescriptor))) {
                  continue;
                }

                addStubMethod(type, methodDescriptor);
              }
            }
          }
        });
  }

  private static Set<String> getDeclaredMethodSignatures(TypeDeclaration classTypeDeclaration) {
    Set<String> signatures = new HashSet<>();
    for (MethodDescriptor methodDescriptor : classTypeDeclaration.getDeclaredMethodDescriptors()) {
      if (methodDescriptor.isConstructor() || methodDescriptor.isStatic()) {
        continue;
      }

      signatures.add(ManglingNameUtils.getMangledName(methodDescriptor));
    }
    return signatures;
  }

  private static Set<DeclaredTypeDescriptor> getImmediateInterfacesAndTheirSupers(
      TypeDeclaration classTypeDeclaration) {
    Set<DeclaredTypeDescriptor> interfaces = new HashSet<>();
    for (DeclaredTypeDescriptor interfaceTypeDescriptor :
        classTypeDeclaration.getInterfaceTypeDescriptors()) {
      interfaces.addAll(getAllSuperInterfaces(interfaceTypeDescriptor));
    }
    return interfaces;
  }

  private static Set<DeclaredTypeDescriptor> getAllSuperInterfaces(
      DeclaredTypeDescriptor interfaceTypeDescriptor) {
    Set<DeclaredTypeDescriptor> interfaces = new HashSet<>();
    interfaces.add(interfaceTypeDescriptor);
    for (DeclaredTypeDescriptor superInterfaceTypeDescriptor :
        interfaceTypeDescriptor.getInterfaceTypeDescriptors()) {
      interfaces.addAll(getAllSuperInterfaces(superInterfaceTypeDescriptor));
    }
    return interfaces;
  }

  private static Set<String> getJsSignaturesInImmediateInterfacesTransitive(
      TypeDeclaration typeDeclaration) {
    return getImmediateInterfacesAndTheirSupers(typeDeclaration)
        .stream()
        .map(DeclaredTypeDescriptor::getDeclaredMethodDescriptors)
        .flatMap(Collection::stream)
        .map(ManglingNameUtils::getMangledName)
        .collect(Collectors.toSet());
  }

  private static void addStubMethod(Type type, MethodDescriptor methodDescriptor) {
    MethodDescriptor stubMethodDescriptor =
        MethodDescriptor.Builder.from(methodDescriptor)
            .setEnclosingTypeDescriptor(type.getTypeDescriptor())
            .setAbstract(true)
            .setNative(false)
            .build();
    type.addMethod(
        Method.newBuilder()
            .setMethodDescriptor(stubMethodDescriptor)
            .setParameters(
                AstUtils.createParameterVariables(
                    stubMethodDescriptor.getParameterTypeDescriptors()))
            .setOverride(true)
            .setSourcePosition(type.getSourcePosition())
            .build());
  }
}
