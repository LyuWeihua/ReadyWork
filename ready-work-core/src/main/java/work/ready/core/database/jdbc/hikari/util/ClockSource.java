/*
 * Copyright (C) 2015 Brett Wooldridge
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

import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.*;

public interface ClockSource
{
   static ClockSource CLOCK = Factory.create();

   static long currentTime() {
      return CLOCK.currentTime0();
   }

   long currentTime0();

   static long toMillis(long time) {
      return CLOCK.toMillis0(time);
   }

   long toMillis0(long time);

   static long toNanos(long time) {
      return CLOCK.toNanos0(time);
   }

   long toNanos0(long time);

   static long elapsedMillis(long startTime) {
      return CLOCK.elapsedMillis0(startTime);
   }

   long elapsedMillis0(long startTime);

   static long elapsedMillis(long startTime, long endTime) {
      return CLOCK.elapsedMillis0(startTime, endTime);
   }

   long elapsedMillis0(long startTime, long endTime);

   static long elapsedNanos(long startTime) {
      return CLOCK.elapsedNanos0(startTime);
   }

   long elapsedNanos0(long startTime);

   static long elapsedNanos(long startTime, long endTime) {
      return CLOCK.elapsedNanos0(startTime, endTime);
   }

   long elapsedNanos0(long startTime, long endTime);

   static long plusMillis(long time, long millis) {
      return CLOCK.plusMillis0(time, millis);
   }

   long plusMillis0(long time, long millis);

   static TimeUnit getSourceTimeUnit() {
      return CLOCK.getSourceTimeUnit0();
   }

   TimeUnit getSourceTimeUnit0();

   static String elapsedDisplayString(long startTime, long endTime) {
      return CLOCK.elapsedDisplayString0(startTime, endTime);
   }

   default String elapsedDisplayString0(long startTime, long endTime) {
      long elapsedNanos = elapsedNanos0(startTime, endTime);

      StringBuilder sb = new StringBuilder(elapsedNanos < 0 ? "-" : "");
      elapsedNanos = Math.abs(elapsedNanos);

      for (TimeUnit unit : TIMEUNITS_DESCENDING) {
         long converted = unit.convert(elapsedNanos, NANOSECONDS);
         if (converted > 0) {
            sb.append(converted).append(TIMEUNIT_DISPLAY_VALUES[unit.ordinal()]);
            elapsedNanos -= NANOSECONDS.convert(converted, unit);
         }
      }

      return sb.toString();
   }

   TimeUnit[] TIMEUNITS_DESCENDING = {DAYS, HOURS, MINUTES, SECONDS, MILLISECONDS, MICROSECONDS, NANOSECONDS};

   String[] TIMEUNIT_DISPLAY_VALUES = {"ns", "µs", "ms", "s", "m", "h", "d"};

   class Factory
   {
      private static ClockSource create() {
         String os = System.getProperty("os.name");
         if ("Mac OS X".equals(os)) {
            return new MillisecondClockSource();
         }

         return new NanosecondClockSource();
      }
   }

   final class MillisecondClockSource implements ClockSource
   {
      
      @Override
      public long currentTime0() {
         return System.currentTimeMillis();
      }

      @Override
      public long elapsedMillis0(final long startTime) {
         return System.currentTimeMillis() - startTime;
      }

      @Override
      public long elapsedMillis0(final long startTime, final long endTime) {
         return endTime - startTime;
      }

      @Override
      public long elapsedNanos0(final long startTime) {
         return MILLISECONDS.toNanos(System.currentTimeMillis() - startTime);
      }

      @Override
      public long elapsedNanos0(final long startTime, final long endTime) {
         return MILLISECONDS.toNanos(endTime - startTime);
      }

      @Override
      public long toMillis0(final long time) {
         return time;
      }

      @Override
      public long toNanos0(final long time) {
         return MILLISECONDS.toNanos(time);
      }

      @Override
      public long plusMillis0(final long time, final long millis) {
         return time + millis;
      }

      @Override
      public TimeUnit getSourceTimeUnit0() {
         return MILLISECONDS;
      }
   }

   class NanosecondClockSource implements ClockSource
   {
      
      @Override
      public long currentTime0() {
         return System.nanoTime();
      }

      @Override
      public long toMillis0(final long time) {
         return NANOSECONDS.toMillis(time);
      }

      @Override
      public long toNanos0(final long time) {
         return time;
      }

      @Override
      public long elapsedMillis0(final long startTime) {
         return NANOSECONDS.toMillis(System.nanoTime() - startTime);
      }

      @Override
      public long elapsedMillis0(final long startTime, final long endTime) {
         return NANOSECONDS.toMillis(endTime - startTime);
      }

      @Override
      public long elapsedNanos0(final long startTime) {
         return System.nanoTime() - startTime;
      }

      @Override
      public long elapsedNanos0(final long startTime, final long endTime) {
         return endTime - startTime;
      }

      @Override
      public long plusMillis0(final long time, final long millis) {
         return time + MILLISECONDS.toNanos(millis);
      }

      @Override
      public TimeUnit getSourceTimeUnit0() {
         return NANOSECONDS;
      }
   }
}
