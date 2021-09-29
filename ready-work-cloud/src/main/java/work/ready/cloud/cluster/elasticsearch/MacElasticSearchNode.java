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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

class MacElasticSearchNode extends AbstractElasticSearchNode {

	private final Version version;

	private final Path workingDirectory;

	MacElasticSearchNode(Version version, Path workingDirectory, List<String> jvmOptions,
						 Map<String, Object> systemProperties,
						 Map<String, Object> environmentVariables, Map<String, Object> properties) {
		super(workingDirectory, properties, jvmOptions, systemProperties, environmentVariables);
		this.version = version;
		this.workingDirectory = workingDirectory;
	}

	@Override
	Process doStart(RunProcess runProcess) throws IOException {
		for(String fileOrPath : macFilesNeedExecutePermission){
			Path executable = this.workingDirectory.resolve(fileOrPath);
			if (!Files.isExecutable(executable)) {
				executable.toFile().setExecutable(true);
			}
			if(Files.isDirectory(executable)){
				Files.walk(this.workingDirectory.resolve(fileOrPath), 1).filter(path -> Files.isRegularFile(path) && !Files.isExecutable(path)).forEach(path->path.toFile().setExecutable(true));
			}
		}
		Path executableFile = this.workingDirectory.resolve("bin/elasticsearch");
		if (!Files.isExecutable(executableFile)) {
			executableFile.toFile().setExecutable(true);
		}
		runProcess.setArguments(executableFile);
		
		return runProcess.start();
	}

	@Override
	void doStop(Process process, long pid) throws IOException, InterruptedException {
		if (pid > 0 && kill(pid) == 0) {
			if (!process.waitFor(5, TimeUnit.SECONDS)) {
				sigkill(pid);
			}
		}
		else {
			process.destroy();
		}
	}

	private int kill(long pid) throws InterruptedException, IOException {
		return new RunProcess(this.workingDirectory, "kill", "-SIGINT", pid).run(this.logger::info);
	}

	private void sigkill(long pid) throws InterruptedException, IOException {
		new RunProcess(this.workingDirectory, "kill", "-SIGKILL", pid).run(this.logger::info);
	}

}
