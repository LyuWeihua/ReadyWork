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
package work.ready.core.handler.validate;

import work.ready.core.aop.Interceptor;
import work.ready.core.aop.Invocation;
import work.ready.core.handler.Controller;
import work.ready.core.tools.StrUtil;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Validator implements Interceptor {

	protected Controller controller;
	protected Invocation invocation;
	protected boolean shortCircuit = false;
	protected boolean invalid = false;
	protected String datePattern = null;

	protected static final String DEFAULT_DATE_PATTERN = "yyyy-MM-dd";
	protected static final String emailAddressPattern = "\\b(^['_A-Za-z0-9-]+(\\.['_A-Za-z0-9-]+)*@([A-Za-z0-9-])+(\\.[A-Za-z0-9-]+)*((\\.[A-Za-z0-9]{2,})|(\\.[A-Za-z0-9]{2,}\\.[A-Za-z0-9]{2,}))$)\\b";

	protected Map<String, String> error = new HashMap<>();

	protected void addError(String errorKey, String errorMessage) {
		invalid = true;

		error.put(errorKey, errorMessage);
		if (shortCircuit) {
			throw new ValidateException();
		}
	}

	protected Map<String, String> getError(){ return error; }

	protected void setShortCircuit(boolean shortCircuit) {
		this.shortCircuit = shortCircuit;
	}

	protected void setDatePattern(String datePattern) {
		this.datePattern = datePattern;
	}

	protected String getDatePattern() {
		return (datePattern != null ? datePattern : DEFAULT_DATE_PATTERN);
	}

	@Override
	final public void intercept(Invocation invocation) throws Throwable {
		Validator validator = null;
		try {

			validator = getClass().getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		validator.controller = invocation.getController();
		validator.invocation = invocation;

		try {
			validator.validate(validator.controller);
		} catch (ValidateException e) {

		}

		if (validator.invalid) {
			validator.controller.setAttr("error", error);
			validator.handleError(validator.controller);
		} else {
			invocation.invoke();
		}
	}

	protected abstract void validate(Controller c);

	protected abstract void handleError(Controller c);

	protected Controller getController() {
		return controller;
	}

	protected String getActionKey() {
		return invocation.getActionKey();
	}

	protected String getControllerKey() {
		return invocation.getControllerKey();
	}

	protected Method getActionMethod() {
		return invocation.getMethod();
	}

	protected String getActionMethodName() {
		return invocation.getMethodName();
	}

	protected String getViewPath() {
		return invocation.getViewPath();
	}

	protected void validateRequired(String field, String errorKey, String errorMessage) {
		String value = controller.getParam(field);
		if (value == null || "".equals(value)) {	
			addError(errorKey, errorMessage);
		}
	}

	protected void validateRequiredPath(String pathKey, String errorKey, String errorMessage) {
		String value = controller.getPathParam(pathKey);
		if (value == null ) {
			addError(errorKey, errorMessage);
		}
	}

	protected void validateRequiredString(String field, String errorKey, String errorMessage) {
		if (StrUtil.isBlank(controller.getParam(field))) {
			addError(errorKey, errorMessage);
		}
	}

	protected void validateRequiredStringPath(String pathKey, String errorKey, String errorMessage) {
		if (StrUtil.isBlank(controller.getPathParam(pathKey))) {
			addError(errorKey, errorMessage);
		}
	}

	protected void validateInteger(String field, int min, int max, String errorKey, String errorMessage) {
		validateIntegerValue(controller.getParam(field), min, max, errorKey, errorMessage);
	}

	protected void validateIntegerPath(String pathKey, int min, int max, String errorKey, String errorMessage) {
		String value = controller.getPathParam(pathKey);
		if (value != null && (value.startsWith("N") || value.startsWith("n"))) {
			value = "-" + value.substring(1);
		}
		validateIntegerValue(value, min, max, errorKey, errorMessage);
	}

	private void validateIntegerValue(String value, int min, int max, String errorKey, String errorMessage) {
		if (StrUtil.isBlank(value)) {
			addError(errorKey, errorMessage);
			return ;
		}
		try {
			int temp = Integer.parseInt(value.trim());
			if (temp < min || temp > max) {
				addError(errorKey, errorMessage);
			}
		}
		catch (Exception e) {
			addError(errorKey, errorMessage);
		}
	}

	protected void validateInteger(String field, String errorKey, String errorMessage) {
		validateIntegerValue(controller.getParam(field), errorKey, errorMessage);
	}

	protected void validateIntegerPath(String pathKey, String errorKey, String errorMessage) {
		String value = controller.getPathParam(pathKey);
		if (value != null && (value.startsWith("N") || value.startsWith("n"))) {
			value = "-" + value.substring(1);
		}
		validateIntegerValue(value, errorKey, errorMessage);
	}

	private void validateIntegerValue(String value, String errorKey, String errorMessage) {
		if (StrUtil.isBlank(value)) {
			addError(errorKey, errorMessage);
			return ;
		}
		try {
			Integer.parseInt(value.trim());
		}
		catch (Exception e) {
			addError(errorKey, errorMessage);
		}
	}

	protected void validateLong(String field, long min, long max, String errorKey, String errorMessage) {
		validateLongValue(controller.getParam(field), min, max, errorKey, errorMessage);
	}

	protected void validateLongPath(String pathKey, long min, long max, String errorKey, String errorMessage) {
		String value = controller.getPathParam(pathKey);
		if (value != null && (value.startsWith("N") || value.startsWith("n"))) {
			value = "-" + value.substring(1);
		}
		validateLongValue(value, min, max, errorKey, errorMessage);
	}

	private void validateLongValue(String value, long min, long max, String errorKey, String errorMessage) {
		if (StrUtil.isBlank(value)) {
			addError(errorKey, errorMessage);
			return ;
		}
		try {
			long temp = Long.parseLong(value.trim());
			if (temp < min || temp > max) {
				addError(errorKey, errorMessage);
			}
		}
		catch (Exception e) {
			addError(errorKey, errorMessage);
		}
	}

	protected void validateLong(String field, String errorKey, String errorMessage) {
		validateLongValue(controller.getParam(field), errorKey, errorMessage);
	}

	protected void validateLongPath(String pathKey, String errorKey, String errorMessage) {
		String value = controller.getPathParam(pathKey);
		if (value != null && (value.startsWith("N") || value.startsWith("n"))) {
			value = "-" + value.substring(1);
		}
		validateLongValue(value, errorKey, errorMessage);
	}

	private void validateLongValue(String value, String errorKey, String errorMessage) {
		if (StrUtil.isBlank(value)) {
			addError(errorKey, errorMessage);
			return ;
		}
		try {
			Long.parseLong(value.trim());
		}
		catch (Exception e) {
			addError(errorKey, errorMessage);
		}
	}

	protected void validateDouble(String field, double min, double max, String errorKey, String errorMessage) {
		String value = controller.getParam(field);
		if (StrUtil.isBlank(value)) {
			addError(errorKey, errorMessage);
			return ;
		}
		try {
			double temp = Double.parseDouble(value.trim());
			if (temp < min || temp > max) {
				addError(errorKey, errorMessage);
			}
		}
		catch (Exception e) {
			addError(errorKey, errorMessage);
		}
	}

	protected void validateDouble(String field, String errorKey, String errorMessage) {
		String value = controller.getParam(field);
		if (StrUtil.isBlank(value)) {
			addError(errorKey, errorMessage);
			return ;
		}
		try {
			Double.parseDouble(value.trim());
		}
		catch (Exception e) {
			addError(errorKey, errorMessage);
		}
	}

	protected void validateDate(String field, String errorKey, String errorMessage) {
		String value = controller.getParam(field);
		if (StrUtil.isBlank(value)) {
			addError(errorKey, errorMessage);
			return ;
		}
		try {
			new SimpleDateFormat(getDatePattern()).parse(value.trim());	
		}
		catch (Exception e) {
			addError(errorKey, errorMessage);
		}
	}

	protected void validateDate(String field, Date min, Date max, String errorKey, String errorMessage) {
		String value = controller.getParam(field);
		if (StrUtil.isBlank(value)) {
			addError(errorKey, errorMessage);
			return ;
		}
		try {
			Date temp = new SimpleDateFormat(getDatePattern()).parse(value.trim());	
			if (temp.before(min) || temp.after(max)) {
				addError(errorKey, errorMessage);
			}
		}
		catch (Exception e) {
			addError(errorKey, errorMessage);
		}
	}

	protected void validateDate(String field, String min, String max, String errorKey, String errorMessage) {

		try {
			SimpleDateFormat sdf = new SimpleDateFormat(getDatePattern());
			validateDate(field, sdf.parse(min.trim()), sdf.parse(max.trim()), errorKey, errorMessage);
		} catch (Exception e) {
			addError(errorKey, errorMessage);
		}
	}

	protected void validateEqualField(String field_1, String field_2, String errorKey, String errorMessage) {
		String value_1 = controller.getParam(field_1);
		String value_2 = controller.getParam(field_2);
		if (value_1 == null || value_2 == null || (! value_1.equals(value_2))) {
			addError(errorKey, errorMessage);
		}
	}

	protected void validateEqualString(String s1, String s2, String errorKey, String errorMessage) {
		if (s1 == null || s2 == null || (! s1.equals(s2))) {
			addError(errorKey, errorMessage);
		}
	}

	protected void validateEqualInteger(Integer i1, Integer i2, String errorKey, String errorMessage) {
		if (i1 == null || i2 == null || (i1.intValue() != i2.intValue())) {
			addError(errorKey, errorMessage);
		}
	}

	protected void validateEmail(String field, String errorKey, String errorMessage) {
		validateRegex(field, emailAddressPattern, false, errorKey, errorMessage);
	}

	protected void validateUrl(String field, String errorKey, String errorMessage) {
		String value = controller.getParam(field);
		if (StrUtil.isBlank(value)) {
			addError(errorKey, errorMessage);
			return ;
		}
		try {
			value = value.trim();
			if (value.startsWith("https://")) {
				value = "http://" + value.substring(8); // URL doesn't understand the https protocol, hack it
			}
			new URL(value);
		} catch (MalformedURLException e) {
			addError(errorKey, errorMessage);
		}
	}

	protected void validateRegex(String field, String regExpression, boolean isCaseSensitive, String errorKey, String errorMessage) {
        String value = controller.getParam(field);
        if (value == null) {
        	addError(errorKey, errorMessage);
        	return ;
        }
        Pattern pattern = isCaseSensitive ? Pattern.compile(regExpression) : Pattern.compile(regExpression, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(value);
        if (!matcher.matches()) {
        	addError(errorKey, errorMessage);
        }
	}

	protected void validateRegex(String field, String regExpression, String errorKey, String errorMessage) {
		validateRegex(field, regExpression, true, errorKey, errorMessage);
	}

	protected void validateString(String field, int minLen, int maxLen, String errorKey, String errorMessage) {
		validateStringValue(controller.getParam(field), minLen, maxLen, errorKey, errorMessage);
	}

	protected void validateStringPath(String pathKey, int minLen, int maxLen, String errorKey, String errorMessage) {
		validateStringValue(controller.getPathParam(pathKey), minLen, maxLen, errorKey, errorMessage);
	}

	private void validateStringValue(String value, int minLen, int maxLen, String errorKey, String errorMessage) {
		if (minLen > 0 && StrUtil.isBlank(value)) {
			addError(errorKey, errorMessage);
			return ;
		}
		if (value == null) {		
			value = "";
		}
		if (value.length() < minLen || value.length() > maxLen) {
			addError(errorKey, errorMessage);
		}
	}

	protected void validateBoolean(String field, String errorKey, String errorMessage) {
		validateBooleanValue(controller.getParam(field), errorKey, errorMessage);
	}

	protected void validateBooleanPath(String pathKey, String errorKey, String errorMessage) {
		validateBooleanValue(controller.getPathParam(pathKey), errorKey, errorMessage);
	}

	private void validateBooleanValue(String value, String errorKey, String errorMessage) {
		if (StrUtil.isBlank(value)) {
			addError(errorKey, errorMessage);
			return ;
		}
		value = value.trim().toLowerCase();
		if ("1".equals(value) || "true".equals(value)) {
			return ;
		} else if ("0".equals(value) || "false".equals(value)) {
			return ;
		}
		addError(errorKey, errorMessage);
	}

	protected void validateCaptcha(String field, String errorKey, String errorMessage) {
		if (getController().validateCaptcha(field) == false) {
			addError(errorKey, errorMessage);
		}
	}
}

