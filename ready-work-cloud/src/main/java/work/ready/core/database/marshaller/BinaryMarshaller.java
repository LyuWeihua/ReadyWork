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

import org.apache.ignite.binary.BinaryObjectException;
import org.apache.ignite.internal.binary.BinaryContext;
import org.apache.ignite.internal.binary.GridBinaryMarshaller;
import org.apache.ignite.internal.binary.streams.BinaryInputStream;

public class BinaryMarshaller extends GridBinaryMarshaller {

    public BinaryMarshaller(BinaryContext ctx) {
        super(ctx);
    }

    @Override
    public <T> T unmarshal(byte[] bytes, ClassLoader clsLdr) throws BinaryObjectException {
        return super.unmarshal(bytes, clsLdr);
    }

    @Override
    public <T> T unmarshal(BinaryInputStream in) throws BinaryObjectException {
        return super.unmarshal(in);
    }

    @Override
    public <T> T deserialize(byte[] arr, ClassLoader ldr) throws BinaryObjectException {
        return super.deserialize(arr, ldr);
    }

}
