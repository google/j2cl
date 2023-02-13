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
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.j2cl.transpiler.ast.TypeDescriptor.replaceTypeDescriptors;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.ArrayLiteral;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.BinaryExpression;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.Field;
import com.google.j2cl.transpiler.ast.FieldAccess;
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
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.StringLiteral;
import com.google.j2cl.transpiler.ast.SuperReference;
import com.google.j2cl.transpiler.ast.ThisOrSuperReference;
import com.google.j2cl.transpiler.ast.ThisReference;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptor.TypeReplacer;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.Variable;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nullable;

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

    Set<TypeDeclaration> inlinableTypes =
        library
            .streamTypes()
            .filter(t -> canBeInlinedTo(t))
            .map(Type::getDeclaration)
            .collect(toImmutableSet());
    if (inlinableTypes.isEmpty()) {
      // This also means there are not AutoValue types, so there is nothing to optimize here.
      return;
    }

    inlineImplementationTypes(library, inlinableTypes);
    optimizeAsValueTypes(library);
  }

  private static void inlineImplementationTypes(
      Library library, Set<TypeDeclaration> optimizableTypes) {

    ImmutableMap<TypeDeclaration, Type> superTypeToInlinedType =
        library
            .streamTypes()
            .filter(t -> optimizableTypes.contains(t.getDeclaration().getSuperTypeDeclaration()))
            // Filter the unlikely empty AutoValue case to avoid handling edge cases.
            .filter(t -> !Iterables.isEmpty(getInstanceFields(t.getDeclaration())))
            .collect(
                toImmutableMap(
                    t -> t.getSuperTypeDescriptor().getTypeDeclaration(), Function.identity()));

    // Inline the types.
    library
        .streamTypes()
        .forEach(
            type -> {
              Type typeToInline = superTypeToInlinedType.get(type.getDeclaration());
              if (typeToInline != null) {
                inlineMembers(typeToInline, type);
                inlineNestedTypes(typeToInline, type);
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
    if (type.getDeclaration().isAnnotatedWithAutoValueBuilder()) {
      // Note that AutoValue.Builder will generate default ctor so would be only safe to inline
      // the implementation if user didn't declare non-empty one.
      // Most complete logic for safety here would be cross-checking all generated ctors against
      // user declared ones but given the practical constraints of AutoValue, checking only this
      // edge case should be good enough.
      Method method = type.getDefaultConstructor();
      return method == null || method.isEmpty();
    }
    return type.getDeclaration().isAnnotatedWithAutoValue();
  }

  private static void inlineMembers(Type from, Type to) {
    // Move all the members to base class except the java.lang.Object methods.
    // We could also have moved members from all subclasses however @Memoized methods are
    // overridden hence requires more complicated re-writing.

    // Validate our assumption that AutoValue only generates single non-default constructor.
    checkState(
        !to.getDeclaration().isAnnotatedWithAutoValue()
            || (from.getConstructors().size() == 1 && from.getDefaultConstructor() == null));

    // We need to make sure inlined constructors explicitly call this() otherwise they would
    // implicitly call super() and change the behavior from the original code (since existing
    // implicit super() call was targeting the parent default constructor).
    addThisCallToInlinedConstructors(from, to);

    List<Member> movedMembers = from.getMembers();

    // Ensure no static fields, they are not expected and wouldn't be handled properly.
    // TODO(b/193926520): Remove this when we start rewriting enclosing type of fields.
    movedMembers.removeIf(
        m -> {
          if (m.isStatic()) {
            checkState(m.getDescriptor().getName().equals("serialVersionUID"), "Unexpected field");
            return true;
          }
          return false;
        });

    // Note that the collection here will error out in duplicate keys so if any our
    // assumptions incorrect (e.g. no multiple static initializer), we should fail-fast.
    ImmutableMap<String, Member> movedMembersByMangledName =
        Maps.uniqueIndex(movedMembers, Member::getMangledName);

    to.getMembers()
        .removeIf(
            m -> {
              if (movedMembersByMangledName.containsKey(m.getMangledName())) {
                // We should never end up replacing a non-empty method.
                checkState(!m.isMethod() || ((Method) m).isEmpty(), m);
                return true;
              }
              return false;
            });

    // Note that the adding to end here matters since later preserveFields will assume last
    // constructor is the one that is coming from AutoValue implementation class.
    to.addMembers(movedMembers);
  }

  private static void addThisCallToInlinedConstructors(Type from, Type to) {
    if (from.getDefaultConstructor() != null) {
      // If there is new default constructor being inlined then there is nothing to worry about
      // since either default doesn't exist or it is empty. Hence the implicit behavior is correct.
      return;
    }

    Method defaultCtor = to.getDefaultConstructor();
    if (defaultCtor == null) {
      // If there are no default constructors in the parent, there is nothing to update since
      // implicit behavior is still correct.
      // TODO(b/215777271): Handle single varargs constructors which are currently not considered
      // as targets for the parameterless and implicit super invocation but should.
      return;
    }

    for (Method fromCtor : from.getConstructors()) {
      checkState(!AstUtils.hasThisCall(fromCtor) && !AstUtils.hasSuperCall(fromCtor));
      fromCtor
          .getBody()
          .getStatements()
          .add(
              0,
              MethodCall.Builder.from(defaultCtor.getDescriptor())
                  .build()
                  .makeStatement(fromCtor.getBody().getSourcePosition()));
    }
  }

  private static void inlineNestedTypes(Type from, Type to) {
    // Move all the directly nested types to the base class.
    from.getTypes().forEach(to::addType);
    from.getTypes().clear();
  }

  private static void rewriteTypeReferences(Library library, TypeReplacer fn) {
    library.accept(
        new AbstractRewriter() {

          @Nullable
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
              return Invocation.Builder.from(expr).setTarget(newDescriptor).build();
            }
            return expr;
          }

          @Override
          public Variable rewriteVariable(Variable variable) {
            TypeDescriptor descriptor = variable.getTypeDescriptor();
            TypeDescriptor newDescriptor = replaceTypeDescriptors(descriptor, fn);
            if (descriptor != newDescriptor) {
              // Don't replace the variable object since the references in the code point to it
              // that would need to be changed; just set the variable to its new type.
              variable.setTypeDescriptor(newDescriptor);
            }
            return variable;
          }

          @Override
          public Expression rewriteThisOrSuperReference(ThisOrSuperReference receiverReference) {
            DeclaredTypeDescriptor descriptor = receiverReference.getTypeDescriptor();
            DeclaredTypeDescriptor newDescriptor = replaceTypeDescriptors(descriptor, fn);
            if (descriptor != newDescriptor) {
              return receiverReference instanceof ThisReference
                  ? new ThisReference(newDescriptor)
                  : new SuperReference(newDescriptor);
            }
            return receiverReference;
          }

          private MethodDescriptor rewriteMethodDescriptor(MethodDescriptor descriptor) {
            return descriptor.transform(
                builder -> {
                  DeclaredTypeDescriptor newEnclosingTypeDescriptor =
                      replaceTypeDescriptors(builder.getEnclosingTypeDescriptor(), fn);
                  if (!newEnclosingTypeDescriptor.equals(builder.getEnclosingTypeDescriptor())
                      && descriptor.isJsMember()) {
                    // When a method moves from one class to another, it might no longer override
                    // a JsMethod and loose the fact that it needs to remain a JsMethod in the
                    // new class.
                    builder.setOriginalJsInfo(descriptor.getJsInfo());
                  }
                  builder
                      .setEnclosingTypeDescriptor(newEnclosingTypeDescriptor)
                      .setReturnTypeDescriptor(
                          replaceTypeDescriptors(builder.getReturnTypeDescriptor(), fn))
                      .updateParameterTypeDescriptors(
                          replaceTypeDescriptors(builder.getParameterTypeDescriptors(), fn));
                });
          }

          private FieldDescriptor rewriteFieldDescriptor(FieldDescriptor descriptor) {
            // Note that we are not re-writing enclosing type to preserve the mangling to avoid
            // potential name collisions.
            // TODO(b/193926520): Add getManglingDescriptor concept similar to MethodDescriptor so
            // that we can set an origin of FieldDescriptor to preserve its mangled name. This will
            // let us set correct enclosing type while preserving manged names.
            return descriptor.transform(
                builder ->
                    builder.setTypeDescriptor(
                        replaceTypeDescriptors(builder.getTypeDescriptor(), fn)));
          }
        });
  }

  private void optimizeAsValueTypes(Library library) {
    // Note that AutoValue classes can have multiple subtypes due the way AutoValue extensions work.
    Multimap<TypeDeclaration, Type> autoValueToSubTypes = ArrayListMultimap.create();
    library
        .streamTypes()
        .forEach(
            t -> {
              TypeDeclaration autoValue =
                  getAutoValueParent(t.getDeclaration().getSuperTypeDeclaration());
              if (autoValue != null) {
                autoValueToSubTypes.put(autoValue, t);
              }
            });

    Multimap<TypeDeclaration, FieldDescriptor> autoValueToExcludedFields =
        library
            .streamTypes()
            .map(Type::getDeclaration)
            .filter(TypeDeclaration::isAnnotatedWithAutoValue)
            .map(t -> getAutoValueExcludedFields(t, autoValueToSubTypes.get(t)))
            .collect(ArrayListMultimap::create, Multimap::putAll, Multimap::putAll);

    library
        .streamTypes()
        .filter(t -> t.getDeclaration().isAnnotatedWithAutoValue())
        .forEach(
            autoValue -> {
              int mask = removeJavaLangObjectMethods(autoValue);
              if (mask == 0) {
                // No method removed/needs optimization. Leave the type alone.
                return;
              }

              Collection<FieldDescriptor> excludedFields =
                  autoValueToExcludedFields.get(autoValue.getDeclaration());

              if (TypeDescriptors.isJavaLangObject(autoValue.getSuperTypeDescriptor())) {
                // Change the parent type of AutoValue class to ValueType.
                autoValue.setSuperTypeDescriptor(TypeDescriptors.get().javaemulInternalValueType);
                // Recorded excluded fields if there are any.
                if (!excludedFields.isEmpty()) {
                  addExcludedFieldsDeclaration(autoValue, excludedFields);
                }
              } else {
                // Add mixin that will provide the implementation for java.lang.Object.
                addValueTypeMixin(autoValue, mask, excludedFields);
              }

              // Prevent JsCompiler from optimizing the fields to not break the semantics.
              // If we don't take any special precautions, if a field is never read explicitly (even
              // it is set), JsCompiler can optimize them away. As a result they won't be seen
              // reflectively and they won't contribute to equals/hashcode/toString.
              preserveFields(autoValue, excludedFields);
            });
  }

  @Nullable
  private static TypeDeclaration getAutoValueParent(TypeDeclaration type) {
    return type == null
        ? null
        : type.isAnnotatedWithAutoValue()
            ? type
            : getAutoValueParent(type.getSuperTypeDeclaration());
  }

  private static Multimap<TypeDeclaration, FieldDescriptor> getAutoValueExcludedFields(
      TypeDeclaration type, Collection<Type> subtypes) {
    ArrayListMultimap<TypeDeclaration, FieldDescriptor> excludedFields = ArrayListMultimap.create();

    // None of the user declared fields in AutoValue and its parents are included in AutoValue
    // generated equals/hashCode/toString.
    if (type.isAnnotatedWithAutoValue()) {
      for (TypeDeclaration t = type;
          !TypeDescriptors.isJavaLangObject(t.toRawTypeDescriptor());
          t = t.getSuperTypeDeclaration()) {
        excludedFields.putAll(type, getInstanceFields(t));
      }
    }

    // Fields declared in subclasses of AutoValue impl (i.e. code generated by extensions) are also
    // not included in calculations.
    subtypes.stream()
        .map(Type::getDeclaration)
        // Skip the AutoValue impl.
        .filter(t -> !t.getSuperTypeDeclaration().isAnnotatedWithAutoValue())
        .forEach(t -> excludedFields.putAll(type, getInstanceFields(t)));

    return excludedFields;
  }

  private static Iterable<FieldDescriptor> getInstanceFields(TypeDeclaration t) {
    return Iterables.filter(t.getDeclaredFieldDescriptors(), FieldDescriptor::isInstanceMember);
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

  private static void addValueTypeMixin(
      Type autoValue, int mask, Collection<FieldDescriptor> excludedFields) {
    MethodDescriptor mixinMethodDescriptor =
        TypeDescriptors.get().javaemulInternalValueType.getMethodDescriptorByName("mixin");

    MethodCall mixinCall =
        MethodCall.Builder.from(mixinMethodDescriptor)
            .setArguments(
                new JavaScriptConstructorReference(autoValue.getDeclaration()),
                new JavaScriptConstructorReference(
                    TypeDescriptors.get().javaemulInternalValueType.getTypeDeclaration()),
                NumberLiteral.fromInt(mask),
                getProperyNameExpressions(autoValue.getDeclaration(), excludedFields))
            .build();
    autoValue.addLoadTimeStatement(mixinCall.makeStatement(SourcePosition.NONE));
  }

  private static void preserveFields(Type type, Collection<FieldDescriptor> excludedFields) {
    MethodDescriptor preserveFn =
        TypeDescriptors.get().javaemulInternalValueType.getMethodDescriptorByName("preserve");

    List<Expression> fieldReferences =
        type.getFields().stream()
            .map(Field::getDescriptor)
            .filter(f -> f.isInstanceMember() && !excludedFields.contains(f))
            .map(
                f ->
                    FieldAccess.Builder.from(f)
                        .setQualifier(new ThisReference(type.getTypeDescriptor()))
                        .build())
            .collect(toImmutableList());

    // This special call will make JsCompiler think that all these fields are used. There is a
    // special pass in JsCompiler that later removes this call itself so they won't exist in the
    // final output.
    Statement preserveCall =
        MethodCall.Builder.from(preserveFn)
            .setArguments(AstUtils.maybePackageVarargs(preserveFn, fieldReferences))
            .build()
            .makeStatement(SourcePosition.NONE);

    // Hack: Using the last constructor here since AutoValue constructor is appended to end.
    Iterables.getLast(type.getConstructors()).getBody().getStatements().add(preserveCall);
  }

  private static void addExcludedFieldsDeclaration(
      Type autoValue, Collection<FieldDescriptor> excludedFields) {
    FieldDescriptor excludedFieldDescriptor =
        FieldDescriptor.newBuilder()
            .setOriginalJsInfo(JsInfo.RAW_FIELD)
            .setEnclosingTypeDescriptor(TypeDescriptors.get().javaLangObject)
            .setTypeDescriptor(TypeDescriptors.get().javaLangObject)
            .setName("$excluded_fields")
            .build();
    Expression excludedFieldAccess = createPrototypeFieldAccess(autoValue, excludedFieldDescriptor);
    // Adds load time statement MyFoo.prototype.$excluded_fields = [ ... ]
    autoValue.addLoadTimeStatement(
        BinaryExpression.Builder.asAssignmentTo(excludedFieldAccess)
            .setRightOperand(getProperyNameExpressions(autoValue.getDeclaration(), excludedFields))
            .build()
            .makeStatement(SourcePosition.NONE));
  }

  private static ArrayLiteral getProperyNameExpressions(
      TypeDeclaration type, Collection<FieldDescriptor> fields) {
    // Note that ValueType.objectProperty is just a native proxy for goog.reflect.objectProperty.
    MethodCall.Builder objectPropertyCallBuilder =
        MethodCall.Builder.from(
            TypeDescriptors.get()
                .javaemulInternalValueType
                .getMethodDescriptorByName("objectProperty"));
    // Generates an array literal in the form of:
    // [
    //   ValueType.objectProperty("<property1>", MyType),
    //   ValueType.objectProperty("<property2>", MyType),
    //   ...
    // ]
    return new ArrayLiteral(
        TypeDescriptors.get().javaLangObjectArray,
        fields.stream()
            .map(FieldDescriptor::getMangledName)
            .map(StringLiteral::new)
            .map(
                name ->
                    objectPropertyCallBuilder
                        .setArguments(name, new JavaScriptConstructorReference(type))
                        .build())
            .toArray(Expression[]::new));
  }

  private static Expression createPrototypeFieldAccess(Type type, FieldDescriptor field) {
    return FieldAccess.Builder.from(field)
        .setQualifier(
            new JavaScriptConstructorReference(type.getDeclaration()).getPrototypeFieldAccess())
        .build();
  }
}
