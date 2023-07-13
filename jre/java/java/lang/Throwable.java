/*
 * Copyright 2008 Google Inc.
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
package java.lang;

import static javaemul.internal.InternalPreconditions.checkCriticalArgument;
import static javaemul.internal.InternalPreconditions.checkNotNull;
import static javaemul.internal.InternalPreconditions.checkState;

import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import javaemul.internal.ThrowableUtils;
import javaemul.internal.ThrowableUtils.NativeError;
import javaemul.internal.ThrowableUtils.NativeTypeError;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsNonNull;
import jsinterop.annotations.JsProperty;

/**
 * See <a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/java/lang/Throwable.html">the
 * official Java API doc</a> for details.
 */
public class Throwable implements Serializable {

  private String detailMessage;
  private Throwable cause;
  private ArrayList<Throwable> suppressedExceptions;
  private StackTraceElement[] stackTrace = new StackTraceElement[0];
  private boolean disableSuppression;
  private boolean disableStackTrace;

  @JsProperty
  private Object backingJsObject;

  public Throwable() {
    fillInStackTrace();
  }

  public Throwable(String message) {
    this.detailMessage = message;
    fillInStackTrace();
  }

  public Throwable(String message, Throwable cause) {
    this.cause = cause;
    this.detailMessage = message;
    fillInStackTrace();
  }

  public Throwable(Throwable cause) {
    this.cause = cause;
    if (cause != null) {
      this.detailMessage = cause.toString();
    }
    fillInStackTrace();
  }

  /**
   * Constructor that allows subclasses disabling exception suppression and stack traces.
   * Those features should only be disabled in very specific cases.
   */
  protected Throwable(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    this.cause = cause;
    this.detailMessage = message;
    this.disableStackTrace = !writableStackTrace;
    this.disableSuppression = !enableSuppression;
    if (writableStackTrace) {
      fillInStackTrace();
    }
  }

  Throwable(Object backingJsObject) {
    this(String.valueOf(backingJsObject));
  }

  // Called by transpiler. Do not remove!
  void privateInitError(Object error) {
    setBackingJsObject(error);
  }

  public Object getBackingJsObject() {
    return backingJsObject;
  }

  private void setBackingJsObject(Object backingJsObject) {
    this.backingJsObject = backingJsObject;
    ThrowableUtils.setJavaThrowable(backingJsObject, this);
  }

  /** Call to add an exception that was suppressed. Used by try-with-resources. */
  public final void addSuppressed(Throwable exception) {
    checkNotNull(exception, "Cannot suppress a null exception.");
    checkCriticalArgument(exception != this, "Exception can not suppress itself.");

    if (disableSuppression) {
      return;
    }

    if (suppressedExceptions == null) {
      suppressedExceptions = new ArrayList<>();
    }
    suppressedExceptions.add(exception);
  }

  /**
   * Populates the stack trace information for the Throwable.
   *
   * @return this
   */
  public Throwable fillInStackTrace() {
    if (!disableStackTrace) {
      // Note that when this called from ctor, transpiler hasn't initialized backingJsObject yet.
      if (backingJsObject instanceof NativeError) {
        // The stack property on Error is lazily evaluated in Chrome, so it is better use
        // captureStackTrace if available.
        if (NativeError.hasCaptureStackTraceProperty) {
          NativeError.captureStackTrace((NativeError) backingJsObject);
        } else {
          ((NativeError) backingJsObject).stack = new NativeError().stack;
        }
      }

      // Invalidate the cached trace
      this.stackTrace = null;
    }
    return this;
  }

  @JsMethod
  public Throwable getCause() {
    return cause;
  }

  public String getLocalizedMessage() {
    return getMessage();
  }

  public String getMessage() {
    return detailMessage;
  }

  /** Returns the stack trace for the Throwable if it is available. */
  public StackTraceElement[] getStackTrace() {
    if (stackTrace == null) {
      stackTrace = constructJavaStackTrace();
    }
    return stackTrace;
  }

  private StackTraceElement[] constructJavaStackTrace() {
    StackTraceElement[] stackTraceElements = new StackTraceElement[0]; // Will auto-grow...
    Object e = this.backingJsObject;
    if (e instanceof NativeError) {
      NativeError error = ((NativeError) e);
      if (error.stack != null) {
        String[] splitStack = error.stack.split("\n");
        for (int i = 0; i < splitStack.length; i++) {
          stackTraceElements[i] = new StackTraceElement("", splitStack[i], null, -1);
        }
      }
    }
    return stackTraceElements;
  }

  /** Returns the array of Exception that this one suppressedExceptions. */
  @JsMethod
  public final Throwable[] getSuppressed() {
    return suppressedExceptions == null
        ? new Throwable[0]
        : suppressedExceptions.toArray(new Throwable[0]);
  }

  public Throwable initCause(Throwable cause) {
    checkState(this.cause == null, "Can't overwrite cause");
    checkCriticalArgument(cause != this, "Self-causation not permitted");
    this.cause = cause;
    return this;
  }

  public void printStackTrace() {
    printStackTrace(System.err);
  }

  public void printStackTrace(PrintStream out) {
    printStackTraceImpl(out, "", "");
  }

  private void printStackTraceImpl(PrintStream out, String prefix, String ident) {
    out.println(ident + prefix + this);
    printStackTraceItems(out, ident);

    for (Throwable t : getSuppressed()) {
      t.printStackTraceImpl(out, "Suppressed: ", "\t" + ident);
    }

    Throwable theCause = getCause();
    if (theCause != null) {
      theCause.printStackTraceImpl(out, "Caused by: ", ident);
    }
  }

  private void printStackTraceItems(PrintStream out, String ident) {
    for (StackTraceElement element : getStackTrace()) {
      out.println(ident + "\tat " + element);
    }
  }

  public void setStackTrace(StackTraceElement[] stackTrace) {
    int length = stackTrace.length;
    StackTraceElement[] copy = new StackTraceElement[length];
    for (int i = 0; i < length; ++i) {
      copy[i] = checkNotNull(stackTrace[i]);
    }
    this.stackTrace = copy;
  }

  @Override
  public String toString() {
    String className = getClass().getName();
    String message = getLocalizedMessage();
    return message == null ? className : className + ": " + message;
  }

  @JsMethod
  public static @JsNonNull Throwable of(Object e) {
    // TODO(b/260631095): Clean up this part. Consider a ThrowableUtils.throwableOf method?
    // If the JS error is already mapped to a Java Exception, use it.
    if (e != null) {
      Throwable throwable = ThrowableUtils.getJavaThrowable(e);
      if (throwable != null) {
        return throwable;
      }
    }

    // If the JS error is being seen for the first time, map it best corresponding Java exception.
    return e instanceof NativeTypeError ? new NullPointerException(e) : new JsException(e);
  }
}
