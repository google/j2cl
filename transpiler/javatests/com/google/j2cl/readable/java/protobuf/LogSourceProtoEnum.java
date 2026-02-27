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
package protobuf;

import com.google.wireless.android.play.playlog.proto.LogSourceEnum.LogSource;

public class LogSourceProtoEnum {
  // This static method should be skipped in the ObjC compat header
  public static void setLogSourceStatus(LogSource source) {}

  // This static method returning LogSource should be skipped in the ObjC compat header
  public static LogSource getDefaultLogSource() {
    return LogSource.UNKNOWN;
  }
}
