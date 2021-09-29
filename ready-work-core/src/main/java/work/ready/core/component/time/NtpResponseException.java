/**
 *
 * Original work Copyright (C) 2008 The Android Open Source Project
 * Modified work Copyright (c) 2020 WeiHua Lyu [ready.work]
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

package work.ready.core.component.time;

import java.io.IOException;
import java.util.Locale;

public class NtpResponseException
        extends IOException {

    public final String property;
    public final float expectedValue;
    public final float actualValue;

    NtpResponseException(String detailMessage) {
        super(detailMessage);

        this.property = "na";
        this.expectedValue = 0F;
        this.actualValue = 0F;
    }

    NtpResponseException(String message,
                         String property,
                         float actualValue,
                         float expectedValue) {

        super(String.format(Locale.getDefault(),
                message,
                property,
                actualValue,
                expectedValue));

        this.property = property;
        this.actualValue = actualValue;
        this.expectedValue = expectedValue;
    }
}
