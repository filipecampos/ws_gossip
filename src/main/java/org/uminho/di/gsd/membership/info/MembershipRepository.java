/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uminho.di.gsd.membership.info;

import java.util.Random;
import org.apache.log4j.Logger;
import org.uminho.di.gsd.common.Configuration;
import org.uminho.di.gsd.common.Constants;
import org.uminho.di.gsd.common.device.BasicDevice;
import org.uminho.di.gsd.membership.client.MembershipRepositoryClient;
import org.ws4d.java.service.Device;
import org.ws4d.java.service.reference.DeviceReference;
import org.ws4d.java.service.reference.ServiceReference;
import org.ws4d.java.structures.ArrayList;
import org.ws4d.java.structures.DataStructure;
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.structures.List;
import org.ws4d.java.structures.ListIterator;
import org.ws4d.java.types.EndpointReference;
import org.ws4d.java.types.QName;
import org.ws4d.java.types.URI;

/**
 *
 * @author fcampos
 */
public class MembershipRepository {

    static Logger logger = Logger.getLogger(MembershipRepository.class);
    private String idStr = "";
    // random generator
    private Random random;
    // This Membership Service URI
    private URI thisMembershipServiceEPR;
//    private URI thisGossipServiceEPR;
    // key -> URI - device endpointAddressReference, value -> DeviceInfo
    private HashMap devices;
    // key -> URI - service endpointAddressReference, value -> ServiceInfo
    private HashMap membershipServices;
    // key -> URI - service endpointAddressReference, value -> ServiceInfo
    private HashMap gossipServices;
    private BasicDevice device;
    private MembershipRepositoryClient client;

    private String[] targetIps;
    private int[] targetPorts;

    public MembershipRepository() {
        random = new Random(System.nanoTime());
        devices = new HashMap();
        membershipServices = new HashMap();
        gossipServices = new HashMap();
    }

    public String[] getTargetIps() {
        return targetIps;
    }

    public int[] getTargetPorts() {
        return targetPorts;
    }

    public URI getThisMembershipServiceEPR() {
        return thisMembershipServiceEPR;
    }

    public void setThisMembershipServiceEPR(URI thisMembershipServiceEPR) {
        this.thisMembershipServiceEPR = thisMembershipServiceEPR;
    }

    public void setClient(MembershipRepositoryClient client) {
        this.client = client;
    }

    public BasicDevice getDevice() {
        return device;
    }

    public void initializeWithDevice(BasicDevice device) {
        this.device = device;

        idStr = "[Device" + device.getPort() + "]";
        addDeviceInfo(device);
    }

    public DeviceInfo addDeviceInfo(Device device) {
        DeviceInfo devInfo = null;

        if(device != null)
        {
            devInfo = new DeviceInfo(device);
            URI devEPR = devInfo.getEndpointReference();
            devices.put(devEPR, devInfo);

            getHostedServicesInfo(device, devInfo);
        }

        return devInfo;
    }

