/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package work.ready.core.database.query;

import work.ready.core.database.annotation.Column;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;

public class BeanProcessor {

    protected static final int PROPERTY_NOT_FOUND = -1;

    private static final Map<Class<?>, Object> primitiveDefaults = new HashMap<>();

    private static final List<ColumnHandler> columnHandlers = new ArrayList<>();

    private static final List<PropertyHandler> propertyHandlers = new ArrayList<>();

    private final Map<String, String> columnToPropertyOverrides;

    static {
        primitiveDefaults.put(Integer.TYPE, Integer.valueOf(0));
        primitiveDefaults.put(Short.TYPE, Short.valueOf((short) 0));
        primitiveDefaults.put(Byte.TYPE, Byte.valueOf((byte) 0));
        primitiveDefaults.put(Float.TYPE, Float.valueOf(0f));
        primitiveDefaults.put(Double.TYPE, Double.valueOf(0d));
        primitiveDefaults.put(Long.TYPE, Long.valueOf(0L));
        primitiveDefaults.put(Boolean.TYPE, Boolean.FALSE);
        primitiveDefaults.put(Character.TYPE, Character.valueOf((char) 0));

        for (final ColumnHandler handler : ServiceLoader.load(ColumnHandler.class)) {
            columnHandlers.add(handler);
        }

        for (final PropertyHandler handler : ServiceLoader.load(PropertyHandler.class)) {
            propertyHandlers.add(handler);
        }
    }

    public BeanProcessor() {
        this(new HashMap<String, String>());
    }

    public BeanProcessor(final Map<String, String> columnToPropertyOverrides) {
        if (columnToPropertyOverrides == null) {
            throw new IllegalArgumentException("columnToPropertyOverrides map cannot be null");
        }
        this.columnToPropertyOverrides = columnToPropertyOverrides;
    }

    public <T> T toBean(final ResultSet rs, final Class<? extends T> type) throws SQLException {
        final T bean = this.newInstance(type);
        return this.populateBean(rs, bean);
    }

    public <T> List<T> toBeanList(final ResultSet rs, final Class<? extends T> type) throws SQLException {
        final List<T> results = new ArrayList<>();

        if (!rs.next()) {
            return results;
        }

        final PropertyDescriptor[] props = this.propertyDescriptors(type);
        final ResultSetMetaData rsmd = rs.getMetaData();
        final int[] columnToProperty = this.mapColumnsToProperties(rsmd, props);

        do {
            results.add(this.createBean(rs, type, props, columnToProperty));
        } while (rs.next());

        return results;
    }

    private <T> T createBean(final ResultSet rs, final Class<T> type,
                             final PropertyDescriptor[] props, final int[] columnToProperty)
    throws SQLException {

        final T bean = this.newInstance(type);
        return populateBean(rs, bean, props, columnToProperty);
    }

    public <T> T populateBean(final ResultSet rs, final T bean) throws SQLException {
        final PropertyDescriptor[] props = this.propertyDescriptors(bean.getClass());
        final ResultSetMetaData rsmd = rs.getMetaData();
        final int[] columnToProperty = this.mapColumnsToProperties(rsmd, props);

        return populateBean(rs, bean, props, columnToProperty);
    }

    private <T> T populateBean(final ResultSet rs, final T bean,
            final PropertyDescriptor[] props, final int[] columnToProperty)
            throws SQLException {

        for (int i = 1; i < columnToProperty.length; i++) {

            if (columnToProperty[i] == PROPERTY_NOT_FOUND) {
                continue;
            }

            final PropertyDescriptor prop = props[columnToProperty[i]];
            final Class<?> propType = prop.getPropertyType();

            Object value = null;
            if (propType != null) {
                value = this.processColumn(rs, i, propType);

                if (value == null && propType.isPrimitive()) {
                    value = primitiveDefaults.get(propType);
                }
            }

            this.callSetter(bean, prop, value);
        }

        return bean;
    }

