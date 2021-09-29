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

package work.ready.core.aop.transformer.match;

import net.bytebuddy.description.type.TypeDescription;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MultiNameOrMatch implements ClassMatch {

    private Set<String> classNames = new HashSet<>(4);

    public MultiNameOrMatch(String... classNames) {
        this.classNames.addAll(Arrays.asList(classNames));
    }

    public MultiNameOrMatch(String className) {
        this.classNames.add(className);
    }

    public MultiNameOrMatch or(String className) {
        classNames.add(className);
        return this;
    }

    public Set<String> getClassNames() {
        return classNames;
    }

    public static MultiNameOrMatch byName(String className) {
        return new MultiNameOrMatch(className);
    }

    public static MultiNameOrMatch byNames(String... classNames) {
        return new MultiNameOrMatch(classNames);
    }

    @Override
    public boolean isMatch(TypeDescription typeDescription) {
        return classNames.contains(typeDescription.getActualName());
    }
}
