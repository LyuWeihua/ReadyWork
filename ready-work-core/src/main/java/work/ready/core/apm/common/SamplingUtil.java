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

import work.ready.core.apm.ApmManager;

import java.util.concurrent.atomic.AtomicLong;

public class SamplingUtil {
    private static AtomicLong total = new AtomicLong(0);
    private static AtomicLong currNum = new AtomicLong(0);

    public static long incrTotal() {
        return total.incrementAndGet();
    }

    public static long getTotal() {
        return total.get();
    }

    public static long incrCurrNum() {
        return currNum.incrementAndGet();
    }

    public static long getCurrNum() {
        return currNum.get();
    }

    public static boolean YES() {
        return isCollect();
    }

    public static boolean NO() {
        return !isCollect();
    }

    private static boolean isCollect() {
        if (ApmManager.getConfig().getRate() <= 0) {
            TraceContext.setTag(ApmConst.VAL_N);
            return false;
        }
        if (ApmManager.getConfig().getRate() >= ApmConst.MAX_SAMPLING_RATE) {
            TraceContext.setTag(ApmConst.VAL_Y);
            return true;
        }
        String cTag = TraceContext.getTag();
        if (ApmConst.VAL_Y.equals(cTag)) {
            return true;
        } else if (ApmConst.VAL_N.equals(cTag)) {
            return false;
        } else if (getCurrNum() == 0) {
            
            incrCurrNum();
            incrTotal();
            TraceContext.setTag(ApmConst.VAL_Y);
            return true;
        }
        long tmpTotal = incrTotal();
        long tmpCurrNum = getCurrNum() + 1;
        Double rate = tmpCurrNum * 1.0 / tmpTotal * ApmConst.MAX_SAMPLING_RATE;
        if (rate.intValue() > ApmManager.getConfig().getRate()) {
            TraceContext.setTag(ApmConst.VAL_N);
            return false;
        }
        incrCurrNum();
        TraceContext.setTag(ApmConst.VAL_Y);
        return true;
    }
}