    private void getHostedServicesInfo(Device dvc, DeviceInfo devInfo) {
        // get device EPR
        URI deviceEPR = dvc.getEndpointReference().getAddress();

        // get services EPRs
        Iterator svcRefs = dvc.getServiceReferences();
        ServiceReference svcRef = null;
        URI preferredXAddress = null;
        if (svcRefs != null) {
            while (svcRefs.hasNext()) {
                svcRef = (ServiceReference) svcRefs.next();
                // get declared preferred xaddress
                preferredXAddress = svcRef.getPreferredXAddress();

                // otherwise get first of endpoint addresses as preferred
                if (preferredXAddress == null) {
                    Iterator eprs = svcRef.getEndpointReferences();

                    if (eprs.hasNext()) {
                        preferredXAddress = ((EndpointReference) eprs.next()).getAddress();
                    }
                }
                logger.debug(idStr + "Adding Service " + preferredXAddress + " to repo");
                devInfo.addServiceAddress(preferredXAddress);

                ServiceInfo si = new ServiceInfo(svcRef, deviceEPR);
                List svcTypes = si.getServiceTypes();

                if (logger.isDebugEnabled()) {
                    ListIterator lIter = svcTypes.listIterator();
                    StringBuilder sb = new StringBuilder(idStr);
                    sb.append("Service ");
                    sb.append(preferredXAddress);
                    sb.append(" - PortTypes:\n");
                    while (lIter.hasNext()) {
                        sb.append(lIter.next());
                        sb.append('\n');
                    }
                    logger.debug(sb.toString());
                }

                // every gossip service must have the push port
                if (svcTypes.contains(Constants.GossipPushPortQName)) {
                    addInfoToHash(gossipServices, si, preferredXAddress);

                    logger.debug(idStr + "Added GossipService: " + preferredXAddress);
                } else if (svcTypes.contains(Constants.MembershipPortTypeQName)) {
                    addInfoToHash(membershipServices, si, preferredXAddress);

                    // initializing with the first known membershipServiceEPR
                    if (getThisMembershipServiceEPR() == null) {
                        setThisMembershipServiceEPR(preferredXAddress);
                        logger.debug(idStr + "MembershipService: " + getThisMembershipServiceEPR());
                    }
                } else {
                    // other type of service
                    logger.warn(idStr + "Found service of unknown type" + preferredXAddress);
                    // TODO: what should we do? initialize into an hash of generic services?
                }
            }
        } else {
            logger.warn(idStr + "Has no hosted services!");
        }

        if (getThisMembershipServiceEPR() == null) {
            setThisMembershipServiceEPR(new URI((String) Configuration.getConfigParamValue(Configuration.monitoringService)));
            logger.debug(idStr + "MembershipService: " + getThisMembershipServiceEPR());
        }


    }

    // return num gossip services to be targetted by requesting service
    public List getTargets(int num, QName serviceType, URI requestingGossipServiceURI) {
        List targets = null;
        // for mock purposes:
//        Object mockObject = new Object();
//        int n = 10;
//        int j = 0;
//        while(j < n)
//        {
//            gossipServices.put("http://service" + j + "/gossip", mockObject);
//            j++;
//        }

        logger.debug("getTargets:num=" + num + ";serviceType=" + serviceType + ";requestingGossipServiceURI=" + requestingGossipServiceURI);

        // TODO use serviceType to select targets that are not Gossip Services -> add new hash for these services
        List services = new ArrayList(gossipServices.keySet());

        logger.debug("getTargets:Known gossip services=" + services.size());

        if (requestingGossipServiceURI != null) {
            // do not send service its own EPR
            if (services.contains(requestingGossipServiceURI)) {
                services.remove(requestingGossipServiceURI);
            } else {
                // create service info for requesting service
                ServiceReference svcRef = client.getServiceReference(new EndpointReference(requestingGossipServiceURI));
                // Verify if service exists before inserting in the repository
                if (!gossipServices.containsKey(requestingGossipServiceURI)) {
                    ServiceInfo svcInfo = new ServiceInfo(svcRef, requestingGossipServiceURI);
                    addService(requestingGossipServiceURI, svcInfo);
                }
            }

            // update heartbeat of requesting service
            ServiceInfo requestingServiceSI = (ServiceInfo) getServiceInfo(requestingGossipServiceURI);
            if (requestingServiceSI != null) {
                // service unknown
                requestingServiceSI.heartbeat();
            }

            int numServices = services.size();

            if (num >= numServices) {
                targets = services;
            } else {
                targets = new ArrayList(num);
                List temp = new ArrayList(services);

                for (int i = 0; i < num; i++) {
                    int index = random.nextInt(temp.size());

                    targets.add(temp.remove(index));
                }
            }
        }

        // update this service's heartbeat
        updateMembershipServiceHeartbeat();

        return targets;
    }

