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
package work.ready.core.render.captcha;

import work.ready.core.server.Ready;

import java.io.Serializable;

public class Captcha implements Serializable {

	private static final long serialVersionUID = -2593323370708163022L;

	public static final int liveSeconds = 180;

	private String key;

	private String value;

	private long expireAt;

	public Captcha(String key, String value, int expireTime) {
		if (key == null || value == null) {
			throw new IllegalArgumentException("key and value can not be null");
		}
		this.key = key;
		this.value = value;
		long et = expireTime;
		this.expireAt = et * 1000 + Ready.currentTimeMillis();
	}

	public Captcha(String key, String value) {
		this(key, value, liveSeconds);
	}

	public Captcha() {
	}

	public String getKey() {
		return key;
	}

	public String getValue() {
		return value;
	}

	public long getExpireAt() {
		return expireAt;
	}

	public boolean isExpired() {
		return expireAt < Ready.currentTimeMillis();
	}

	public boolean notExpired() {
		return !isExpired();
	}

	public String toString() {
		return key + " : " + value;
	}
}

