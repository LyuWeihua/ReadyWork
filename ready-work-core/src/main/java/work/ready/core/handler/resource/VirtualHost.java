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

public class VirtualHost {
    private String path;
    private String domain;
    private String basePath;
    private int transferMinSize = 1024;
    private boolean directoryListingEnabled = true;

    public VirtualHost() {
    }

    public String getPath() {
        return path;
    }

    public VirtualHost setPath(String path) {
        this.path = path;
        return this;
    }

    public String getDomain() {
        return domain;
    }

    public VirtualHost setDomain(String domain) {
        this.domain = domain;
        return this;
    }

    public String getBasePath() {
        return basePath;
    }

    public VirtualHost setBasePath(String basePath) {
        this.basePath = basePath;
        return this;
    }

    public int getTransferMinSize() {
        return transferMinSize;
    }

    public VirtualHost setTransferMinSize(int transferMinSize) {
        this.transferMinSize = transferMinSize;
        return this;
    }

    public boolean isDirectoryListingEnabled() {
        return directoryListingEnabled;
    }

    public VirtualHost setDirectoryListingEnabled(boolean directoryListingEnabled) {
        this.directoryListingEnabled = directoryListingEnabled;
        return this;
    }
}
