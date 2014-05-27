package org.uminho.di.gsd.membership.client;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import org.apache.log4j.Logger;
import org.uminho.di.gsd.common.Configuration;
import org.uminho.di.gsd.common.Constants;
import org.uminho.di.gsd.membership.device.MembershipDevice;
import org.uminho.di.gsd.membership.info.MembershipRepository;
import org.uminho.di.gsd.membership.info.ServiceInfo;
import org.ws4d.java.DPWSFramework;
import org.ws4d.java.client.DefaultClient;
import org.ws4d.java.client.SearchParameter;
import org.ws4d.java.communication.TimeoutException;
import org.ws4d.java.dispatch.HelloData;
import org.ws4d.java.service.Device;
import org.ws4d.java.service.InvocationException;
import org.ws4d.java.service.Operation;
import org.ws4d.java.service.Service;
import org.ws4d.java.service.parameter.ParameterValue;
import org.ws4d.java.service.reference.DeviceReference;
import org.ws4d.java.service.reference.ServiceReference;
import org.ws4d.java.structures.ArrayList;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.structures.List;
import org.ws4d.java.types.EndpointReference;
import org.ws4d.java.types.QName;
import org.ws4d.java.types.QNameSet;
import org.ws4d.java.types.URI;

public class MembershipRepositoryClient extends DefaultClient {

    static Logger logger = Logger.getLogger(MembershipRepositoryClient.class);
    private MembershipRepository repository;
    private List latencies;
    private MembershipDevice device;
    private Long queryRate;
    private Random random;
    private int[] probs;
    private String idStr = "";

    public MembershipRepositoryClient() {
        this.init();
    }

    public MembershipRepositoryClient(MembershipRepository repository) {
        this.repository = repository;
        this.init();
    }

    public void setDevice(MembershipDevice dvc) {
        device = dvc;
        // get configuration values

        queryRate = (Long) Configuration.getConfigParamValue(Configuration.queryRate);
        idStr = "[Device" + device.getPort() + "]";
        logger.debug(idStr + "QueryRate=" + queryRate);

        random = new Random();

        initProbabilities();
    }

    private void initProbabilities() {
        probs = new int[100];

        for (int i = 0; i < 100; i++) {
            if (i < queryRate) {
                probs[i] = 1;
            } else {
                probs[i] = 0;
            }
        }

        Collections.shuffle(Arrays.asList(probs));
    }

    public void setRepository(MembershipRepository repository) {
        this.repository = repository;
    }

    public MembershipRepository getRepository() {
        return repository;
    }

    private void init() {
        latencies = new ArrayList();

        this.initDiscoveryListening();
    }

    private void initDiscoveryListening() {
        // makes helloReceived catch Hello messages
        this.registerHelloListening();


        // Register client for service reference changes
        this.registerServiceListening();
    }

    private boolean decideQuery() {
        int index = random.nextInt(100);

        return (probs[index] == 1);
    }

    /**
     * Callback method, if device hello was received
     * @param hd
     */
    @Override
    public void helloReceived(HelloData hd) {
        // for now only considers hello messages coming from devices
        boolean query = decideQuery();

        if (query) {
            // if device announced Gossip or MembershipType or if, according to configuration, it is supposed to be queried
            DeviceReference deviceRef = getDeviceReference(hd);
            addDeviceToRepository(deviceRef);
        }
    }

    @Override
    public void deviceFound(DeviceReference devRef, SearchParameter searchParams) {
        logger.debug(idStr + "Found Device with Reference " + devRef.getEndpointReference());

        addDeviceToRepository(devRef);
    }

    @Override
    public void serviceFound(ServiceReference sr, SearchParameter sp) {
        logger.debug(idStr + "Client found service with ID: " + sr.getServiceId());

        Iterator iter = sr.getPortTypes();
        boolean matchType = false;

        while (iter.hasNext() && !matchType) {
            QName portType = (QName) iter.next();
            matchType = Constants.MembershipPortTypeQName.equals(portType)
                    || Constants.GossipPushPortQName.equals(portType);
        }

        if (matchType) {
            try {
                Service svc = sr.getService();
                DeviceReference devRef = svc.getParentDeviceReference();
                logger.debug(idStr + "Device " + devRef.getEndpointReference().getAddress() + " being added.");
                repository.addDeviceInfo(devRef.getDevice());
            } catch (TimeoutException ex) {
                logger.error(idStr + ex.getMessage(), ex);
            }
        }
    }

    @Override
    public void deviceBye(DeviceReference devRef) {
        // Remove device and hosted services from repository
        repository.removeDeviceInfo(devRef);
    }

