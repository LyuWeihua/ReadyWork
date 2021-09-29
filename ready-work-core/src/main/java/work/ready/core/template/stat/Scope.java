/**
 * Copyright (c) 2011-2019, James Zhan 詹波 (jfinal@126.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package work.ready.core.template.stat;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({"rawtypes", "unchecked"})
public class Scope {

	private final Scope parent;
	private final Ctrl ctrl;
	private Map data;
	private Map<String, Object> sharedObjectMap;

	public Scope(Map data, Map<String, Object> sharedObjectMap) {
		this.parent = null;
		this.ctrl = new Ctrl();
		this.data = data;
		this.sharedObjectMap = sharedObjectMap;
	}

	public Scope(Scope parent) {
		if (parent == null) {
			throw new IllegalArgumentException("parent can not be null.");
		}
		this.parent = parent;
		this.ctrl = parent.ctrl;
		this.data = null;
		this.sharedObjectMap = parent.sharedObjectMap;
	}

	public Ctrl getCtrl() {
		return ctrl;
	}

	public void set(Object key, Object value) {
		for (Scope cur=this; true; cur=cur.parent) {

			if (cur.data != null && cur.data.containsKey(key)) {
				cur.data.put(key, value);
				return ;
			}

			if (cur.parent == null) {
				if (cur.data == null) {			
					cur.data = new HashMap();
				}
				cur.data.put(key, value);
				return ;
			}
		}
	}

	public Object get(Object key) {
		Scope cur = this;
		do {

			if (cur.data != null) {
				Object ret = cur.data.get(key);
				if (ret != null) {
					return ret;
				}

				if (cur.data.containsKey(key)) {
					return null;
				}
			}

			cur = cur.parent;
		} while (cur != null);

		return sharedObjectMap != null ? sharedObjectMap.get(key) : null;
	}

	public void remove(Object key) {
		for (Scope cur=this; cur!=null; cur=cur.parent) {
			if (cur.data != null && cur.data.containsKey(key)) {
				cur.data.remove(key);
				return ;
			}
		}
	}

	public void setLocal(Object key, Object value) {
		if (data == null) {
			data = new HashMap();
		}
		data.put(key, value);
	}

	public Object getLocal(Object key) {
		return data != null ? data.get(key) : null;
	}

	public void removeLocal(Object key) {
		if (data != null) {
			data.remove(key);
		}
	}

	public void setGlobal(Object key, Object value) {
		for (Scope cur=this; true; cur=cur.parent) {
			if (cur.parent == null) {
				if (cur.data == null) {
					cur.data = new HashMap();
				}

				cur.data.put(key, value);
				return ;
			}
		}
	}

	public Object getGlobal(Object key) {
		for (Scope cur=this; true; cur=cur.parent) {
			if (cur.parent == null) {
				return cur.data != null ? cur.data.get(key) : null;
			}
		}
	}

	public void removeGlobal(Object key) {
		for (Scope cur=this; true; cur=cur.parent) {
			if (cur.parent == null) {
				if (cur.data != null) {
					cur.data.remove(key);
				}

				return ;
			}
		}
	}

	public Map getMapOfValue(Object key) {
		for (Scope cur=this; cur!=null; cur=cur.parent) {
			if (cur.data != null && cur.data.containsKey(key)) {
				return cur.data;
			}
		}
		return null;
	}

	public Map getData() {
		return data;
	}

	public void setData(Map data) {
		this.data = data;
	}

	public Map getRootData() {
		for (Scope cur=this; true; cur=cur.parent) {
			if (cur.parent == null) {
				return cur.data;
			}
		}
	}

	public void setRootData(Map data) {
		for (Scope cur=this; true; cur=cur.parent) {
			if (cur.parent == null) {
				cur.data = data;
				return ;
			}
		}
	}

	public boolean exists(Object key) {
		for (Scope cur=this; cur!=null; cur=cur.parent) {
			if (cur.data != null && cur.data.containsKey(key)) {
				return true;
			}
		}
		return false;
	}

	public Object getSharedObject(String key) {
		return sharedObjectMap != null ? sharedObjectMap.get(key) : null;
	}
}

