package work.ready.cloud.jdbc.common;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class VirtualMethod<C> {

    private static final Set<Method> singletonSet = Collections.synchronizedSet(new HashSet<Method>());

    private final Class<C> baseClass;
    private final String method;
    private final Class<?>[] parameters;
    private final ClassValue<Integer> distanceOfClass = new ClassValue<Integer>() {
        @Override
        protected Integer computeValue(Class<?> subclazz) {
            return Integer.valueOf(reflectImplementationDistance(subclazz));
        }
    };

    public VirtualMethod(Class<C> baseClass, String method, Class<?>... parameters) {
        this.baseClass = baseClass;
        this.method = method;
        this.parameters = parameters;
        try {
            if (!singletonSet.add(baseClass.getDeclaredMethod(method, parameters)))
                throw new UnsupportedOperationException(
                        "VirtualMethod instances must be singletons and therefore " +
                                "assigned to static final members in the same class, they use as baseClass ctor param."
                );
        } catch (NoSuchMethodException nsme) {
            throw new IllegalArgumentException(baseClass.getName() + " has no such method: "+nsme.getMessage());
        }
    }

    public int getImplementationDistance(final Class<? extends C> subclazz) {
        return distanceOfClass.get(subclazz).intValue();
    }

    public boolean isOverriddenAsOf(final Class<? extends C> subclazz) {
        return getImplementationDistance(subclazz) > 0;
    }

    int reflectImplementationDistance(final Class<?> subclazz) {
        if (!baseClass.isAssignableFrom(subclazz))
            throw new IllegalArgumentException(subclazz.getName() + " is not a subclass of " + baseClass.getName());
        boolean overridden = false;
        int distance = 0;
        for (Class<?> clazz = subclazz; clazz != baseClass && clazz != null; clazz = clazz.getSuperclass()) {
            
            if (!overridden) {
                try {
                    clazz.getDeclaredMethod(method, parameters);
                    overridden = true;
                } catch (NoSuchMethodException nsme) {
                }
            }

            if (overridden) distance++;
        }
        return distance;
    }

    public static <C> int compareImplementationDistance(final Class<? extends C> clazz,
                                                        final VirtualMethod<C> m1, final VirtualMethod<C> m2)
    {
        return Integer.compare(m1.getImplementationDistance(clazz), m2.getImplementationDistance(clazz));
    }

}