    public URI selectUpdateTarget() {
        URI target = null;
        List services = new ArrayList(membershipServices.keySet());

        services.remove(getThisMembershipServiceEPR());

        int size = services.size();
        if (size > 0) {
            int index = random.nextInt(size);
            target = (URI) services.get(index);
        }

        return target;
    }

    // return list with all the info from repository to be exchanged in update
    public synchronized List preUpdateList() {
        List list = new ArrayList();

        // first update this membership service heartbeat
        updateMembershipServiceHeartbeat();

        list.addAll(gossipServices.values());
        list.addAll(membershipServices.values());

        return list;
    }

    // merge received info and return information that is more recent or inexistent
    public synchronized List update(List info) {
        List queue = new ArrayList();
        List processedQueue = new ArrayList();

        updateMembershipServiceHeartbeat();

        Iterator infos = info.iterator();
        while (infos.hasNext()) {
            ServiceInfo si = (ServiceInfo) infos.next();
            URI svcAddr = si.getPreferredXAddress();
            processedQueue.add(svcAddr);

            ServiceInfo ownSi = processReceivedServiceInfo(si);
            if (ownSi != null) {
                queue.add(ownSi);
            }
        }

        // add remaining services information
        queue.addAll(addUnprocessedServicesInfo(membershipServices, processedQueue));
        queue.addAll(addUnprocessedServicesInfo(gossipServices, processedQueue));

        return queue;
    }

    // merge info received as update response
    public synchronized void merge(List info) {
        Iterator infos = info.iterator();
        ServiceInfo si = null;
        while (infos.hasNext()) {
            si = (ServiceInfo) infos.next();
            processReceivedServiceInfo(si);
        }
    }

    private ServiceInfo processReceivedServiceInfo(ServiceInfo si) {
        ServiceInfo ret = null;
        URI svcAddr = si.getPreferredXAddress();
        ServiceInfo ownSi = getServiceInfo(svcAddr);

        if (ownSi != null) {
            // service is already monitored
            boolean older = ownSi.update(si);

            // if received SI is older than a possessed one, add corresponding si to the returning queue
            if (older) {
                logger.debug(idStr + "Received update for service " + ownSi.getPreferredXAddress() + " is older!");
                ret = ownSi;
            } else {
                logger.debug(idStr + "Received update for service " + ownSi.getPreferredXAddress() + " is newer! receivedHB=" + si.getHeartbeat() + "; ownHB=" + ownSi.getHeartbeat());
            }

        } else {
            // service not monitored before, so add it to repository
            addService(svcAddr, si);
            // no need to add to queue since it was received from the caller of the update
        }

        return ret;
    }

    private void updateMembershipServiceHeartbeat() {
        // update this service's heartbeat
        ServiceInfo thisSvcInfo = (ServiceInfo) membershipServices.get(getThisMembershipServiceEPR());
        if (thisSvcInfo == null) {
            logger.error(idStr + "MembershipService SI is NULL! ERROR!" + toString());
        } else {
            thisSvcInfo.heartbeat();
        }
    }

    public ServiceInfo getServiceInfo(URI addr) {
        ServiceInfo si = null;

        if (membershipServices.containsKey(addr)) {
            si = (ServiceInfo) membershipServices.get(addr);
        } else if (gossipServices.containsKey(addr)) {
            si = (ServiceInfo) gossipServices.get(addr);
        }

        return si;
    }

    private void addService(URI addr, ServiceInfo si) {
        si.updateTimestamp();

        boolean serviceToBeMonitored = true;

        if (si.isGossipService()) {
            gossipServices.put(addr, si);
        } else if (si.isMembershipService()) {
            membershipServices.put(addr, si);
        } else {
            // TODO monitor other types of services
            serviceToBeMonitored = false;
            logger.error(idStr + "Service with URI " + addr + " isn't a gossip or membership service!");
            List svcTypes = si.getServiceTypes();
            int size = svcTypes.size();

            for (int i = 0; i < size; i++) {
                logger.error(idStr + "Type [" + i + "]:" + svcTypes.get(i));
            }
        }

        if (serviceToBeMonitored) {
            addServiceToDevice(addr, si.getHostingDeviceEndpointAddress());
        }
    }

