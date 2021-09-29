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

package work.ready.core.template.expr;

import work.ready.core.template.stat.Location;
import work.ready.core.template.stat.ParseException;

import java.math.BigDecimal;

public class NumTok extends Tok {

	private Number value;

	NumTok(Sym sym, String s, int radix, boolean isScientificNotation, Location location) {
		super(sym, location.getRow());
		try {
			typeConvert(sym, s, radix, isScientificNotation, location);
		} catch (Exception e) {
			throw new ParseException(e.getMessage(), location, e);
		}
	}

	private void typeConvert(Sym sym, String s, int radix, boolean isScientificNotation, Location location) {
		switch (sym) {
		case INT:
			if (isScientificNotation) {
				value = new BigDecimal(s).intValue();
			} else {
				value = Integer.valueOf(s, radix);		
			}
			break ;
		case LONG:
			if (isScientificNotation) {
				value = new BigDecimal(s).longValue();
			} else {
				value = Long.valueOf(s, radix);			
			}
			break ;
		case FLOAT:
			if (isScientificNotation) {
				value = new BigDecimal(s).floatValue();
			} else {
				value = Float.valueOf(s);				
			}
			break ;
		case DOUBLE:
			if (isScientificNotation) {
				value = new BigDecimal(s).doubleValue();
			} else {
				value = Double.valueOf(s);				
			}
			break ;
		default :
			throw new ParseException("Unsupported type: " + sym.value(), location);
		}
	}

	public String value() {
		return value.toString();
	}

	public Object getNumberValue() {
		return value;
	}

	public String toString() {
		return sym.value() + " : " + value;
	}
}

