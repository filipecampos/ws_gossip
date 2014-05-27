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

package org.uminho.di.gsd.gossip.service.repo;

import org.ws4d.java.types.URI;

public class Message {
    private URI identifier;
    private URI action;
    private long rounds;
    private String message;
    private long received_at; // milliseconds

    public Message(URI identifier, URI action, long rounds, String message, long millis) {
        this.identifier = identifier;
        this.action = action;
        this.rounds = rounds;
        this.message = message;
        this.received_at = millis;
    }

    public URI getAction() {
        return action;
    }

    public void setAction(URI action) {
        this.action = action;
    }

    public URI getIdentifier() {
        return identifier;
    }

    public void setIdentifier(URI identifier) {
        this.identifier = identifier;
    }

    public long getReceived_at() {
        return received_at;
    }

    public void resetReceived_at() {
        this.received_at = System.currentTimeMillis();
    }

    public long getRounds() {
        return rounds;
    }

    public void setRounds(long rounds) {
        this.rounds = rounds;
    }

    public long decrementAndGetRounds() {
        return --this.rounds;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        Message copy = new Message(identifier, action, rounds, message, received_at);
        return copy;
    }
}
