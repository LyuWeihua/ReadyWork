/*
 * Copyright (C) 2013 Brett Wooldridge
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
 */

package work.ready.core.database.jdbc.hikari.util;

import java.lang.reflect.Constructor;
import java.util.Locale;
import java.util.concurrent.*;

import static java.lang.Thread.currentThread;
import static java.util.concurrent.TimeUnit.SECONDS;

public final class UtilityElf
{
   private UtilityElf()
   {
      
   }

   public static String getNullIfEmpty(final String text)
   {
      return text == null ? null : text.trim().isEmpty() ? null : text.trim();
   }

   public static void quietlySleep(final long millis)
   {
      try {
         Thread.sleep(millis);
      }
      catch (InterruptedException e) {
         
         currentThread().interrupt();
      }
   }

   public static boolean safeIsAssignableFrom(Object obj, String className) {
      try {
         Class<?> clazz = Class.forName(className);
         return clazz.isAssignableFrom(obj.getClass());
      } catch (ClassNotFoundException ignored) {
         return false;
      }
   }

   public static <T> T createInstance(final String className, final Class<T> clazz, final Object... args)
   {
      if (className == null) {
         return null;
      }

      try {
         Class<?> loaded = UtilityElf.class.getClassLoader().loadClass(className);
         if (args.length == 0) {
            return clazz.cast(loaded.getDeclaredConstructor().newInstance());
         }

         Class<?>[] argClasses = new Class<?>[args.length];
         for (int i = 0; i < args.length; i++) {
            argClasses[i] = args[i].getClass();
         }
         Constructor<?> constructor = loaded.getConstructor(argClasses);
         return clazz.cast(constructor.newInstance(args));
      }
      catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   public static ThreadPoolExecutor createThreadPoolExecutor(final int queueSize, final String threadName, ThreadFactory threadFactory, final RejectedExecutionHandler policy)
   {
      if (threadFactory == null) {
         threadFactory = new DefaultThreadFactory(threadName, true);
      }

      LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(queueSize);
      ThreadPoolExecutor executor = new ThreadPoolExecutor(1 , 1 , 5 , SECONDS, queue, threadFactory, policy);
      executor.allowCoreThreadTimeOut(true);
      return executor;
   }

   public static ThreadPoolExecutor createThreadPoolExecutor(final BlockingQueue<Runnable> queue, final String threadName, ThreadFactory threadFactory, final RejectedExecutionHandler policy)
   {
      if (threadFactory == null) {
         threadFactory = new DefaultThreadFactory(threadName, true);
      }

      ThreadPoolExecutor executor = new ThreadPoolExecutor(1 , 1 , 5 , SECONDS, queue, threadFactory, policy);
      executor.allowCoreThreadTimeOut(true);
      return executor;
   }

   public static int getTransactionIsolation(final String transactionIsolationName)
   {
      if (transactionIsolationName != null) {
         try {
            
            final String upperCaseIsolationLevelName = transactionIsolationName.toUpperCase(Locale.ENGLISH);
            return IsolationLevel.valueOf(upperCaseIsolationLevelName).getLevelId();
         } catch (IllegalArgumentException e) {
            
            try {
               final int level = Integer.parseInt(transactionIsolationName);
               for (IsolationLevel iso : IsolationLevel.values()) {
                  if (iso.getLevelId() == level) {
                     return iso.getLevelId();
                  }
               }

               throw new IllegalArgumentException("Invalid transaction isolation value: " + transactionIsolationName);
            }
            catch (NumberFormatException nfe) {
               throw new IllegalArgumentException("Invalid transaction isolation value: " + transactionIsolationName, nfe);
            }
         }
      }

      return -1;
   }

   public static final class DefaultThreadFactory implements ThreadFactory {

      private final String threadName;
      private final boolean daemon;

      public DefaultThreadFactory(String threadName, boolean daemon) {
         this.threadName = threadName;
         this.daemon = daemon;
      }

      @Override
      public Thread newThread(Runnable r) {
         Thread thread = new Thread(r, threadName);
         thread.setDaemon(daemon);
         return thread;
      }
   }
}
