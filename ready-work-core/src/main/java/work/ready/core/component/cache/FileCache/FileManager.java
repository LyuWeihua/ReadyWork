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

import work.ready.core.server.Ready;
import work.ready.core.tools.FileUtil;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

public class FileManager {

    static final String cacheName = "default";
    private File cacheDirectory;

    public void init(){
        String cachePath = Ready.path("/cache").toString();
        cacheDirectory = new File(cachePath);
        if(!cacheDirectory.isDirectory()){
            if(!cacheDirectory.mkdirs()){
                throw new RuntimeException("Cannot create path for file cache.");
            }
        }
    }

    public void destroy() throws IOException
    {
        if(cacheDirectory.isDirectory()) FileUtil.delete(cacheDirectory);
    }

    public void add(String cacheName, Serializable key, byte[] data) throws IOException {
        File file = new File(cacheDirectory, cacheName + File.separator + key.toString());
        FileUtil.writeBytes(file, data);
    }

    public void add(Serializable key, byte[] data) throws IOException {
        add(cacheName, key, data);
    }

    public byte[] get(String cacheName, Serializable key) throws IOException {
        File file = new File(cacheDirectory, cacheName + File.separator + key.toString());
        if(!file.isFile())return null;
        return FileUtil.readStream(file).toByteArray();
    }

    public byte[] get( Serializable key) throws IOException {
        return get(cacheName, key);
    }

    public void remove(String cacheName, Serializable key) {
        File file = new File(cacheDirectory, File.separator + key.toString());
        if(file.isFile()) file.delete();
    }

    public void remove( Serializable key) {
        remove(cacheName, key);
    }

    public void clear(String cacheName) throws IOException
    {
        File file = new File(cacheDirectory, cacheName);
        if(file.isDirectory()) FileUtil.delete(cacheDirectory);
    }

    public void clear() throws IOException
    {
        clear(cacheName);
    }

}
