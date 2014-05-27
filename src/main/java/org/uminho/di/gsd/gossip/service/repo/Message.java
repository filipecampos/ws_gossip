/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.uminho.di.gsd.gossip.service.repo;

import org.ws4d.java.types.URI;

/**
 *
 * @author fjoc
 */
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
