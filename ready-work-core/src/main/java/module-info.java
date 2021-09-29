module ready.work.core {
    uses work.ready.core.database.jdbc.event.JdbcEventListener;
    uses work.ready.core.module.CoreContextInitializer;
    uses work.ready.core.component.cache.Cache;
    uses work.ready.core.module.ApplicationContextInitializer;

    exports work.ready.core.aop;
    exports work.ready.core.aop.annotation;
    exports work.ready.core.aop.transformer;
    exports work.ready.core.aop.transformer.enhance;
    exports work.ready.core.aop.transformer.match;

    exports work.ready.core.apm;
    exports work.ready.core.apm.common;
    exports work.ready.core.apm.model;
    exports work.ready.core.apm.reporter;
    exports work.ready.core.apm.collector.logger;
    exports work.ready.core.apm.collector.jdbc;
    exports work.ready.core.apm.collector.jdbc.interceptor;

    exports work.ready.core.component.cache;
    exports work.ready.core.component.cache.annotation;
    exports work.ready.core.component.crypto;
    exports work.ready.core.component.decrypt;
    exports work.ready.core.component.i18n;
    exports work.ready.core.component.plugin;
    exports work.ready.core.component.proxy;
    exports work.ready.core.component.serializer;
    exports work.ready.core.component.snowflake;
    exports work.ready.core.component.switcher;
    exports work.ready.core.component.time;

    exports work.ready.core.config;

    exports work.ready.core.database;
    exports work.ready.core.database.cleverorm;
    exports work.ready.core.database.annotation;
    exports work.ready.core.database.datasource to ready.work.cloud;
    exports work.ready.core.database.jdbc.event;
    exports work.ready.core.database.jdbc.common;
    exports work.ready.core.database.transaction;
    exports work.ready.core.event;
    exports work.ready.core.exception;
    exports work.ready.core.handler;
    exports work.ready.core.handler.validate;
    exports work.ready.core.handler.route;
    exports work.ready.core.handler.resource;
    exports work.ready.core.handler.session;
    exports work.ready.core.handler.request;
    exports work.ready.core.handler.response;
    exports work.ready.core.ioc;
    exports work.ready.core.ioc.aware;
    exports work.ready.core.ioc.annotation;
    exports work.ready.core.json;
    exports work.ready.core.log;
    exports work.ready.core.module;
    exports work.ready.core.module.system;
    exports work.ready.core.render;
    exports work.ready.core.security;
    exports work.ready.core.security.cors;
    exports work.ready.core.security.data;
    exports work.ready.core.security.access.limiter;
    exports work.ready.core.security.access.limiter.storage;
    exports work.ready.core.security.access.limiter.limit;
    exports work.ready.core.security.access.limiter.storage.utils;
    exports work.ready.core.security.access.limiter.exception;
    exports work.ready.core.security.access.limiter.limit.override;
    exports work.ready.core.security.access.limiter.trigger;
    exports work.ready.core.server;
    exports work.ready.core.service;
    exports work.ready.core.service.result;
    exports work.ready.core.service.status;
    exports work.ready.core.template;
    exports work.ready.core.template.ext.directive;
    exports work.ready.core.tools;
    exports work.ready.core.tools.validator;
    exports work.ready.core.tools.define;
    exports work.ready.core.tools.define.io;

    requires transitive jdk.attach;
    requires transitive java.instrument;
    requires transitive java.sql;
    requires transitive java.naming;
    requires transitive java.desktop;
    requires transitive java.management;
    requires transitive java.compiler;
    requires transitive java.net.http;

    requires transitive xnio.api;
    requires transitive undertow.core;
    requires org.slf4j;

    requires transitive com.fasterxml.jackson.datatype.jsr310;
    requires transitive com.fasterxml.jackson.module.afterburner;
    requires transitive com.fasterxml.jackson.dataformat.yaml;
    requires transitive com.fasterxml.jackson.dataformat.xml;
    requires transitive com.fasterxml.jackson.databind;

    requires jedis;
    requires net.bytebuddy;
    requires net.bytebuddy.agent;
    requires transitive com.esotericsoftware.kryo;
    requires jsqlparser;
    requires org.apache.commons.compress;
    requires commons.pool2;
    requires h2;
    requires mysql.connector.java;
    requires com.github.benmanes.caffeine;
    requires pinyin4j;
    requires com.google.zxing.javase;
    requires com.google.zxing;
}

