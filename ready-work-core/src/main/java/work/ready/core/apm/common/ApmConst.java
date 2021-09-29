/**
 *
 * Original work copyright bee-apm
 * Copyright (c) 2020 WeiHua Lyu [ready.work]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package work.ready.core.apm.common;

public class ApmConst {
    public static String LINE_SEPARATOR = "\n";
    public static String VAL_Y = "Y";
    public static String VAL_N = "N";
    public static int MAX_SAMPLING_RATE = 10000;

    public final static String PARENT_ID = "X-Parent-Id";
    public final static String TAG = "X-Tag-Id";
    public final static String ID  = "X-Id";
    public final static String SRC_APPLICATION = "X-Src-Application";
    public final static String SRC_INSTANCE = "X-Src-Instance";

    public final static String SRC_APPLICATION_STRING = "srcApplication";
    public final static String SRC_INSTANCE_STRING = "srcInstance";
    public final static String SRC_NO_APPLICATION = "nvl";

}
