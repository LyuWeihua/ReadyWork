/**
 * Copyright (c) 2011-2019, James Zhan 詹波 (jfinal@126.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package work.ready.core.template.stat;

public class Ctrl {

	private static final int JUMP_NONE = 0;
	private static final int JUMP_BREAK = 1;
	private static final int JUMP_CONTINUE = 2;
	private static final int JUMP_RETURN = 3;

	private static final int WISDOM_ASSIGNMENT = 0;
	private static final int LOCAL_ASSIGNMENT = 1;
	private static final int GLOBAL_ASSIGNMENT = 2;

	private int jump = JUMP_NONE;
	private int assignmentType = WISDOM_ASSIGNMENT;
	private boolean nullSafe = false;

	public Object attachment;

	public void setAttachment(Object attachment) {
		this.attachment = attachment;
	}

	@SuppressWarnings("unchecked")
	public <T> T getAttachment() {
		return (T)attachment;
	}

	public boolean isJump() {
		return jump != JUMP_NONE;
	}

	public boolean notJump() {
		return jump == JUMP_NONE;
	}

	public boolean isBreak() {
		return jump == JUMP_BREAK;
	}

	public void setBreak() {
		jump = JUMP_BREAK;
	}

	public boolean isContinue() {
		return jump == JUMP_CONTINUE;
	}

	public void setContinue() {
		jump = JUMP_CONTINUE;
	}

	public boolean isReturn() {
		return jump == JUMP_RETURN;
	}

	public void setReturn() {
		jump = JUMP_RETURN;
	}

	public void setJumpNone() {
		jump = JUMP_NONE;
	}

	public boolean isWisdomAssignment() {
		return assignmentType == WISDOM_ASSIGNMENT;
	}

	public void setWisdomAssignment() {
		assignmentType = WISDOM_ASSIGNMENT;
	}

	public boolean isLocalAssignment() {
		return assignmentType == LOCAL_ASSIGNMENT;
	}

	public void setLocalAssignment() {
		assignmentType = LOCAL_ASSIGNMENT;
	}

	public boolean isGlobalAssignment() {
		return assignmentType == GLOBAL_ASSIGNMENT;
	}

	public void setGlobalAssignment() {
		assignmentType = GLOBAL_ASSIGNMENT;
	}

	public boolean isNullSafe() {
		return nullSafe;
	}

	public boolean notNullSafe() {
		return !nullSafe;
	}

	public void setNullSafe(boolean nullSafe) {
		this.nullSafe = nullSafe;
	}
}

