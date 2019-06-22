/*
 * Copyright 2016 Google Inc.
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
package com.google.j2cl.junit.async;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.reflect.Reflection;
import com.google.common.util.concurrent.SettableFuture;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

/**
 * Experimental, do not use!
 *
 * <p>Should really be living in guava or xplat for cross platform async testing. This probably
 * should also support a common async abstraction on top of a promise.
 *
 * <p>A test runner that allows for asynchronous test using a structural Promise.
 *
 * <p>A structural promise to this runner is a class that conforms to the following conditions:
 *
 * <ul>
 *   <li>A method called then
 *   <li>The then method must have two arguments
 *   <li>Both arguments have to be an interface
 *   <li>Arguments must be either be @FunctionalInterface or @JsFunction and thus can only have one
 *       method
 *   <li>The second argument is assumed to be the errorCallback and thus needs to take a subclass of
 *       Throwable as parameter
 * </ul>
 */
public class J2clAsyncTestRunner extends BlockJUnit4ClassRunner {

  private static final String PROMISE_LIKE =
      "A promise-like type is a type that is annotated with @JsType "
          + "and has a 'then' method. 'then' method should have a 'success' callback parameter "
          + "and an optional 'failure' callback parameter.";

  public enum ErrorMessage {
    ASYNC_NO_TIMEOUT(
        "Method %s is missing @Test timeout attribute but returns a promise-like" + " type."),
    ASYNC_HAS_EXPECTED_EXCEPTION(
        "Method %s has expectedException attribute but returns a promise-like type."),
    NO_THEN_METHOD(
        "Type %s is not a promise-like type. It's missing a 'then' method with two paramters. "
            + PROMISE_LIKE),
    MULTIPLE_THEN_METHOD(
        "Type %s is not a promise-like type. It has multiple 'then' methods with two paramters. "
            + PROMISE_LIKE),
    INVALID_CALLBACK_PARAMETER(
        "Type '%s' is not a promise-like type,"
            + " since the argument '%s' on the 'then' is not a @JsFunction/@FunctionalInterface. "
            + PROMISE_LIKE);

    private final String formattedMsg;

    private ErrorMessage(String formattedMsg) {
      this.formattedMsg = formattedMsg;
    }

    public String format(Object... args) {
      return String.format(formattedMsg, args);
    }
  }

  private static class PromiseStatement extends Statement {

    private final SettableFuture<Void> future = SettableFuture.create();

    private final FrameworkMethod method;
    private final Object test;

    public PromiseStatement(FrameworkMethod method, Object test) {
      this.method = method;
      this.test = test;
    }

    @Override
    public void evaluate() throws Throwable {
      Object promiseLike = method.invokeExplosively(test);
      registerCallbacks(promiseLike);
      try {
        future.get();
      } catch (ExecutionException e) {
        throw e.getCause();
      }
    }

    private void registerCallbacks(Object promiseLike) throws Exception {
      checkState(promiseLike != null, "Test returned null as its promise");
      PromiseType promiseType = getPromiseType(promiseLike.getClass());
      Object successCallback = createSuccessCallback(promiseType);
      Object errorCallback = createErrorCallback(promiseType);
      promiseType.thenMethod.invoke(promiseLike, successCallback, errorCallback);
    }

    private Object createErrorCallback(PromiseType promiseType) {
      return Reflection.newProxy(
          promiseType.errorCallbackType,
          (proxy, method, args) -> {
            Throwable t = (Throwable) args[0];
            future.setException(t);
            return t;
          });
    }

    private Object createSuccessCallback(PromiseType promiseType) {
      return Reflection.newProxy(
          promiseType.successCallbackType,
          (proxy, method, args) -> {
            future.set(null);
            return args[0];
          });
    }
  }

  private static class PromiseType {
    final Method thenMethod;

    final Class<?> successCallbackType;

    final Class<?> errorCallbackType;

    public PromiseType(
        Method thenMethod, Class<?> successCallbackType, Class<?> errorCallbackType) {
      this.thenMethod = thenMethod;
      this.successCallbackType = successCallbackType;
      this.errorCallbackType = errorCallbackType;
    }
  }

  private static class InvalidTypeException extends Exception {

    public InvalidTypeException(String message) {
      super(message);
    }
  }

  public J2clAsyncTestRunner(Class<?> klass) throws InitializationError {
    super(klass);
  }

