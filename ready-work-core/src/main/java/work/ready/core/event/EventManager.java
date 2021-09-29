/**
 *
 * Original work Copyright jfinal-event L.cm
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
package work.ready.core.event;

import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.module.Initializer;
import work.ready.core.module.ShutdownHook;
import work.ready.core.server.Ready;
import work.ready.core.tools.ReadyThreadFactory;
import work.ready.core.tools.define.CheckedConsumer;
import work.ready.core.tools.define.ConcurrentMultiMap;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;

public class EventManager {

    private static final Log logger = LogFactory.getLog(EventManager.class);

    private final List<ApplicationListenerAdapter> listenerList;

    private final List<Class<?>> registry;

    private ExecutorService pool = null;

    protected List<Consumer<GeneralEvent>> eventFilter = new ArrayList<>();
    protected List<Initializer<EventManager>> initializers = new ArrayList<>();
    
    private ConcurrentMultiMap<EventType, ApplicationListenerAdapter> cacheForEvent
            = new ConcurrentMultiMap<>();
    private ConcurrentMultiMap<String, ApplicationListenerAdapter> cacheForGeneralEvent
            = new ConcurrentMultiMap<>();

    public EventManager(){
        this.listenerList = new ArrayList<>();
        this.registry = new ArrayList<>();
    }

    public void addInitializer(Initializer<EventManager> initializer) {
        this.initializers.add(initializer);
        initializers.sort(Comparator.comparing(Initializer::order));
    }

    public void startInit() {
        try {
            for (Initializer<EventManager> i : initializers) {
                i.startInit(this);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void endInit() {
        try {
            for (Initializer<EventManager> i : initializers) {
                i.endInit(this);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public EventManager async() {
        if (pool == null) {
            pool = Executors.newSingleThreadExecutor(new ReadyThreadFactory("Event"));
            Ready.shutdownHook.add(ShutdownHook.STAGE_9, (inMs)->pool.shutdown());
        }
        return this;
    }

    public EventManager threadPool(ExecutorService executorService) {
        pool = executorService;
        return this;
    }

    public synchronized EventManager addListener(Class<?> listener) {
        if(registry.contains(listener)) {
            if (logger.isWarnEnabled())
                logger.warn("Listener " + listener.getCanonicalName() + " already registered, skip.");
            return this;
        }
        Method[] methods = listener.getMethods();
        for (Method method : methods) {
            if (!method.isAnnotationPresent(EventListener.class) || method.getParameterCount() > 1) {
                continue;
            }
            listenerList.add(new ApplicationListenerAdapter(method));
            if (logger.isDebugEnabled())
                logger.debug("Listener " + listener.getCanonicalName() + method.getName() + " attached.");
        }
        registry.add(listener);
        cacheForEvent.clear();
        cacheForGeneralEvent.clear();
        return this;
    }

    public synchronized EventManager addListener(Object listener) {
        if(registry.contains(listener.getClass())) {
            if (logger.isWarnEnabled())
                logger.warn("Listener " + listener.getClass().getCanonicalName() + " already registered, skip.");
            return this;
        }
        Method[] methods = listener.getClass().getMethods();
        for (Method method : methods) {
            if (!method.isAnnotationPresent(EventListener.class) || method.getParameterCount() > 1) {
                continue;
            }
            listenerList.add(new ApplicationListenerAdapter(listener, method));
            if (logger.isDebugEnabled())
                logger.debug("Listener " + listener.getClass().getCanonicalName() + "." + method.getName() + " attached.");
        }
        registry.add(listener.getClass());
        cacheForEvent.clear();
        cacheForGeneralEvent.clear();
        return this;
    }

    public final class ListenerSetter implements EventListener {
        private List<Class<?>> value = new ArrayList<>();
        private List<String> name = new ArrayList<>();
        private List<Class<?>> events = new ArrayList<>();
        private int order = 0;
        private boolean async = false;
        private String condition = "";
        private Function<Object, Boolean> filter;
        private Object contextReference;
        private boolean global = true;

        @Override
        public Class<?>[] value() {
            return value.toArray(new Class<?>[0]);
        }

        public ListenerSetter addValue(Class<?> value){
            this.value.add(value);
            return this;
        }

        @Override
        public String[] name() {
            return name.toArray(new String[0]);
        }

        public ListenerSetter addName(String name){
            this.name.add(name);
            return this;
        }

        @Override
        public Class<?>[] events() {
            return events.toArray(new Class<?>[0]);
        }

        public ListenerSetter addEvent(Class<?> event){
            this.events.add(event);
            return this;
        }

        @Override
        public int order() {
            return order;
        }

        public ListenerSetter setOrder(int order){
            this.order = order;
            return this;
        }

        @Override
        public boolean async() {
            return async;
        }

        public ListenerSetter setAsync(boolean async){
            this.async = async;
            return this;
        }

        @Override
        public String condition() {
            return condition;
        }

        public ListenerSetter setCondition(String condition){
            this.condition = condition;
            return this;
        }

        public Function<Object, Boolean> filter() {
            return filter;
        }

        public ListenerSetter setFilter(Function<Object, Boolean> filter) {
            this.filter = filter;
            return this;
        }

        public Object getContextReference() {
            return contextReference;
        }

        public ListenerSetter setContextReference(Object contextReference) {
            this.contextReference = contextReference;
            return this;
        }

        @Override
        public boolean global() {
            return global;
        }

        public ListenerSetter setGlobal(boolean global) {
            this.global = global;
            return this;
        }

        @Override
        public Class<? extends Annotation> annotationType()
        {
            return EventListener.class;
        }
    }

    public synchronized EventManager addListener(Object listener, String method, Consumer<ListenerSetter> listenTo) {
        ListenerSetter setter = new ListenerSetter();
        listenTo.accept(setter);

        Method[] methods = listener.getClass().getMethods();
        boolean found = false;
        for (Method m : methods) {
            if (!m.getName().equals(method) || m.isAnnotationPresent(EventListener.class) || m.getParameterCount() > 1) {
                continue;
            }
            found = true;
            listenerList.add(new ApplicationListenerAdapter(listener, m, setter));
            if (logger.isDebugEnabled())
                logger.debug("Listener %s.%s attached on %s.", listener.getClass().getCanonicalName(), m.getName(), setter.name);
        }
        if(!found) throw new RuntimeException("could not find listener method '" + method + "' in class " + listener.getClass().getCanonicalName());
        cacheForEvent.clear();
        cacheForGeneralEvent.clear();
        return this;
    }

    public synchronized EventManager addListener(Consumer<ListenerSetter> listenTo, CheckedConsumer<GeneralEvent, Exception> handler) {
        ListenerSetter setter = new ListenerSetter();
        listenTo.accept(setter);
        listenerList.add(new ApplicationListenerAdapter(setter, handler));
        if (logger.isDebugEnabled())
            logger.debug("Listener with lambda handler attached on %s.", setter.name);
        cacheForEvent.clear();
        cacheForGeneralEvent.clear();
        return this;
    }

    public EventManager addListener(String generalEventName, boolean async, CheckedConsumer<GeneralEvent, Exception> handler) {
        return addListener((setter)->setter.addName(generalEventName).setAsync(async), handler);
    }

    public EventManager addListener(String generalEventName, boolean async, boolean global, CheckedConsumer<GeneralEvent, Exception> handler) {
        return addListener((setter)->setter.addName(generalEventName).setAsync(async).setGlobal(global), handler);
    }

    private List<ApplicationListenerAdapter> getListener(EventType eventType) {
        if (listenerList.isEmpty()) {
            return Collections.emptyList();
        }
        return cacheForEvent.computeIfAbsent(eventType, (key) -> initListeners(listenerList, key));
    }

    private List<ApplicationListenerAdapter> initListeners(List<ApplicationListenerAdapter> listeners, EventType eventType) {
        final Class<?> eventClass = eventType.getEventClass();
        final Class<?> sourceClass = eventType.getSourceClass();
        final Class<?> sourceEventClass = sourceClass == null ? eventClass : sourceClass;
        final List<ApplicationListenerAdapter> list = new ArrayList<>();
        for (ApplicationListenerAdapter listener : listeners) {
            
            List<Class<?>> declaredEventClasses = listener.getDeclaredEventClasses();
            if (!declaredEventClasses.isEmpty()) {
                boolean canExec = false;
                for (Class<?> annType : declaredEventClasses) {
                    
                    if (annType.isAssignableFrom(sourceEventClass)) {
                        canExec = true;
                        break;
                    }
                }
                if (!canExec) {
                    continue;
                }
            }
            
            if (listener.getParamCount() > 0) {
                
                Class<?> paramType = listener.getParamType();
                
                if (!paramType.isAssignableFrom(sourceEventClass)) {
                    continue;
                }
            }
            list.add(listener);
        }
        
        if (list.size() > 1) {
            list.sort(Comparator.comparingInt(ApplicationListenerAdapter::getOrder));
        }
        return list;
    }

    public EventManager setEventFilter(int idx, Consumer<GeneralEvent> eventFilter){
        this.eventFilter.add(idx, eventFilter);
        return this;
    }

    public EventManager setEventFilter(Consumer<GeneralEvent> eventFilter){
        this.eventFilter.add(eventFilter);
        return this;
    }

    public void post(Object event) {
        Objects.requireNonNull(event, "event cannot be null");
        EventType eventType;
        if (event instanceof BaseEvent) {
            eventType = new EventType(event.getClass(), null);
        } else {
            eventType = new EventType(BaseEvent.class, event.getClass());
        }
        post(event, eventType);
    }

    private void post(final Object event, EventType eventType) {
        List<ApplicationListenerAdapter> finalList;
        boolean internalEvent = true;
        if(event instanceof GeneralEvent){
            for (Consumer<GeneralEvent> filter : eventFilter) {
                filter.accept((GeneralEvent)event);
            }
            if (((GeneralEvent)event).isSkip()) return;
            internalEvent = ((GeneralEvent)event).isInternal(); 
            String name = ((GeneralEvent)event).getName();
            finalList = cacheForGeneralEvent.computeIfAbsent(name, (key)->{
                List<ApplicationListenerAdapter> list = new ArrayList<>();
                final List<ApplicationListenerAdapter> listenerList = getListener(eventType);
                for(var adapter : listenerList){
                    if(Arrays.asList(adapter.getName()).contains(key))
                        list.add(adapter);
                }
                return list;
            });
        } else {
            finalList = getListener(eventType);
        }
        for (final ApplicationListenerAdapter listener : finalList) {
            if(!listener.isGlobal() && !internalEvent) continue; 
            if (null != pool && listener.isAsync()) {
                pool.submit(() -> listener.onApplicationEvent(event));
            } else {
                listener.onApplicationEvent(event);
            }
        }
    }
}
