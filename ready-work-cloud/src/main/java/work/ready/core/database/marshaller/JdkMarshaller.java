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

package work.ready.core.database.marshaller;

import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.lang.IgnitePredicate;

import java.io.InputStream;

public class JdkMarshaller extends org.apache.ignite.marshaller.jdk.JdkMarshaller {

    public JdkMarshaller() {
        super();
    }

    public JdkMarshaller(IgnitePredicate<String> clsFilter){
        super(clsFilter);
    }

    @Override
    protected <T> T unmarshal0(InputStream in, ClassLoader clsLdr) throws IgniteCheckedException {
        return super.unmarshal0(in, clsLdr);
    }
}
