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

package work.ready.core.handler.request;

import io.undertow.server.handlers.form.FormData;

import java.io.File;
import java.nio.file.Path;

public final class UploadFile {
    public final FormData.FileItem fileItem;
    public final Path file;
    public final String fileName;
    public final String originalFileName;
    public final String contentType;

    public UploadFile(FormData.FileItem fileItem, String fileName, String contentType) {
        this.fileItem = fileItem;
        this.file = fileItem.getFile();
        if(file != null){
            this.fileName = file.getFileName().toString();
        } else {
            this.fileName = null;
        }
        this.originalFileName = fileName;
        this.contentType = contentType;
    }
}
