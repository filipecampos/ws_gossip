/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.uminho.di.gsd.membership.info;

/**
 *
 * @author fjoc
 */
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
//        System.out.println("Tum tum...");
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