    @Override
    public void serviceDisposed(ServiceReference sr) {
        URI serviceAddress = sr.getPreferredXAddress();
        if (serviceAddress == null) {
            Iterator iter = sr.getEndpointReferences();
            while (iter.hasNext()) {
                serviceAddress = ((EndpointReference) iter.next()).getAddress();
            }
        }

        if (serviceAddress != null) {
            repository.removeServiceInfo(serviceAddress);
        }
    }

    public Device getDevice(URI devRef) {
        Device dvc = null;
        try {
            dvc = getDeviceReference(new EndpointReference(devRef)).getDevice();
        } catch (TimeoutException ex) {
            logger.error(idStr + ex.getMessage(), ex);
        }

        return dvc;
    }

    public void initUpdate() {
        try {
            // selects update target membership service randomly from repository
            URI updateTarget = repository.selectUpdateTarget();

            if (updateTarget != null) {
                Operation updateOperation = null;
                ParameterValue updateRequest;
                try {
                    // instantiates service
                    EndpointReference epr = new EndpointReference(updateTarget);
                    ServiceReference sr = getServiceReference(epr);
                    Service svc = sr.getService();
                    updateOperation = svc.getAnyOperation(Constants.MembershipPortTypeQName, "Update");
                } catch (TimeoutException ex) {
                    logger.error(idStr + ex.getMessage(), ex);
                } catch (Exception ex) {
                    logger.error(idStr + ex.getMessage(), ex);
                }

                // Invoke update op
                if (updateOperation != null) {
                    try {
                        updateRequest = updateOperation.createInputValue();

                        logger.debug(idStr + "Going to invoke Update op at " + updateTarget);

                        updateRequest = ServiceInfo.fromServiceInfoListToPV(repository.preUpdateList(), updateRequest);

                        long start = System.nanoTime();
                        ParameterValue updateResponse = updateOperation.invoke(updateRequest);
                        long end = System.nanoTime();

                        latencies.add(end - start);

                        // received list is merged with repository
                        if (updateResponse == null) {
                            logger.error(idStr + "Update Response is null!");
                        } else {
                            repository.merge(ServiceInfo.fromPVToServiceInfoList(updateResponse));
                        }
                    } catch (InvocationException ex) {
                        logger.error(idStr + ex.getMessage(), ex);
                    } catch (TimeoutException ex) {
                        logger.error(idStr + ex.getMessage(), ex);
                    } catch (Exception ex) {
                        logger.error(idStr + ex.getMessage(), ex);
                    }
                } else {
                    logger.error(idStr + "No Update operation found for target " + updateTarget);
                }

            } else {
                logger.error(idStr + "No update target found!" + repository.toString());

                // there is no service in repository so search for a membership service
                SearchParameter param = new SearchParameter();
                QNameSet qns = new QNameSet(Constants.MembershipPortTypeQName);
                param.setServiceTypes(qns);

                searchService(param);
            }

        } catch (Exception e) {
            logger.error(idStr + e.getMessage(), e);
        }

    }

    private void addDeviceToRepository(DeviceReference devRef) {
        EndpointReference ownDvcEPR = repository.getDevice().getEndpointReference();

        if (!ownDvcEPR.equals(devRef.getEndpointReference())) {
            try {
                Device dvc = devRef.getDevice();
                repository.addDeviceInfo(dvc);

            } catch (TimeoutException ex) {
                logger.error(idStr + ex.getMessage(), ex);
            }
        }
    }

    public void writeStats() {
        int size = latencies.size();

        if (size > 0) {
            // process latencies
            long minLat;
            long maxLat;
            long sumLat;
            minLat = maxLat = sumLat = (Long) latencies.get(0);
            for (int i = 1; i < size; i++) {
                long currentLat = (Long) latencies.get(i);
                if (currentLat < minLat) {
                    minLat = currentLat;
                }
                if (currentLat > maxLat) {
                    maxLat = currentLat;
                }
                sumLat += currentLat;
            }

            String port = "unknown";
            if (device != null) {
                port = device.getPort();
                FileWriter fw = device.getFileWriter();
                synchronized (fw) {
                    try {
                        fw.write(port + ";" + size + ";" + minLat + ";" + maxLat + ";" + sumLat + "\n");
                        fw.flush();
                    } catch (IOException ex) {
                        logger.error(idStr + ex.getMessage(), ex);
                    }
                }

            }

            logger.info(idStr + "Svc Port:" + port + "; Num Updates=" + size + "; Latency (ms) - Min=" + minLat + "; Max=" + maxLat + "; Sum=" + sumLat + "\n");
        } else {
            logger.info(idStr + "There are no latencies stored!");
        }
    }

    public static void main(String[] args) {
        DPWSFramework.start(args);
        MembershipRepositoryClient client = null;

        try {
            client = new MembershipRepositoryClient();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            if (client != null) {
                logger.warn("Shutting down...");
            }

            DPWSFramework.stop();
            System.exit(0);
        }

    }
}
