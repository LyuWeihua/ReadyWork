/**
 *
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

package work.ready.cloud;

public class Version extends work.ready.core.module.Version {

    public static final int ES_V_7_10_0_ID = 7_10_00_99;

    public static final int IG_V_2_11_0_ID = 2_11_00_99;

    public static final Version V_0_6_6 = new Version(IG_V_2_11_0_ID, ES_V_7_10_0_ID, 1);

    public static final Version CURRENT = V_0_6_6;

    public static Version unknown() {
        return new Version(0, 0, 0);
    }

    private int esId;
    private int igId;
    public Version(int igId, int esId, int id) {
        super(id, "210501", "v0.66", "work.ready", "ready-work-cloud", "WeiHua Lyu", "ready work framework, http://ready.work");
        this.igId = igId;
        this.esId = esId;
    }

}