    private List addUnprocessedServicesInfo(HashMap services, List processed) {
        // Complexity = <services>*<processed>
/*        List ret = new ArrayList();

        Set keys = services.keySet();

        Iterator addrs = keys.iterator();
        Object addr = null;
        while (addrs.hasNext()) {
        addr = addrs.next();
        if (!processed.contains(addr)) {
        ret.add(services.get(addr));
        }
        }
         */
        // could be done differently
        // Complexity = <processed>+<services>
        List ret = new ArrayList();

        List keys = new ArrayList(services.keySet());

        Iterator proc = processed.iterator();
        Object procAddr = null;
        while (proc.hasNext()) {
            procAddr = proc.next();

            keys.remove(procAddr);
        }

        Iterator addrs = keys.iterator();
        Object addr = null;
        while (addrs.hasNext()) {
            addr = addrs.next();
            ret.add(services.get(addr));
        }

        return ret;
    }

    private void addServiceToDevice(URI svcURI, URI hostingDeviceReference) {
        DeviceInfo dvc = (DeviceInfo) devices.get(hostingDeviceReference);

        // device is unknown so add it
        if (dvc == null) {
            dvc = addDeviceInfo(client.getDevice(hostingDeviceReference));
        }

        // add service to device's hosted services
        if (!dvc.getHostedServices().contains(svcURI)) {
            dvc.getHostedServices().add(svcURI);
        }
    }

    private void addInfoToHash(HashMap repo, AbstractInfo ai, URI key) {
        if (!repo.containsKey(key)) {
            repo.put(key, ai);
        }
    }

    public void removeDeviceInfo(DeviceReference devRef) {
        URI devRefURI = devRef.getEndpointReference().getAddress();

        if (devices.containsKey(devRefURI)) {
            DeviceInfo devInfo = (DeviceInfo) devices.remove(devRefURI);
            List hostedServices = devInfo.getHostedServices();
            Iterator iter = hostedServices.iterator();

            while (iter.hasNext()) {
                URI svcAddr = (URI) iter.next();

                removeServiceInfoFromRepo(svcAddr);
            }
        }
    }

    public void removeServiceInfo(URI serviceAddress) {
        ServiceInfo si = removeServiceInfoFromRepo(serviceAddress);

        if (si != null) {
            DeviceInfo di = (DeviceInfo) devices.get(si.getHostingDeviceEndpointAddress());
            di.removeService(serviceAddress);
        }
    }

    private ServiceInfo removeServiceInfoFromRepo(URI svcAddr) {
        ServiceInfo si = null;
        if (gossipServices.containsKey(svcAddr)) {
            si = (ServiceInfo) gossipServices.remove(svcAddr);
        } else if (membershipServices.containsKey(svcAddr)) {
            si = (ServiceInfo) membershipServices.remove(svcAddr);
        }

        return si;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(idStr);
        sb.append("MembershipRepository:\n");

        // this membership service
        sb.append("\nSelf MembershipService EPR:");
        sb.append(getThisMembershipServiceEPR());

        // devices
        sb.append("\n\n\nAll Devices:\n");
        DeviceInfo devInfo = null;
        DataStructure dvcs = devices.values();
        sb.append("Repository has ");
        sb.append(dvcs.size());
        sb.append(" devices.\n");
        Iterator dvcIter = dvcs.iterator();
        List hostedServices = null;
        Iterator hostedServicesIter = null;
        URI svcEPR = null;
        String msg = null;
        while (dvcIter.hasNext()) {
            devInfo = (DeviceInfo) dvcIter.next();

            if (devInfo.getEndpointReference().equals(device.getEndpointReference().getAddress())) {
                sb.append("This device:");
            }

            sb.append(devInfo.toString());

            hostedServices = devInfo.getHostedServices();
            sb.append("\nThis device hosts ");
            sb.append(hostedServices.size());
            sb.append(" services.\n");

            if ((hostedServices != null) && (!hostedServices.isEmpty())) {
                hostedServicesIter = hostedServices.iterator();

                while (hostedServicesIter.hasNext()) {
                    svcEPR = (URI) hostedServicesIter.next();

                    if (gossipServices.containsKey(svcEPR)) {
                        msg = "\n\n\nGossipRepo:" + printServiceInfo(gossipServices, svcEPR);
                    } else if (membershipServices.containsKey(svcEPR)) {
                        msg = "\n\n\nMembershipRepo:" + printServiceInfo(membershipServices, svcEPR);
                    } else {
                        msg = "The repository has no info on the service with EPR " + svcEPR + "\n";
                    }

                    sb.append(msg);
                }
            }
        }

        return sb.toString();
    }

