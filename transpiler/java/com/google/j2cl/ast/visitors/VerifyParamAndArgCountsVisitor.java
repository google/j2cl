package com.google.j2cl.ast.visitors;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.j2cl.ast.AbstractVisitor;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.NewInstance;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.Variable;

import java.util.List;

/**
 * Verifies that the method call argument counts match the method descriptor parameter counts and
 * method declaration parameter counts match the method descriptor.
 */
public class VerifyParamAndArgCountsVisitor extends AbstractVisitor {
  public static void doVerifyParamAndArgCountsVisitor(CompilationUnit compilationUnit) {
    new VerifyParamAndArgCountsVisitor().verifyParamAndArgCountsVisitor(compilationUnit);
  }

  private CompilationUnit compilationUnit;

  @Override
  public void exitMethod(Method method) {
    verifyParameters(method.getParameters(), method.getDescriptor());
  }

  @Override
  public void exitMethodCall(MethodCall methodCall) {
    verifyArguments(methodCall.getArguments(), methodCall.getTarget());
  }

  @Override
  public void exitNewInstance(NewInstance newInstance) {
    verifyArguments(newInstance.getArguments(), newInstance.getTarget());
  }

  private void verifyArguments(
      List<Expression> passedArguments, MethodDescriptor methodDescriptor) {
    ImmutableList<TypeDescriptor> declaredParameterTypes =
        methodDescriptor.getParameterTypeDescriptors();
    Preconditions.checkState(
        passedArguments.size() == declaredParameterTypes.size(),
        "Invalid method call argument count. Expected %s arguments but received "
            + "%s in call to method '%s() from compilation unit %s",
        declaredParameterTypes.size(),
        passedArguments.size(),
        methodDescriptor.getMethodName(),
        compilationUnit.getName());
  }

  private void verifyParamAndArgCountsVisitor(CompilationUnit compilationUnit) {
    this.compilationUnit = compilationUnit;
    compilationUnit.accept(this);
  }

  private void verifyParameters(
      List<Variable> declaredParameters, MethodDescriptor methodDescriptor) {
    ImmutableList<TypeDescriptor> declaredParameterTypes =
        methodDescriptor.getParameterTypeDescriptors();
    Preconditions.checkState(
        declaredParameters.size() == declaredParameterTypes.size(),
        "Invalid method call argument count. Expected %s arguments but received "
            + "%s in call to method '%s() from compilation unit %s",
        declaredParameterTypes.size(),
        declaredParameters.size(),
        methodDescriptor.getMethodName(),
        compilationUnit.getName());
  }
}