    private void callSetter(final Object target, final PropertyDescriptor prop, Object value)
            throws SQLException {

        final Method setter = getWriteMethod(target, prop, value);

        if (setter == null || setter.getParameterTypes().length != 1) {
            return;
        }

        try {
            final Class<?> firstParam = setter.getParameterTypes()[0];
            for (final PropertyHandler handler : propertyHandlers) {
                if (handler.match(firstParam, value)) {
                    value = handler.apply(firstParam, value);
                    break;
                }
            }

            if (this.isCompatibleType(value, firstParam)) {
                setter.invoke(target, value);
            } else {
              throw new SQLException(
                  "Cannot set " + prop.getName() + ": incompatible types, cannot convert "
                  + value.getClass().getName() + " to " + firstParam.getName());
                  
            }

        } catch (final IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
            throw new SQLException(
                "Cannot set " + prop.getName() + ": " + e.getMessage());
        }
    }

    private boolean isCompatibleType(final Object value, final Class<?> type) {
        
        if (value == null || type.isInstance(value) || matchesPrimitive(type, value.getClass())) {
            return true;

        }
        return false;

    }

    private boolean matchesPrimitive(final Class<?> targetType, final Class<?> valueType) {
        if (!targetType.isPrimitive()) {
            return false;
        }

        try {
            
            final Field typeField = valueType.getField("TYPE");
            final Object primitiveValueType = typeField.get(valueType);

            if (targetType == primitiveValueType) {
                return true;
            }
        } catch (final NoSuchFieldException | IllegalAccessException e) {

        }
        return false;
    }

    protected Method getWriteMethod(final Object target, final PropertyDescriptor prop, final Object value) {
        final Method method = prop.getWriteMethod();
        return method;
    }

    protected <T> T newInstance(final Class<T> c) throws SQLException {
        try {
            return c.getDeclaredConstructor().newInstance();

        } catch (final IllegalAccessException | InstantiationException | InvocationTargetException |
            NoSuchMethodException e) {
            throw new SQLException("Cannot create " + c.getName() + ": " + e.getMessage());
        }
    }

    private PropertyDescriptor[] propertyDescriptors(final Class<?> c)
        throws SQLException {
        
        BeanInfo beanInfo = null;
        try {
            beanInfo = Introspector.getBeanInfo(c);

        } catch (final IntrospectionException e) {
            throw new SQLException(
                "Bean introspection failed: " + e.getMessage());
        }

        return beanInfo.getPropertyDescriptors();
    }

    protected int[] mapColumnsToProperties(final ResultSetMetaData rsmd,
            final PropertyDescriptor[] props) throws SQLException {

        final int cols = rsmd.getColumnCount();
        final int[] columnToProperty = new int[cols + 1];
        Arrays.fill(columnToProperty, PROPERTY_NOT_FOUND);

        for (int col = 1; col <= cols; col++) {
            String columnName = rsmd.getColumnLabel(col);
            if (null == columnName || 0 == columnName.length()) {
              columnName = rsmd.getColumnName(col);
            }
            String propertyName = columnToPropertyOverrides.get(columnName);
            if (propertyName == null) {
                propertyName = columnName;
            }
            for (int i = 0; i < props.length; i++) {

                final PropertyDescriptor prop = props[i];
                final Column column = prop.getReadMethod().getAnnotation(Column.class);
                String propertyColumnName = null;
                if (column != null) {
                    propertyColumnName = column.name();
                } else {
                    propertyColumnName = prop.getName();
                }
                if (propertyName.equalsIgnoreCase(propertyColumnName)) {
                    columnToProperty[col] = i;
                    break;
                }
            }
        }

        return columnToProperty;
    }

    protected Object processColumn(final ResultSet rs, final int index, final Class<?> propType)
        throws SQLException {

        Object retval = rs.getObject(index);

        if ( !propType.isPrimitive() && retval == null ) {
            return null;
        }

        for (final ColumnHandler handler : columnHandlers) {
            if (handler.match(propType)) {
                retval = handler.apply(rs, index);
                break;
            }
        }

        return retval;

    }

}
