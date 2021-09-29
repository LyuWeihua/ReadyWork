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

package work.ready.core.component.cache.FileCache;

import work.ready.core.component.serializer.KryoSerializer;
import work.ready.core.component.serializer.Serializer;

import java.io.IOException;
import java.io.Serializable;

public class FileCache {

    private static FileManager fileManager;
    private static Serializer serialize = KryoSerializer.instance;
    private static String defaultCacheName = FileManager.cacheName;

    private static class LazyHolder{
        static final FileCache instance = new FileCache();
    }

    public static FileCache getInstance(){
        return LazyHolder.instance;
    }

    private FileCache(){
        fileManager = new FileManager();
        fileManager.init();
    }

    public void add(String cacheName, Serializable key, Object value) {
        if(cacheName!=null&&key!=null&&value!=null){
            try {
                byte[] data = serialize.serialize(value);
                fileManager.add(cacheName, key, data);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public void add( Serializable key, Object value) {
        add(defaultCacheName, key, value);
    }

    public Object get(String cacheName, Serializable key) {
        if(cacheName!=null&&key!=null)
        {
            try {
                byte[] data = fileManager.get(cacheName, key);
                return serialize.deserialize(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public Object get( Serializable key) {
        return get(defaultCacheName, key);
    }

    public void remove(String cacheName, Serializable key) {
        if(cacheName!=null&&key!=null)
        {
            fileManager.remove(cacheName, key);
        }
    }

    public void remove( Serializable key) {
        remove(defaultCacheName, key);
    }

    public void clear(String cacheName) {
        if(cacheName!=null)
        {
            try {
                fileManager.clear(cacheName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void clear() {
        clear(defaultCacheName);
    }

    public void destroy() {
        try {
            fileManager.destroy();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
