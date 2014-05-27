/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.uminho.di.gsd.common;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.uminho.di.gsd.gossip.GossipVariants;
import org.ws4d.java.structures.ArrayList;
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.structures.List;

/**
 *
 * @author fjoc
 */
public class Configuration {

    static Logger logger = Logger.getLogger(Configuration.class);

    private static String fileName = "config.properties";

    // membership
    public final static String updatePeriod = "membership.updatePeriod";
    public final static String queryRate = "membership.queryRate";
    public final static String cacheSize = "membership.cacheSize";

    // gossip
    public final static String actions = "gossip.actions";
    public final static String prefVariant = "gossip.prefVariant";
    public final static String targetSamplingUnit = "gossip.targetSampling.unit";
    public final static String targetSamplingValue = "gossip.targetSampling.value";
    public final static String monitoringService = "gossip.monitoringService";
    public final static String fanout = "gossip.fanout";
    public final static String maxRounds = "gossip.maxRounds";
    public final static String buffer = "gossip.buffer";
    public final static String pullPeriod = "gossip.pullPeriod";

    // aggregation
    public final static String waitResponses = "gossip.aggregation.waiting.responses";
    public final static String waitTime = "gossip.aggregation.waiting.time";

    private static List wantedActions = new ArrayList();

    private static HashMap params = new HashMap();

    public static void loadConfigs()
    {
        loadConfigs("config.properties");
    }

    public static void loadConfigs(String file)
    {
        Properties config = new Properties();
        try {
            config.load(new FileInputStream(fileName));

            readMembershipParameters(config);

            readGossipParameters(config);
        } catch (IOException ex) {
            logger.error(ex);
        }
    }

    public static boolean isWantedAction(String action)
    {
        boolean wanted = (wantedActions.contains(action)) || (wantedActions.contains("all"));

        return wanted;
    }

    private static void readMembershipParameters(Properties config) {
        readLongConfigParam(config, Configuration.updatePeriod);
        readLongConfigParam(config, Configuration.queryRate);
        readLongConfigParam(config, Configuration.cacheSize);
    }

    private static Long readLongConfigParam(Properties config, String paramName)
    {
        Long ret = null;
        
        String paramValue = config.getProperty(paramName);

        if((paramValue != null) && (!paramValue.isEmpty()))
        {
            ret = Long.parseLong(paramValue);

            if(ret != null)
            {
                params.put(paramName, ret);
                logger.debug("Read " + paramName + " : " + ret);
            }
            else
                logger.error("Error reading Long param " + paramName);
        }

        return ret;
    }

    private static String readStringConfigParam(Properties config, String paramName)
    {
        String paramValue = null;

        paramValue = config.getProperty(paramName);

        if((paramValue != null) && (!paramValue.isEmpty()))
        {
            params.put(paramName, paramValue);
            logger.debug("Read " + paramName + " : " + paramValue);
        }
        else
            logger.error("Error reading String param " + paramName);

        return paramValue;
    }

    private static String readMultipleConfigParam(Properties config, String paramName)
    {
        String paramValue = null;

        paramValue = config.getProperty(paramName);

        if((paramValue != null) && (!paramValue.isEmpty()))
        {
            params.put(paramName, paramValue);
            logger.debug("Read " + paramName + " : " + paramValue);
        }
        else
            logger.error("Error reading param " + paramName);

        if(paramName.equalsIgnoreCase(Configuration.actions))
        {
            String[] splitted = paramValue.split(";");

            for(String action : splitted)
                wantedActions.add(action);
        }

        return paramValue;
    }

    private static void readGossipParameters(Properties config) {
        readMultipleConfigParam(config, Configuration.actions);
        readStringConfigParam(config, Configuration.prefVariant);
        readStringConfigParam(config, Configuration.targetSamplingUnit);
        readLongConfigParam(config, Configuration.targetSamplingValue);
        readStringConfigParam(config, Configuration.monitoringService);
        readLongConfigParam(config, Configuration.fanout);
        readLongConfigParam(config, Configuration.maxRounds);
        readLongConfigParam(config, Configuration.buffer);
        readLongConfigParam(config, Configuration.pullPeriod);
        
        // aggregation
        readLongConfigParam(config, Configuration.waitTime);
        readLongConfigParam(config, Configuration.waitResponses);
    }

    public static Object getConfigParamValue(String paramName)
    {
        Object value = null;
        
        if(paramName.equalsIgnoreCase(Configuration.actions))
        {
            // actions
            value = wantedActions;
        }
        else
        {
            value = params.get(paramName);
        }

        if(paramName.equalsIgnoreCase(Configuration.prefVariant))
        {
            String str = (String) value;
            value = GossipVariants.valueOf(str);
        }

        return value;
    }

    public static void setConfigParamValue(String paramName, Object value)
    {
        if(paramName.equalsIgnoreCase(Configuration.actions))
        {
            // actions
            wantedActions.add(value);
        }
        else
        {
            params.put(paramName, value);
        }
    }
}
