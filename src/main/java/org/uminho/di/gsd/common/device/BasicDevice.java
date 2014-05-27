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

package org.uminho.di.gsd.common.device;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.uminho.di.gsd.common.Configuration;
import org.uminho.di.gsd.common.RunConstants;
import org.uminho.di.gsd.gossip.client.workers.ActionTask;
import org.ws4d.java.DPWSFramework;
import org.ws4d.java.communication.HTTPBinding;
import org.ws4d.java.service.DefaultDevice;
import org.ws4d.java.service.Operation;
import org.ws4d.java.service.Service;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.types.EndpointReference;
import org.ws4d.java.types.URI;

public class BasicDevice extends DefaultDevice {

    final static Logger logger = Logger.getLogger(BasicDevice.class);
    
    // enter your local IP here
    protected String IP = null;
    protected String PORT = null;
    protected FileWriter fileWriter = null;
    protected FileWriter hopsFileWriter = null;

    protected boolean simulated = false;

    protected ActionTask actionTask;

    protected String idStr = null;

    protected RunConstants constants;

    public BasicDevice() {
        super();
    }

    /* Getters and setters */
    public void setIp(String ip) {
        IP = ip;
    }

    public String getIp()
    {
        return IP;
    }

    public void setPort(String port) {
        PORT = port;
    }

    public String getPort() {
        return PORT;
    }

    public void setSimulated(boolean simulated) {
        this.simulated = simulated;
    }

    public boolean isSimulated() {
        return simulated;
    }

    public void setFileWriter(FileWriter fileWriter) {
        this.fileWriter = fileWriter;
    }

    public FileWriter getFileWriter() {
        return fileWriter;
    }

    public String getIdStr() {
        return idStr;
    }

    public RunConstants getConstants() {
        return constants;
    }

    public void setConstants(RunConstants consts) {
        constants = consts;

        setIp(constants.getIp());
        setPort(Integer.toString(constants.getPort()));

        setSimulated(constants.isSimulated());

        EndpointReference devEPR = new EndpointReference(new URI("urn:uuid:device" + PORT));
        setEndpointReference(devEPR);
    }

    /* Initializers */
    public void initializeConfiguration() {
        Configuration.loadConfigs();
    }

    public void initializeBinding() {

        if ((IP != null) && (!IP.isEmpty()) && (PORT != null) && (!PORT.isEmpty())) {
            this.addBinding(new HTTPBinding(new URI("http://" + IP + ":" + PORT + "/device")));
            idStr = "Device" + PORT;
        }
    }

    public void startDevice() {
        startServices();

        try {
            this.start();
            logger.debug("Started device " + IP + ":" + PORT + "\n");
            if(fileWriter != null)
            {
                this.fileWriter.append("Started device " + IP + ":" + PORT + "\n");
                this.fileWriter.flush();
            }
        } catch (IOException ex) {
            logger.error(ex);
        }
    }

    public void stopDevice() {
        stopServices();

        shutdown();
        try {
            this.stop();
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }
    
    // to be overriden
    public void startServices()
    {
        // no service
    }

    public void stopServices()
    {
        
    }

    public void writeStats(String filename)
    {
        File file = new File(filename);
        File hopsFile = new File("hops_" + filename);
        try {
            fileWriter = new FileWriter(file, true);
            hopsFileWriter = new FileWriter(hopsFile, true);

            writeStats();

            fileWriter.close();
            hopsFileWriter.close();
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    public void shutdown() {
        shutdownWorkers();
    }

    public void writeStats() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        // configure loggers
        PropertyConfigurator.configure("log4j.properties");

        if (args.length >= 2) {
            RunConstants consts = new RunConstants(args);

            BasicDevice device = null;
            
            try {
                // always start the framework first
                DPWSFramework.start(args);

                // create the device ...
                device = new BasicDevice();
                device.setConstants(consts);

                device.initializeConfiguration();

                device.initializeBinding();

                // no services


                // start device
                device.startDevice();
            }
            catch(Exception e)
            {
                logger.error(e.getMessage(), e);
                device.stopDevice();
                DPWSFramework.stop();

                System.exit(0);
            }
        }
    }

    public void shutdownWorkers() {
        if(actionTask != null)
        {
            logger.debug("Terminating Pull Task...");
            actionTask.setTerminate(true);

            actionTask = null;
        }
    }

    public void inspectServicesAndOperations()
    {
        Iterator services = getServices();
        while(services.hasNext())
        {
            Service service = (Service) services.next();
            logger.debug("Got service with ID: " + service.getServiceId());

            Iterator operations = service.getOperations();
            while(operations.hasNext())
            {
                Operation op = (Operation) operations.next();
                logger.debug("Got operation: " + op.getName() + " portType: " + op.getPortType());
            }
        }
    }
}