    private String printServiceInfo(HashMap repo, URI epr) {
        ServiceInfo svcInfo = (ServiceInfo) repo.get(epr);
        return svcInfo.toString();
    }

    public int getNumberOfGossipServices()
    {
        return gossipServices.size();
    }

    public void initRandomly(String ip, int fanout, int ownPort, int basePort, int num)
    {
        int desirable_size = fanout+1;

        if(desirable_size > num)
            desirable_size = num;

        targetIps = new String[fanout];
        targetPorts = new int[fanout];

        java.util.List<Integer> selected = new java.util.ArrayList<Integer>();
        selected.add(ownPort);

        DeviceInfo deviceInfo = null;
        ServiceInfo ownMembershipSI = (ServiceInfo) membershipServices.get(thisMembershipServiceEPR);
        ServiceInfo ownGossipSI = (ServiceInfo) gossipServices.values().iterator().next();

        int curPort = ownPort;
        int counter = 0;
        while(devices.size() < desirable_size)
        {
            while(selected.contains(curPort))
                curPort = basePort + random.nextInt(num);

            deviceInfo = new DeviceInfo(device);
            deviceInfo.setEndpointReference(new URI("urn:uuid:device" + curPort));
            List xAddresses = new ArrayList();
            URI dvcURI = new URI("http://" + ip + ":" + curPort + "/device");
            xAddresses.add(dvcURI);
            deviceInfo.setxAddresses(xAddresses);
            URI membershipService = new URI("http://" + ip + ":" + curPort + "/membership/service");
            URI gossipService = new URI("http://" + ip + ":" + curPort + "/device/gossip/service");
            deviceInfo.addServiceAddress(membershipService);
            deviceInfo.addServiceAddress(gossipService);
            

            targetIps[counter] = ip;
            targetPorts[counter] = curPort;
            counter++;

            //generate SIs
            ServiceInfo membershipSI = new ServiceInfo();
            // device reference
            membershipSI.setHostingDeviceEndpointAddress(dvcURI);
            // service type
            membershipSI.setServiceTypes(ownMembershipSI.getServiceTypes());
            // service reference
            membershipSI.setPreferredXAddress(membershipService);
            // service endpoints
            membershipSI.addEndpointAddress(new EndpointReference(membershipService));


            ServiceInfo gossipSI = new ServiceInfo();
            // device reference
            gossipSI.setHostingDeviceEndpointAddress(dvcURI);
            // service type
            gossipSI.setServiceTypes(ownGossipSI.getServiceTypes());
            // service reference
            gossipSI.setPreferredXAddress(gossipService);
            // service endpoints
            gossipSI.addEndpointAddress(new EndpointReference(gossipService));
            

            devices.put(dvcURI, deviceInfo);
            membershipServices.put(membershipService, membershipSI);
            gossipServices.put(gossipService, gossipSI);

            selected.add(curPort);
        }

        logger.info("Repository " + toString());
    }
}
