/**
 * Copyright (c) 2011-2017, 玛雅牛 (myaniu AT gmail.com).
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
package work.ready.core.handler.action.paramgetter;

import work.ready.core.handler.Controller;
import work.ready.core.handler.action.Action;
import work.ready.core.handler.request.UploadFile;

import java.io.File;

public class FileGetter extends ParamGetter<File> {

	public FileGetter(String parameterName,String defaultValue) {
		super(parameterName,null);
	}

	@Override
	public File get(Action action, Controller c) {
		String parameterName = this.getParameterName();
		UploadFile uf = null;
		if(parameterName.isEmpty()){
			uf = c.getFiles().get(0);
		}else{
			uf = c.getFile(parameterName);
		}
		if(uf != null){
			return uf.file.toFile();
		}
		return null;
	}

	@Override
	protected File to(String v) {
		return null;
	}

}
