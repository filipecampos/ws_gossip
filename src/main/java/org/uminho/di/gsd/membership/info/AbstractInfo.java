/*******************************************************************************
 * Copyright (c) 2014 Filipe Campos.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package org.uminho.di.gsd.membership.info;

public abstract class AbstractInfo {
	protected long updated; // ou Date?
	protected long heartbeat;

	public long getHeartbeat() {
		return heartbeat;
	}

	public long getUpdated() {
		return updated;
	}

	public void heartbeat() {
		setHeartbeat(heartbeat + 1);
	}

	public void setHeartbeat(long hb) {
		heartbeat = hb;
		updateTimestamp();
	}

	public void updateTimestamp() {
		updated = System.currentTimeMillis();
	}

	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("\nHeartbeat-");
		sb.append(heartbeat);

		sb.append("\nUpdated at-");
		sb.append(updated);

		return sb.toString();
	}
}
