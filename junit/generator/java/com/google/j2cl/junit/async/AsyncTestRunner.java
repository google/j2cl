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

import static com.google.common.base.Preconditions.checkState;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.google.common.reflect.Reflection;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestTimedOutException;

/**
 * A test runner that allows for asynchronous test using LitenableFuture or a structural Promise.
 *
 * <p>See {@link PROMISE_LIKE} for the expected requirements of structural promise. Note that this
 * mimics the J2CL version the type but has less requirements since they don't make sense for the
 * JVM version of the promise-like object.
 */
public class AsyncTestRunner extends BlockJUnit4ClassRunner {

  private static final String PROMISE_LIKE =
      "A promise-like type is a type that is either 'ListenableFuture' or a type with a single "
          + "'then' method that has 'success' and 'failure' callback parameters.";

  public enum ErrorMessage {
    ASYNC_HAS_EXPECTED_EXCEPTION(
        "Method %s has expectedException attribute but returns a promise-like type."),
    ASYNC_HAS_NO_TIMEOUT("Method %s is missing timeout but returns a promise-like type."),
    TEST_HAS_TIMEOUT_ANNOTATION(
        "Method %s cannot have Timeout annotation. @Timeout is only for lifecycle methods. "
            + "Test methods should use @Test(timeout=x) instead."),
    NO_THEN_METHOD(
        "Type %s is not a promise-like type. It's missing a 'then' method with two parameters. "
            + PROMISE_LIKE),
    MULTIPLE_THEN_METHOD(
        "Type %s is not a promise-like type. It has multiple 'then' methods with two parameters. "
            + PROMISE_LIKE),
    INVALID_CALLBACK_PARAMETER(
        "Type '%s' is not a promise-like type. The argument '%s' on the 'then' is not a simple "
            + "callback interface. "
            + PROMISE_LIKE);

    private final String formattedMsg;

    ErrorMessage(String formattedMsg) {
      this.formattedMsg = formattedMsg;
    }

    public String format(Object... args) {
      return String.format(formattedMsg, args);
    }
  }

  private static class ListenableFutureStatement extends Statement {
    private final FrameworkMethod method;
    private final Object test;

    public ListenableFutureStatement(FrameworkMethod method, Object test) {
      this.method = method;
      this.test = test;
    }

    @Override
    public void evaluate() throws Throwable {
      long timeout = getTimeout(method);
      ListenableFuture<?> future = (ListenableFuture) method.invokeExplosively(test);
      try {
        future.get(timeout, MILLISECONDS);
      } catch (TimeoutException e) {
        TestTimedOutException timedOutException = new TestTimedOutException(timeout, MILLISECONDS);
        timedOutException.setStackTrace(e.getStackTrace());
        throw timedOutException;
      } catch (ExecutionException e) {
        throw e.getCause();
      }
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
      long timeout = getTimeout(method);
      Object promiseLike = method.invokeExplosively(test);
      registerCallbacks(promiseLike);
      try {
        future.get(timeout, MILLISECONDS);
      } catch (TimeoutException e) {
        TestTimedOutException timedOutException = new TestTimedOutException(timeout, MILLISECONDS);
        timedOutException.setStackTrace(e.getStackTrace());
        throw timedOutException;
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

  public AsyncTestRunner(Class<?> klass) throws InitializationError {
    super(klass);
  }

  @Override
  protected Statement methodInvoker(FrameworkMethod method, Object test) {
    if (method.getReturnType() == Void.TYPE) {
      return super.methodInvoker(method, test);
    }
    if (method.getReturnType() == ListenableFuture.class) {
      return new ListenableFutureStatement(method, test);
    }
    return new PromiseStatement(method, test);
  }

  @Override
  protected Statement withPotentialTimeout(FrameworkMethod method, Object test, Statement next) {
    if (method.getReturnType() == Void.TYPE) {
      return super.withPotentialTimeout(method, test, next);
    }
    // Both ListenableFutureStatement and PromiseStatement wrap the future/promiselike test methods
    // in a `Future.get(timeout)`, so we don't need to enforce the timeout here.
    return next;
  }

  @Override
  protected Statement withBefores(FrameworkMethod method, Object target, Statement next) {
    List<FrameworkMethod> befores = getTestClass().getAnnotatedMethods(Before.class);
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        for (FrameworkMethod m : befores) {
          methodInvoker(m, target).evaluate();
        }
        next.evaluate();
      }
    };
  }

  @Override
  protected Statement withAfters(FrameworkMethod method, Object target, Statement next) {
    List<FrameworkMethod> afters = getTestClass().getAnnotatedMethods(After.class);
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        ArrayList<Throwable> errors = new ArrayList<>();
        try {
          next.evaluate();
        } catch (Throwable e) {
          errors.add(e);
        } finally {
          for (FrameworkMethod m : afters) {
            try {
              methodInvoker(m, target).evaluate();
            } catch (Throwable e) {
              errors.add(e);
            }
          }
        }
        MultipleFailureException.assertEmpty(errors);
      }
    };
  }

  // Needs to be overridden to allow for non void return type
  @Override
  protected void validatePublicVoidNoArgMethods(
      Class<? extends Annotation> annotation, boolean isStatic, List<Throwable> errors) {
    List<FrameworkMethod> methods = getTestClass().getAnnotatedMethods(annotation);

    for (FrameworkMethod eachTestMethod : methods) {
      validateTestMethod(isStatic, errors, eachTestMethod);
    }
  }

  private void validateTestMethod(
      boolean isStatic, List<Throwable> errors, FrameworkMethod testMethod) {
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

    if (returnType != ListenableFuture.class) {
      try {
        getPromiseType(returnType);
      } catch (InvalidTypeException e) {
        errors.add(makeError(e.getMessage()));
        return;
      }
    }

    Test testAnnotation = testMethod.getAnnotation(Test.class);
    // Make sure we have a value greater than zero for test timeout
    if (getTimeout(testMethod) <= 0) {
      errors.add(
          makeError(ErrorMessage.ASYNC_HAS_NO_TIMEOUT.format(testMethod.getMethod().getName())));
    }

    if (testAnnotation != null && !testAnnotation.expected().equals(Test.None.class)) {
      errors.add(
          makeError(
              ErrorMessage.ASYNC_HAS_EXPECTED_EXCEPTION.format(testMethod.getMethod().getName())));
    }
  }

  private static long getTimeout(FrameworkMethod testMethod) {
    Test testAnnotation = testMethod.getAnnotation(Test.class);
    Timeout timeoutAnnotation = testMethod.getAnnotation(Timeout.class);
    return testAnnotation != null
        ? testAnnotation.timeout()
        : timeoutAnnotation != null ? timeoutAnnotation.value() : 0;
  }

  private static Exception makeError(String message) {
    return new Exception(message);
  }

  private static Exception makeError(String message, FrameworkMethod eachTestMethod) {
    return new Exception(String.format(message, eachTestMethod.getMethod().getName()));
  }

  // TODO(b/140131081): Yet another different implementation of what means to be a promise.
  //  This one is only used for running Async tests on the JVM and has more relaxed requirements.
  //  IMHO, since this is only for J2CL tests it should have the same error paths and reject
  //  the test methods in the same cases as the validator, for example this one accepts 'then'
  //  methods that are not JsMethod.
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
