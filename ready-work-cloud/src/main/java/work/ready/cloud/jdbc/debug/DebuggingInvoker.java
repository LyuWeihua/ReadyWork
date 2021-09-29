/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package work.ready.cloud.jdbc.debug;

import work.ready.cloud.jdbc.olap.JdbcSQLException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

abstract class DebuggingInvoker implements InvocationHandler {

    private final Object target;

    protected final Object parent;

    final DebugLog log;

    DebuggingInvoker(DebugLog log, Object target, Object parent) {
        this.log = log;
        this.target = target;
        this.parent = parent;
    }

    @Override
    public final Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String name = method.getName();
        Class<?>[] params = method.getParameterTypes();

        if ("equals".equals(name) && params.length == 1 && params[0] == Object.class) {
            Object o = args[0];
            if (o == null || !(o instanceof DebugProxy)) {
                return Boolean.FALSE;
            }
            InvocationHandler ih = Proxy.getInvocationHandler(o);
            return (ih instanceof DebuggingInvoker && target.equals(((DebuggingInvoker) ih).target));
        }

        else if ("hashCode".equals(name) && params.length == 0) {
            return System.identityHashCode(proxy);
        }

        else if ("toString".equals(name) && params.length == 0) {
            return "Debug proxy for " + target;
        }

        try {
            Object result = method.invoke(target, args);
            log.logResult(method, args, result);
            return result == null || result instanceof DebugProxy ? result : postProcess(result, proxy);
        } catch (InvocationTargetException ex) {
            log.logException(method, args, ex.getCause());
            throw ex.getCause();
        } catch (Exception ex) {
            
            log.logException(method, args, ex);
            throw new JdbcSQLException(ex, "Debugging failed for [" + method + "]");
        }
    }

    protected Object postProcess(Object result, Object proxy) {
        return result;
    }

    Object target() {
        return target;
    }
}
