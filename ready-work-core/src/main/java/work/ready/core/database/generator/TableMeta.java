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

package work.ready.core.database.generator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class TableMeta implements Serializable {

	public String name;					
	public String remarks;				
	public String primaryKey;			
	public List<ColumnMeta> columnMetas = new ArrayList<ColumnMeta>();

	public String baseModelName;		
	public String baseModelContent;

	public String modelName;			
	public String modelContent;

	public String serviceInterfaceContent;
	public String serviceImplContent;

	public int colNameMaxLen = "Field".length();			
	public int colTypeMaxLen = "Type".length();				
	public int colDefaultValueMaxLen = "Default".length();	
}

