/*
 * Copyright 2020 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.j2cl.transpiler.passes;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.j2cl.transpiler.ast.TypeDescriptor.replaceTypeDescriptors;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Field;
import com.google.j2cl.transpiler.ast.FieldDescriptor;
import com.google.j2cl.transpiler.ast.InstanceOfExpression;
import com.google.j2cl.transpiler.ast.Invocation;
import com.google.j2cl.transpiler.ast.JavaScriptConstructorReference;
import com.google.j2cl.transpiler.ast.JsInfo;
import com.google.j2cl.transpiler.ast.Library;
import com.google.j2cl.transpiler.ast.Member;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.NumberLiteral;
import com.google.j2cl.transpiler.ast.PrimitiveTypes;
import com.google.j2cl.transpiler.ast.ReturnStatement;
import com.google.j2cl.transpiler.ast.ThisReference;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptor.TypeReplacer;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.Variable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/** Optimize AutoValue generated classes to reduce their code size. */
public class OptimizeAutoValue extends LibraryNormalizationPass {

  private final boolean enabled;

  public OptimizeAutoValue(boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public void applyTo(Library library) {
    if (!enabled) {
      return;
    }

    inlineImplementationTypes(library);
    optimizeAsValueTypes(library);
  }

  private static void inlineImplementationTypes(Library library) {
    Set<TypeDeclaration> optimizableTypes =
        library.getCompilationUnits().stream()
            .flatMap(c -> c.getTypes().stream())
            .filter(t -> canBeInlinedTo(t))
            .map(Type::getDeclaration)
            .collect(toImmutableSet());
    if (optimizableTypes.isEmpty()) {
      return;
    }

    Map<TypeDeclaration, Type> superTypeToInlinedType =
        library.getCompilationUnits().stream()
            .flatMap(c -> c.getTypes().stream())
            .filter(t -> optimizableTypes.contains(t.getDeclaration().getSuperTypeDeclaration()))
            .collect(
                toImmutableMap(
                    t -> t.getSuperTypeDescriptor().getTypeDeclaration(), Function.identity()));

    // Inline the types.
    library.getCompilationUnits().stream()
        .flatMap(c -> c.getTypes().stream())
        .forEach(
            type -> {
              Type typeToInline = superTypeToInlinedType.get(type.getDeclaration());
              if (typeToInline != null) {
                inlineMembers(typeToInline, type);
                type.setAbstract(typeToInline.isAbstract());
              }
            });

    // Update the related type references in the AST.
    rewriteTypeReferences(
        library,
        new TypeReplacer() {
          @Override
          public <T extends TypeDescriptor> T apply(T t) {
            if (t instanceof DeclaredTypeDescriptor) {
              DeclaredTypeDescriptor declaredTypeDescriptor = (DeclaredTypeDescriptor) t;
              TypeDeclaration superType =
                  declaredTypeDescriptor.getTypeDeclaration().getSuperTypeDeclaration();
              if (superTypeToInlinedType.containsKey(superType)) {
                declaredTypeDescriptor =
                    declaredTypeDescriptor.isNullable()
                        ? declaredTypeDescriptor.getSuperTypeDescriptor().toNullable()
                        : declaredTypeDescriptor.getSuperTypeDescriptor().toNonNullable();
              }
              // This is safe since replacement can only be DeclaredTypeDescriptor as it is 'final'.
              @SuppressWarnings("unchecked")
              T replacement = (T) declaredTypeDescriptor;
              t = replacement;
            }
            return checkNotNull(t);
          }
        });
  }

  private static boolean canBeInlinedTo(Type type) {
    // Note that AutoValue will generate default ctor for Builders so would be only safe to inline
    // the implementation if user didn't declare non-empty one.
    // Most complete logic for safety here would be cross-checking all generated ctors against user
    // declared ones but given the practical constraints of AutoValue, checking only this edge case
    // should be good enough.
    return type.getDeclaration().isAnnotatedWithAutoValue()
        || (type.getDeclaration().isAnnotatedWithAutoValueBuilder()
            && type.getConstructors().stream()
                .filter(c -> c.getParameters().isEmpty())
                .allMatch(Method::isEmpty));
  }

  private static void inlineMembers(Type from, Type to) {
    // Move all the members to base class except the java.lang.Object methods.
    // We could also have moved members from all subclasses however @Memoized methods are
    // overridden hence requires more complicated re-writing.

    // Note that the collection here will error out in duplicate keys so if any our
    // assumptions incorrect (e.g. no multiple static initializer), we should fail-fast.
    ImmutableMap<String, Member> movedMembers =
        Maps.uniqueIndex(from.getMembers(), Member::getMangledName);

    to.getMembers()
        .removeIf(
            m -> {
              if (movedMembers.containsKey(m.getMangledName())) {
                // We should never end up replacing a non-empty method.
                checkState(!m.isMethod() || ((Method) m).isEmpty(), m);
                return true;
              }
              return false;
            });

    to.addMembers(movedMembers.values());
  }

  private static void rewriteTypeReferences(Library library, TypeReplacer fn) {
    library.accept(
        new AbstractRewriter() {

          @Override
          public Type rewriteType(Type type) {
            if (type.getTypeDescriptor() != fn.apply(type.getTypeDescriptor())) {
              // Type is replaced; we can remove it.
              return null;
            }

            type.setSuperTypeDescriptor(replaceTypeDescriptors(type.getSuperTypeDescriptor(), fn));

            // We should also re-write the Type#getDeclaration however since we cannot re-write
            // variables in the declaration and super type is already rewritten there is no
            // value left.

            return type;
          }

          @Override
          public InstanceOfExpression rewriteInstanceOfExpression(InstanceOfExpression expr) {
            TypeDescriptor descriptor = expr.getTestTypeDescriptor();
            TypeDescriptor newDescriptor = replaceTypeDescriptors(descriptor, fn);
            if (descriptor != newDescriptor) {
              return InstanceOfExpression.Builder.from(expr)
                  .setTestTypeDescriptor(newDescriptor)
                  .build();
            }
            return expr;
          }

          @Override
          public CastExpression rewriteCastExpression(CastExpression expr) {
            TypeDescriptor descriptor = expr.getCastTypeDescriptor();
            TypeDescriptor newDescriptor = replaceTypeDescriptors(descriptor, fn);
            if (descriptor != newDescriptor) {
              return CastExpression.Builder.from(expr).setCastTypeDescriptor(newDescriptor).build();
            }
            return expr;
          }

          @Override
          public Field rewriteField(Field field) {
            FieldDescriptor descriptor = field.getDescriptor();
            FieldDescriptor newDescriptor = rewriteFieldDescriptor(descriptor);
            if (descriptor != newDescriptor) {
              field = Field.Builder.from(field).setDescriptor(newDescriptor).build();
            }
            return field;
          }

          @Override
          public Method rewriteMethod(Method method) {
            MethodDescriptor descriptor = method.getDescriptor();
            MethodDescriptor newDescriptor = rewriteMethodDescriptor(descriptor);
            if (descriptor != newDescriptor) {
              method = Method.Builder.from(method).setMethodDescriptor(newDescriptor).build();
            }
            return method;
          }

          @Override
          public Invocation rewriteInvocation(Invocation expr) {
            MethodDescriptor descriptor = expr.getTarget();
            MethodDescriptor newDescriptor = rewriteMethodDescriptor(descriptor);
            if (descriptor != newDescriptor) {
              return Invocation.Builder.from(expr).setMethodDescriptor(newDescriptor).build();
            }
            return expr;
          }

          @Override
          public ReturnStatement rewriteReturnStatement(ReturnStatement statement) {
            TypeDescriptor descriptor = statement.getTypeDescriptor();
            TypeDescriptor newDescriptor = replaceTypeDescriptors(descriptor, fn);
            if (descriptor != newDescriptor) {
              return ReturnStatement.Builder.from(statement)
                  .setTypeDescriptor(newDescriptor)
                  .build();
            }
            return statement;
          }

          @Override
          public Variable rewriteVariable(Variable variable) {
            TypeDescriptor descriptor = variable.getTypeDescriptor();
            TypeDescriptor newDescriptor = replaceTypeDescriptors(descriptor, fn);
            if (descriptor != newDescriptor) {
              return Variable.Builder.from(variable).setTypeDescriptor(newDescriptor).build();
            }
            return variable;
          }

          @Override
          public ThisReference rewriteThisReference(ThisReference expr) {
            DeclaredTypeDescriptor descriptor = expr.getTypeDescriptor();
            DeclaredTypeDescriptor newDescriptor = replaceTypeDescriptors(descriptor, fn);
            if (descriptor != newDescriptor) {
              return new ThisReference(newDescriptor);
            }
            return expr;
          }

          private MethodDescriptor rewriteMethodDescriptor(MethodDescriptor descriptor) {
            return MethodDescriptor.Builder.from(descriptor)
                .setDeclarationDescriptor(
                    descriptor.isDeclaration()
                        ? null
                        : rewriteMethodDescriptor(descriptor.getDeclarationDescriptor()))
                .setEnclosingTypeDescriptor(
                    replaceTypeDescriptors(descriptor.getEnclosingTypeDescriptor(), fn))
                .setReturnTypeDescriptor(
                    replaceTypeDescriptors(descriptor.getReturnTypeDescriptor(), fn))
                .updateParameterTypeDescriptors(
                    replaceTypeDescriptors(descriptor.getParameterTypeDescriptors(), fn))
                .build();
          }

          private FieldDescriptor rewriteFieldDescriptor(FieldDescriptor descriptor) {
            // Note that we are not re-writing enclosing type to preserve the mangling to avoid
            // potential name collisions.
            // TODO(b/193926520): Add getManglingDescriptor concept similar to MethodDescriptor so
            // that we can set an origin of FieldDescriptor to preserve its mangled name. This will
            // let us set correct enclosing type while preserving manged names.
            return FieldDescriptor.Builder.from(descriptor)
                .setDeclarationDescriptor(
                    descriptor.isDeclaration()
                        ? null
                        : rewriteFieldDescriptor(descriptor.getDeclarationDescriptor()))
                .setTypeDescriptor(replaceTypeDescriptors(descriptor.getTypeDescriptor(), fn))
                .build();
          }
        });
  }

  private void optimizeAsValueTypes(Library library) {
    library.getCompilationUnits().stream()
        .flatMap(c -> c.getTypes().stream())
        .filter(t -> t.getDeclaration().isAnnotatedWithAutoValue())
        .forEach(
            autoValue -> {
              int mask = removeJavaLangObjectMethods(autoValue);
              if (TypeDescriptors.isJavaLangObject(autoValue.getSuperTypeDescriptor())) {
                // Change the parent type of AutoValue class to ValueType.
                autoValue.setSuperTypeDescriptor(TypeDescriptors.get().javaemulInternalValueType);
              } else {
                // Add mixin that will provide the implementation for java.lang.Object.
                addValueTypeMixin(autoValue, mask);
              }
            });
  }

  /** @return mask summarizes the removed methods. */
  private static int removeJavaLangObjectMethods(Type type) {
    Set<Method> generatedObjectMethods = new HashSet<>();
    int mask = 0;
    for (Method method : type.getMethods()) {
      if (!method.getDescriptor().isOrOverridesJavaLangObjectMethod()) {
        continue;
      }

      // Skip Object methods that are provided by user. We know such methods since they exist in the
      // type descriptor while others will not as their origin is the generated class.
      if (type.getTypeDescriptor()
          .getDeclaredMemberDescriptors()
          .contains(method.getDescriptor())) {
        continue;
      }

      switch (method.getDescriptor().getName()) {
        case "equals":
          mask |= 1;
          break;
        case "hashCode":
          mask |= 2;
          break;
        case "toString":
          mask |= 4;
          break;
        default:
          throw new AssertionError(method.getDescriptor());
      }

      generatedObjectMethods.add(method);
    }

    type.getMembers().removeAll(generatedObjectMethods);

    return mask;
  }

  private static void addValueTypeMixin(Type type, int mask) {
    MethodDescriptor mixinMethodDescriptor =
        MethodDescriptor.newBuilder()
            .setJsInfo(JsInfo.RAW)
            .setStatic(true)
            .setEnclosingTypeDescriptor(TypeDescriptors.get().javaemulInternalValueType)
            .setName("mixin")
            .setParameterTypeDescriptors(TypeDescriptors.get().nativeFunction, PrimitiveTypes.INT)
            .build();
    MethodCall mixinCall =
        MethodCall.Builder.from(mixinMethodDescriptor)
            .setArguments(
                new JavaScriptConstructorReference(type.getDeclaration()),
                NumberLiteral.fromInt(mask))
            .build();
    type.addLoadTimeStatement(mixinCall.makeStatement(type.getSourcePosition()));
  }
}
