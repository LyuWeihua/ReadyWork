/**
 *
 * Original work copyright dyagent
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

package work.ready.core.aop.transformer.enhance;

import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterDescription.InDefinedShape;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.matcher.ElementMatcher;

import java.util.Iterator;

public class ParametersMatcher implements ElementMatcher<Iterable<? extends ParameterDescription>> {

    private ParameterList<InDefinedShape> otherPds;

    public ParametersMatcher(ParameterList<InDefinedShape> parameterDescriptions) {
        if (parameterDescriptions == null) {
            throw new IllegalArgumentException("parameter descriptions can't be null.");
        }
        this.otherPds = parameterDescriptions;
    }

    public boolean matches(Iterable<? extends ParameterDescription> thisPds) {
        boolean matched = true;
        Iterator<? extends ParameterDescription> thisPdsItr = thisPds.iterator();
        Iterator<InDefinedShape> otherPdsItr = otherPds.iterator();

        while (thisPdsItr.hasNext() && otherPdsItr.hasNext()) {
            ParameterDescription thisPd = thisPdsItr.next();
            ParameterDescription otherPd = otherPdsItr.next();
            if (!thisPd.equals(otherPd)) {
                matched = false;
            }
        }
        if (matched && (thisPdsItr.hasNext() || otherPdsItr.hasNext())) {
            matched = false;
        }
        return matched;
    }
}
