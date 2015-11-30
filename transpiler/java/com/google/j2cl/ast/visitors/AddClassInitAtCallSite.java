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

import com.google.common.base.Preconditions;
import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.AbstractVisitor;
import com.google.j2cl.ast.Block;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.ExpressionStatement;
import com.google.j2cl.ast.Field;
import com.google.j2cl.ast.FieldAccess;
import com.google.j2cl.ast.JavaType;
import com.google.j2cl.ast.JsInfo;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.Statement;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.Visibility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;

/**
 * This is a temporary workaround to bug b/25512693
 * which was calling class inits during the declaration phase since getters were being accessed
 * to copy over static fields for subclasses.  With this pass, we move the calls to clinit to the
 * call site, before the static field is accessed.
 */
public class AddClassInitAtCallSite {
  public static void applyTo(CompilationUnit compilationUnit) {
    StaticFieldAccessGatherer gatherer = new StaticFieldAccessGatherer();
    compilationUnit.accept(gatherer);

    ClinitCreator rewriter = new ClinitCreator(gatherer.initializedTypesByMethod);
    compilationUnit.accept(rewriter);
  }

  /**
   * We look for static field access inside methods and field initializers.
   */
  private static class StaticFieldAccessGatherer extends AbstractVisitor {
    // We use SortedSet such that the import orders are deterministic.
    private Map<MethodDescriptor, SortedSet<TypeDescriptor>> initializedTypesByMethod =
        new HashMap<>();
    private Stack<Block> enteredBlocks = new Stack<>();

    @Override
    public boolean enterBlock(Block node) {
      enteredBlocks.push(node);
      return super.enterBlock(node);
    }

    @Override
    public void exitBlock(Block node) {
      enteredBlocks.pop();
      super.exitBlock(node);
    }

    @Override
    public boolean enterFieldAccess(FieldAccess fieldAccess) {
      if (!fieldAccess.getTarget().isStatic()) {
        return true;
      }
      JavaType currentType = getCurrentJavaType();
      if (fieldAccess.getTarget().getEnclosingClassTypeDescriptor()
          == currentType.getDescriptor()) {
        // The class already calls its own clinit so don't do anything.
        return true;
      }
      if (fieldAccess.getTarget().getEnclosingClassTypeDescriptor().isNative()) {
        // Do not call clinit on native js types.
        return true;
      }
      Method currentmethod = getCurrentMethod();
      if (currentmethod != null) {
        addTypeToMapForMethod(
            fieldAccess.getTarget().getEnclosingClassTypeDescriptor(),
            currentmethod.getDescriptor());
        return true;
      }
      Field currentField = getCurrentField();
      if (currentField == null) {
        Preconditions.checkArgument(!enteredBlocks.empty());
        Block topLevelBlock = enteredBlocks.get(0);
        if (currentType.getStaticInitializerBlocks().contains(topLevelBlock)) {
          // Inside a static initializer block so add the clinits to the class clinit.
          currentType
              .getStaticFieldClinits()
              .add(fieldAccess.getTarget().getEnclosingClassTypeDescriptor());
        } else {
          // Inside a instance initializer block so add the clinits to the constructor.
          for (Method constructor : currentType.getConstructors()) {
            addTypeToMapForMethod(
                fieldAccess.getTarget().getEnclosingClassTypeDescriptor(),
                constructor.getDescriptor());
          }
        }
        return true;
      }
      if (currentField.getDescriptor().isStatic()) {
        // The field is static so add it to the clinit
        // This is kind of hacky but it avoids extra communication between the ClinitCreator.
        currentType
            .getStaticFieldClinits()
            .add(fieldAccess.getTarget().getEnclosingClassTypeDescriptor());
        return true;
      } else {
        // The field is an an instance type so add it to the constructor.
        for (Method constructor : currentType.getConstructors()) {
          addTypeToMapForMethod(
              fieldAccess.getTarget().getEnclosingClassTypeDescriptor(),
              constructor.getDescriptor());
        }
        return true;
      }
    }

    private void addTypeToMapForMethod(TypeDescriptor type, MethodDescriptor method) {
      SortedSet<TypeDescriptor> staticClassesUsed = initializedTypesByMethod.get(method);
      if (staticClassesUsed == null) {
        staticClassesUsed = new TreeSet<>();
        initializedTypesByMethod.put(method, staticClassesUsed);
      }
      staticClassesUsed.add(type);
    }
  }

  /**
   * This class inserts clinit calls at the beginning of every method that contains a static field
   * access according to the initializedTypesByMethod map.
   */
  private static class ClinitCreator extends AbstractRewriter {
    private Map<MethodDescriptor, SortedSet<TypeDescriptor>> initializedTypesByMethod;

    public ClinitCreator(Map<MethodDescriptor, SortedSet<TypeDescriptor>> map) {
      this.initializedTypesByMethod = map;
    }

    @Override
    public Node rewriteMethod(Method node) {
      SortedSet<TypeDescriptor> staticFieldTypesUsed =
          initializedTypesByMethod.get(node.getDescriptor());
      if (staticFieldTypesUsed != null) {
        List<Statement> classInits = new ArrayList<>();
        for (TypeDescriptor type : staticFieldTypesUsed) {
          classInits.add(clinitCall(type));
        }
        List<Statement> classInitsAndBody = new ArrayList<>(classInits);
        classInitsAndBody.addAll(node.getBody().getStatements());
        node.setBody(new Block(classInitsAndBody));
      }
      return node;
    }
  }

  private static Statement clinitCall(TypeDescriptor type) {
    MethodDescriptor clinitMethodDescriptor =
        MethodDescriptor.createRaw(
            true,
            Visibility.PUBLIC,
            type,
            "$clinit",
            new ArrayList<TypeDescriptor>(),
            TypeDescriptors.get().primitiveVoid,
            JsInfo.NONE);
    MethodCall clinitCall =
        MethodCall.createRegularMethodCall(
            null, clinitMethodDescriptor, new ArrayList<Expression>());
    return new ExpressionStatement(clinitCall);
  }
}
