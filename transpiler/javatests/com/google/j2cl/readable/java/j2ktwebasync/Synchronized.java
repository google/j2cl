/*
 * Copyright 2026 Google Inc.
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
package j2ktwebasync;

import com.google.calendar.v2a.shared.async.AsyncLock;
import com.google.calendar.v2a.shared.async.AsyncLockable;
import com.google.calendar.v2a.shared.async.WebAsync;

/**
 * Mirrors {@code
 * javatests/com/google/apps/xplat/tools/j2j/plugins/webasync/testdata/Synchronized.java} so the J2J
 * and J2KT outputs can be compared.
 */
public class Synchronized {

  public Object lock = new Object();
  public AsyncLock asyncLock = new AsyncLock();

  @WebAsync
  public void doNothing() {
    synchronized (lock) {
      call();
    }
  }

  @WebAsync
  public void doSomething() {
    synchronized (asyncLock) {
      asyncCall();
    }
  }

  @WebAsync
  public synchronized void doNothing2() {
    call();
  }

  @WebAsync
  public synchronized void nestedSynchronization() {
    synchronized (asyncLock) {
      call();
    }

    synchronized (lock) {
      call();
    }
  }

  public static void call() {}

  @WebAsync
  public static void asyncCall() {}

  public static class AsyncSynchronized extends AsyncLockable {
    @WebAsync
    public synchronized void doSomething() {
      asyncCall();
    }

    @WebAsync
    public synchronized void doNothing() {
      call();
    }

    @WebAsync
    public synchronized void nestedSynchronization() {
      synchronized (this) {
        asyncCall();
      }

      synchronized (this) {
        call();
      }
    }
  }
}
