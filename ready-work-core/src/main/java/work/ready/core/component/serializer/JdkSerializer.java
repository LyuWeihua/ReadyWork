/**
 *
 * Original work Copyright (c) 2011-2019, James Zhan 詹波 (jfinal@126.com).
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

package work.ready.core.component.serializer;

import redis.clients.jedis.util.SafeEncoder;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class JdkSerializer implements Serializer {
	private static final Log logger = LogFactory.getLog(JdkSerializer.class);

	public static final Serializer instance = new JdkSerializer();

	public byte[] keyToBytes(String key) {
		return SafeEncoder.encode(key);
	}

	public String keyFromBytes(byte[] bytes) {
		return SafeEncoder.encode(bytes);
	}

	public byte[] fieldToBytes(Object field) {
		return serialize(field);
	}

    public Object fieldFromBytes(byte[] bytes) {
    	return deserialize(bytes);
    }

    @Override
	public byte[] serialize(Object value) {
		ObjectOutputStream objectOut = null;
		try {
			ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
			objectOut = new ObjectOutputStream(bytesOut);
			objectOut.writeObject(value);
			objectOut.flush();
			return bytesOut.toByteArray();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		finally {
			if(objectOut != null)
				try {objectOut.close();} catch (Exception e) {logger.error(e,"ObjectOutputStream close exception");}
		}
	}

	@Override
	public Object deserialize(byte[] bytes) {
		if(bytes == null || bytes.length == 0)
			return null;

		ObjectInputStream objectInput = null;
		try {
			ByteArrayInputStream bytesInput = new ByteArrayInputStream(bytes);
			objectInput = new ObjectInputStream(bytesInput);
			return objectInput.readObject();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		finally {
			if (objectInput != null)
				try {objectInput.close();} catch (Exception e) {logger.error(e, "ObjectInputStream close exception");}
		}
	}
}

