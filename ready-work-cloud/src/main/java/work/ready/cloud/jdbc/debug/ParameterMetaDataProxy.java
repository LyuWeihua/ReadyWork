/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package work.ready.cloud.jdbc.debug;

final class ParameterMetaDataProxy extends DebuggingInvoker {

    ParameterMetaDataProxy(DebugLog log, Object target) {
        super(log, target, null);
    }
}