  @Override
  protected Statement methodInvoker(FrameworkMethod method, Object test) {
    if (method.getReturnType() == Void.TYPE) {
      return super.methodInvoker(method, test);
    }
    return new PromiseStatement(method, test);
  }

  // Needs to be overridden to allow for non void return type
  @Override
  protected void validatePublicVoidNoArgMethods(
      Class<? extends Annotation> annotation, boolean isStatic, List<Throwable> errors) {

    if (!annotation.equals(Test.class)) {
      super.validatePublicVoidNoArgMethods(annotation, isStatic, errors);
      return;
    }

    List<FrameworkMethod> methods = getTestClass().getAnnotatedMethods(annotation);

    for (FrameworkMethod eachTestMethod : methods) {
      validateTestMethod(isStatic, errors, eachTestMethod);
    }
  }

  private void validateTestMethod(
      boolean isStatic, List<Throwable> errors, FrameworkMethod testMethod) {
    checkArgument(testMethod.getAnnotation(Test.class) != null);
    Test testAnnotation = testMethod.getAnnotation(Test.class);
    Class<?> returnType = testMethod.getReturnType();

    // taken from FrameworkMethod.validatePublicVoid
    if (testMethod.isStatic() != isStatic) {
      String state = isStatic ? "should" : "should not";
      errors.add(makeError("Method %s() " + state + " be static", testMethod));
    }
    if (!testMethod.isPublic()) {
      errors.add(makeError("Method %s() should be public", testMethod));
    }

    if (returnType == Void.TYPE) {
      return;
    }

    try {
      getPromiseType(returnType);
    } catch (InvalidTypeException e) {
      errors.add(makeError(e.getMessage()));
      return;
    }

    // Make sure we have a value greater than zero for test timeout
    if (testAnnotation.timeout() <= 0) {
      errors.add(makeError(ErrorMessage.ASYNC_NO_TIMEOUT.format(testMethod.getMethod().getName())));
    }

    if (!testAnnotation.expected().equals(Test.None.class)) {
      errors.add(
          makeError(
              ErrorMessage.ASYNC_HAS_EXPECTED_EXCEPTION.format(testMethod.getMethod().getName())));
    }
  }

  private static Exception makeError(String message) {
    return new Exception(message);
  }

  private static Exception makeError(String message, FrameworkMethod eachTestMethod) {
    return new Exception(String.format(message, eachTestMethod.getMethod().getName()));
  }

  private static PromiseType getPromiseType(Class<?> shouldBePromise) throws InvalidTypeException {

    List<Method> methods =
        Arrays.stream(shouldBePromise.getMethods())
            .filter(m -> m.getName().equals("then"))
            .filter(m -> m.getParameterCount() == 2)
            .collect(Collectors.toList());

    if (methods.isEmpty()) {
      throw new InvalidTypeException(
          ErrorMessage.NO_THEN_METHOD.format(shouldBePromise.getCanonicalName()));
    }

    if (methods.size() > 1) {
      throw new InvalidTypeException(
          ErrorMessage.MULTIPLE_THEN_METHOD.format(shouldBePromise.getCanonicalName()));
    }

    Method thenMethod = methods.get(0);
    thenMethod.setAccessible(true);
    Class<?> successCallbackType = getCallbackType(shouldBePromise, thenMethod.getParameters()[0]);
    Class<?> errorCallbackType = getCallbackType(shouldBePromise, thenMethod.getParameters()[1]);

    return new PromiseType(thenMethod, successCallbackType, errorCallbackType);
  }

  private static Class<?> getCallbackType(Class<?> promiseLike, Parameter parameter)
      throws InvalidTypeException {
    if (!isValidCallbackType(parameter.getType())) {
      throw new InvalidTypeException(
          ErrorMessage.INVALID_CALLBACK_PARAMETER.format(
              promiseLike.getCanonicalName(), parameter.getName()));
    }
    return parameter.getType();
  }

  private static boolean isValidCallbackType(Class<?> type) {
    // type needs to be an interface
    if (!type.isInterface()) {
      return false;
    }

    if (type.getMethods().length != 1) {
      return false;
    }

    Method interfaceMethod = type.getMethods()[0];

    // One parameter for either success or failure
    if (interfaceMethod.getParameterCount() != 1) {
      return false;
    }
    return true;
  }
}
