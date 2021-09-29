/**
 *
 * Original work Copyright 2014 Red Hat, Inc.
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
package work.ready.core.tools;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLockInterruptionException;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class FileLock implements AutoCloseable {

	private final FileChannel fileChannel;

	private final Map<Thread, java.nio.channels.FileLock> locks = new ConcurrentHashMap<>();

	private FileLock(FileChannel fileChannel) {
		this.fileChannel = fileChannel;
	}

	public static FileLock of(Path file) throws IOException {
		Objects.requireNonNull(file, "'file' must not be null");
		return new FileLock(FileChannel.open(FileUtil.createIfNotExists(file), StandardOpenOption.WRITE));
	}

	public FileChannel getChannel() {
		return this.fileChannel;
	}

	public boolean tryLock(long timeout, TimeUnit timeUnit) throws FileLockInterruptionException, IOException {
		Objects.requireNonNull(timeUnit, "'timeUnit' must not be null");
		java.nio.channels.FileLock fileLock = this.locks.get(Thread.currentThread());
		if (fileLock != null && fileLock.isValid()) {
			return true;
		}
		long startTime = System.nanoTime();
		long rem = timeUnit.toNanos(timeout);
		do {
			fileLock = lock(this.fileChannel);
			if (fileLock != null) {
				this.locks.put(Thread.currentThread(), fileLock);
				return true;
			}
			if (rem > 0) {
				try {
					Thread.sleep(Math.min(TimeUnit.NANOSECONDS.toMillis(rem) + 1, 100));
				}
				catch (InterruptedException ex) {
					throw new FileLockInterruptionException();
				}
			}
			rem = timeUnit.toNanos(timeout) - (System.nanoTime() - startTime);
		} while (rem > 0);
		return false;
	}

	public void release() throws IOException {
		java.nio.channels.FileLock fileLock = this.locks.remove(Thread.currentThread());
		if (fileLock != null) {
			fileLock.close();
		}
	}

	@Override
	public void close() throws IOException {
		
		this.fileChannel.close();
		this.locks.clear();
	}

	private static java.nio.channels.FileLock lock(FileChannel fileChannel) throws IOException {
		try {
			return fileChannel.tryLock();
		}
		catch (OverlappingFileLockException ex) {
			return null;
		}
	}

}
