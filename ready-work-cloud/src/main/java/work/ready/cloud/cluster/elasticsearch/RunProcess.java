/**
 *
 * Copyright (c) 2020 WeiHua Lyu [ready.work]
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
package work.ready.cloud.cluster.elasticsearch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.tools.StrUtil;

class RunProcess {

	private static final Log logger = LogFactory.getLog(RunProcess.class);

	private static final AtomicLong number = new AtomicLong();

	private final List<Object> arguments = new ArrayList<>();

	private final Map<String, Object> environment = new LinkedHashMap<>();

	private Path workingDirectory;

	RunProcess(Object... arguments) {
		this(null, arguments);
	}

	RunProcess(Path workingDirectory, Object... arguments) {
		this.workingDirectory = workingDirectory;
		this.arguments.addAll(Arrays.asList(arguments));
	}

	List<Object> getArguments() {
		return this.arguments;
	}

	void setArguments(Object... arguments) {
		this.arguments.clear();
		this.arguments.addAll(Arrays.asList(arguments));
	}

	void addArguments(Object... arguments) {
		this.arguments.addAll(Arrays.asList(arguments));
	}

	Map<String, Object> getEnvironment() {
		return this.environment;
	}

	void putEnvironment(String name, Object value) {
		Objects.requireNonNull(name, "'name' must not be null");
		this.environment.put(name, value);
	}

	Path getWorkingDirectory() {
		return this.workingDirectory;
	}

	void setWorkingDirectory(Path workingDirectory) {
		this.workingDirectory = workingDirectory;
	}

	Process start() throws IOException {
		Path workDir = this.workingDirectory;
		List<String> arguments = this.arguments.stream().filter(Objects::nonNull).map(Object::toString).filter(
				StrUtil::notBlank).collect(Collectors.toList());
		ProcessBuilder builder = new ProcessBuilder(arguments).redirectErrorStream(true);
		if (workDir != null) {
			builder.directory(workDir.toFile());
		}
		Map<String, String> environment = this.environment.entrySet().stream().filter(
				entry -> Objects.nonNull(entry.getKey())).collect(
				Collectors.toMap(Map.Entry::getKey, entry -> Objects.toString(entry.getValue(), "")));
		builder.environment().putAll(environment);
		printCommand(workDir, arguments, environment);
		return builder.start();
	}

	int run(Consumer<? super String> consumer) throws InterruptedException, IOException {
		Objects.requireNonNull(consumer, "'consumer' must not be null");
		Process process = start();
		Thread thread = new Thread(() -> {
			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
				try {
					reader.lines().filter(StrUtil::notBlank).forEach(consumer);
				}
				catch (UncheckedIOException ex) {
					if (!ex.getMessage().contains("Stream closed")) {
						throw ex;
					}
				}
			}
			catch (IOException ex) {
				throw new UncheckedIOException("Stream cannot be closed", ex);
			}
		});
		thread.setUncaughtExceptionHandler((t, ex) -> logger.error(ex, "Exception in thread " + t));
		thread.setName("process-" + number.getAndIncrement());
		thread.setDaemon(true);
		thread.start();
		int exit = process.waitFor();
		thread.join(100);
		return exit;
	}

	private static void printCommand(Path workDir, List<String> arguments, Map<String, String> environment) {
		StringBuilder msg = new StringBuilder(String.format("Run a command '%s'", String.join(" ", arguments)));
		if (workDir != null) {
			msg.append(String.format(" within the directory '%s'", workDir));
		}
		if (!environment.isEmpty()) {
			msg.append(String.format(" using the environment %s", environment));
		}
		logger.info(msg.toString());
	}

}
