package com.google.j2cl.transpiler.integration.trywithresource;

import java.util.List;

/**
 * A helper class that emulates a resource that can be told to fail at the initialization stage
 * or close stage.
 */
class FailableResource implements AutoCloseable {
  public static enum FailureMode {
    None,
    Open,
    Close
  }

  List<String> orderLog;
  String name;
  FailureMode failureMode;

  public FailableResource(String name, List<String> orderLog, FailureMode failureMode)
      throws Exception {
    this.orderLog = orderLog;
    this.name = name;
    this.failureMode = failureMode;
    open();
  }

  public void open() throws Exception {
    if (failureMode == FailureMode.Open) {
      orderLog.add(this.name + " throw open");
      throw new Exception("open");
    }
    orderLog.add(this.name + " open");
  }

  @Override
  public void close() throws Exception {
    if (failureMode == FailureMode.Close) {
      orderLog.add(this.name + " throw close");
      throw new Exception("close");
    }
    orderLog.add(this.name + " close");
  }
}
