/*
 * Copyright 2024 Google Inc.
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
package xplatloggerold;

import com.google.apps.xplat.logging.XLogger;

public class XLoggerCalls {

  static void log() {
    XLogger.getLogger(Object.class).atInfo().log("X");
  }

  static void log(XLogger logger) {
    logger.atInfo().log("X");
  }

  static void isEnabled(XLogger logger) {
    logger.atInfo().isEnabled();
  }
}
