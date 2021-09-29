/**
 *
 * Original work Copyright (c) 2002 P6Spy
 * Modified work Copyright (c) 2020 WeiHua Lyu [ready.work]
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
package work.ready.core.database.jdbc.common;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;

public class Value {

  private static final char[] HEX_CHARS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E',
          'F' };

  private Object value;

  public Value(Object valueToSet) {
    this();
    this.value = valueToSet;
  }

  public Value() {
  }

  public Object getValue() {
    return value;
  }

  public void setValue(Object value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return convertToString(this.value);
  }

  public String convertToString(Object value) {
    String result;
    if (value == null) {
      result = "NULL";
    } else {

      if (value instanceof Timestamp) {
        result = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(value);
      } else if (value instanceof Date) {
        result = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(value);
      } else if (value instanceof Boolean) {

          result = value.toString();
        
      } else if (value instanceof byte[]) {

        result = toHexString((byte[]) value);

      } else {
        result = value.toString();
      }

      result = quoteIfNeeded(result, value);
    }

    return result;
  }

  private String toHexString(byte[] bytes) {
    StringBuilder sb = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      int temp = (int) b & 0xFF;
      sb.append(HEX_CHARS[temp / 16]);
      sb.append(HEX_CHARS[temp % 16]);
    }
    return sb.toString();
  }

  private String quoteIfNeeded(String stringValue, Object obj) {
    if (stringValue == null) {
      return null;
    }

    if (Number.class.isAssignableFrom(obj.getClass()) || Boolean.class.isAssignableFrom(obj.getClass())) {
      return stringValue;
    } else {
      return "'" + escape(stringValue) + "'";
    }
  }

  private String escape(String stringValue) {
    return stringValue.replaceAll("'", "''");
  }

}
