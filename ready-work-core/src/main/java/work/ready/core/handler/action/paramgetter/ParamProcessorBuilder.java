/**
 * Copyright (c) 2011-2019, 玛雅牛 (myaniu AT gmail.com).
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

import work.ready.core.database.Model;
import work.ready.core.handler.Controller;
import work.ready.core.handler.request.UploadFile;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

public class ParamProcessorBuilder {
	private static final Log logger = LogFactory.getLog(ParamProcessorBuilder.class);

	private Map<String, Holder> typeMap = new HashMap<String, Holder>();

	private static class LazyHolder{
		static final ParamProcessorBuilder instance = new ParamProcessorBuilder();
	}

	public static ParamProcessorBuilder getInstance(){
		return LazyHolder.instance;
	}

	private ParamProcessorBuilder() {
		register(short.class, ShortGetter.class, "0");
		register(int.class, IntegerGetter.class, "0");
		register(long.class, LongGetter.class, "0");
		register(float.class, FloatGetter.class, "0");
		register(double.class, DoubleGetter.class, "0");
		register(boolean.class, BooleanGetter.class, "false");
		register(Short.class, ShortGetter.class, null);
		register(Integer.class, IntegerGetter.class, null);
		register(Long.class, LongGetter.class, null);
		register(Float.class, FloatGetter.class, null);
		register(Double.class, DoubleGetter.class, null);
		register(Boolean.class, BooleanGetter.class, null);
		register(String.class, StringGetter.class, null);
		register(java.util.Date.class, DateGetter.class, null);
		register(java.sql.Date.class, SqlDateGetter.class, null);
		register(java.sql.Time.class, TimeGetter.class, null);
		register(java.sql.Timestamp.class, TimestampGetter.class, null);
		register(java.math.BigDecimal.class, BigDecimalGetter.class, null);
		register(java.math.BigInteger.class, BigIntegerGetter.class, null);
		register(java.io.File.class, FileGetter.class, null);
		register(UploadFile.class, UploadFileGetter.class, null);
		register(String[].class, StringArrayGetter.class, null);
		register(Integer[].class, IntegerArrayGetter.class, null);
		register(Long[].class, LongArrayGetter.class, null);
		register(work.ready.core.handler.action.paramgetter.RawData.class, RawDataGetter.class, null);

	}

	public <T> void register(Class<T> typeClass, Class<? extends ParamGetter<T>> pgClass, String defaultValue){
		this.typeMap.put(typeClass.getName(), new Holder(pgClass, defaultValue));
	}

	public ParamProcessor build(Class<? extends Controller> controllerClass, Method method) {
		final int paramCount = method.getParameterCount();

		if (paramCount == 0) {
			return NullParamProcessor.getInstance();
		}

		ParamProcessor ret = new ParamProcessor(paramCount);

		Parameter[] params = method.getParameters();
		for (int i=0; i<paramCount; i++) {
			IParamGetter<?> pg = createParamGetter(controllerClass, method, params[i]);
			ret.addParamGetter(i, pg);
		}

		return ret;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private IParamGetter<?> createParamGetter(Class<? extends Controller> controllerClass, Method method,
											  Parameter p) {
		if(!p.isNamePresent()) {
			logger.warn("You should config compiler argument \"-parameters\" for parameter injection of action : " +
					controllerClass.getName() + "." + method.getName() + "(...) \n");
		}
		String parameterName = p.getName();
		String defaultValue = null;
		Class<?> typeClass = p.getType();
		Param param = p.getAnnotation(Param.class);
		if (param != null) {
			
			if (!Param.NULL_VALUE.equals(param.value())) {
				parameterName = param.value().trim();
			}

			if (!Param.NULL_VALUE.equals(param.defaultValue())) {
				defaultValue = param.defaultValue();
			}

		}
		Holder holder = typeMap.get(typeClass.getName());
		if (holder != null) {
			if (null == defaultValue) {
				defaultValue = holder.getDefaultValue();
			}
			try {
				return holder.born(parameterName, defaultValue);
			} catch (Exception e) {
				throw new RuntimeException(e.getMessage(), e);
			}
		}
		
		if(Enum.class.isAssignableFrom(typeClass)){
			return new EnumGetter(typeClass,parameterName,defaultValue);
		} else if (Model.class.isAssignableFrom(typeClass)) {
			return new ModelGetter(typeClass, parameterName);
		} else {
			return new BeanGetter(typeClass, parameterName);
		}
	}

	private static class Holder {
		private final String defaultValue;
		private final Class<? extends ParamGetter<?>> clazz;

		Holder(Class<? extends ParamGetter<?>> clazz, String defaultValue) {
			this.clazz = clazz;
			this.defaultValue = defaultValue;
		}
		final String getDefaultValue() {
			return defaultValue;
		}
		ParamGetter<?> born(String parameterName, String defaultValue) throws Exception {
			Constructor<? extends ParamGetter<?>> con = clazz.getConstructor(String.class, String.class);
			return con.newInstance(parameterName, defaultValue);
		}
	}
}
