/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.uminho.di.gsd.common;

import org.apache.log4j.Logger;

/**
 *
 * @author fjoc
 */
public class RunConstants {

    static Logger logger = Logger.getLogger(RunConstants.class);

    int base_port;
    int port;
    String ip;
    int run;
    int producers;
    int devices;
    int totalDevices;
    int messages;
    long timeInterval;
    int fanout = -1;
    String disseminationType;
    boolean simulated;
    int ackInterval = 5;

    public RunConstants(String[] args)
    {
        switch(args.length)
        {
            case 12: ackInterval = Integer.parseInt(args[11]);
            case 11: simulated = Boolean.parseBoolean(args[10]);
            case 10: disseminationType = args[9];
            case 9: fanout = Integer.parseInt(args[8]);
            case 8: timeInterval = Integer.parseInt(args[7]);
            case 7: messages = Integer.parseInt(args[6]);
            case 6: totalDevices = devices = Integer.parseInt(args[5]);
            case 5: producers = Integer.parseInt(args[4]);
            case 4: run = Integer.parseInt(args[3]);
            case 3: base_port = Integer.parseInt(args[2]);
            case 2:
            {
                ip = args[1];
                port = Integer.parseInt(args[0]);
            }
        }
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public int getProducers() {
        return producers;
    }

    public int getDevices() {
        return devices;
    }

    public int getTotalDevices() {
        return totalDevices;
    }

    public void setTotalDevices(int total_devices) {
        this.totalDevices = total_devices;
    }

    public int getMessages() {
        return messages;
    }

    public int getRun() {
        return run;
    }

    public long getTimeInterval() {
        return timeInterval;
    }

    public int getFanout() {
        return fanout;
    }

    public int getBasePort() {
        return base_port;
    }

    public String getDisseminationType() {
        return disseminationType;
    }

    public boolean isSimulated() {
        return simulated;
    }

    public int getAckInterval()
    {
        return ackInterval;
    }
    
    public String getFileName()
    {
        StringBuilder sb = new StringBuilder("_r").append(run)
                .append('_').append(producers).append("p_")
                .append(devices).append("n_")
                .append(messages).append("m_")
                .append(timeInterval).append("ms");

        return sb.toString();
    }
}
