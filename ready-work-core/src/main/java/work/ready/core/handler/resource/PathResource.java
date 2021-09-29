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
package work.ready.core.handler.resource;

public class PathResource {
    private String path = "/";
    private String basePath = "";
    private boolean prefix = false;
    private int transferMinSize = 1024; 
    private boolean directoryListingEnabled = true;

    public PathResource() {
    }

    public String getPath() {
        return path;
    }

    public PathResource setPath(String path) {
        this.path = path;
        return this;
    }

    public String getBasePath() {
        return basePath;
    }

    public PathResource setBasePath(String basePath) {
        this.basePath = basePath;
        return this;
    }

    public boolean isPrefix() {
        return prefix;
    }

    public PathResource setPrefix(boolean prefix) {
        this.prefix = prefix;
        return this;
    }

    public int getTransferMinSize() {
        return transferMinSize;
    }

    public PathResource setTransferMinSize(int transferMinSize) {
        this.transferMinSize = transferMinSize;
        return this;
    }

    public boolean isDirectoryListingEnabled() {
        return directoryListingEnabled;
    }

    public PathResource setDirectoryListingEnabled(boolean directoryListingEnabled) {
        this.directoryListingEnabled = directoryListingEnabled;
        return this;
    }
}
