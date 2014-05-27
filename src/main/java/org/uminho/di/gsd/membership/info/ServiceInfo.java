/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uminho.di.gsd.membership.info;

import org.apache.log4j.Logger;
import org.uminho.di.gsd.common.Constants;
import org.ws4d.java.communication.TimeoutException;
import org.ws4d.java.service.parameter.ParameterValue;
import org.ws4d.java.service.reference.ServiceReference;
import org.ws4d.java.structures.ArrayList;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.structures.List;
import org.ws4d.java.types.EndpointReference;
import org.ws4d.java.types.QName;
import org.ws4d.java.types.URI;

/**
 *
 * @author fcampos
 */
public class ServiceInfo extends AbstractInfo {

    static Logger logger = Logger.getLogger(ServiceInfo.class);
    
    // service
    private URI serviceId;
    private List serviceTypes;
    private List endpointAddresses;
    // preferred endpoint address -> used to uniquely identify the service
    private URI preferredXAddress;
    // device identifier
    private URI hostingDeviceEndpointAddress;

    public ServiceInfo() {
        this.serviceTypes = new ArrayList();
        this.endpointAddresses = new ArrayList();

        setHeartbeat(0);
    }

    public ServiceInfo(ServiceReference svcRef, URI dvcEndpointAddress) {
        serviceId = svcRef.getServiceId();
        if(svcRef.getLocation() == ServiceReference.LOCATION_UNKNOWN)
            try {
                serviceTypes = new ArrayList(svcRef.getService().getPortTypes());
            } catch (TimeoutException ex) {
                logger.error(ex);
                // assume push gossip if using a mock service url
                serviceTypes = new ArrayList();
                serviceTypes.add(Constants.GossipPushPortQName);
            }
        else
            serviceTypes = new ArrayList(svcRef.getPortTypes());

        if(serviceTypes.size() == 0)
        {
            logger.error("Service has no ServiceType!");
            throw new NullPointerException("Service has no ServiceType!");
        }

        endpointAddresses = new ArrayList(svcRef.getEndpointReferences());

        preferredXAddress = svcRef.getPreferredXAddress();
        if (preferredXAddress == null) {
            if (endpointAddresses.size() > 0) {
                preferredXAddress = ((EndpointReference) endpointAddresses.get(0)).getAddress();
            } else {
                preferredXAddress = new URI("Error! No epr defined!");
            }
        }

        this.hostingDeviceEndpointAddress = dvcEndpointAddress;

        setHeartbeat(0);
    }

    public void setPreferredXAddress(URI preferredXAddress) {
        this.preferredXAddress = preferredXAddress;
    }

    public void addEndpointAddress(EndpointReference endpointAddress) {
        this.endpointAddresses.add(endpointAddress);
    }

    public void setHostingDeviceEndpointAddress(URI hostingDeviceEndpointAddress) {
        this.hostingDeviceEndpointAddress = hostingDeviceEndpointAddress;
    }

    public void setServiceId(URI serviceId) {
        this.serviceId = serviceId;
    }

    public void addServiceType(QName serviceType) {
        this.serviceTypes.add(serviceType);
    }

    public List getEndpointAddresses() {
        return endpointAddresses;
    }

    public URI getHostingDeviceEndpointAddress() {
        return hostingDeviceEndpointAddress;
    }

    public URI getPreferredXAddress() {
        return preferredXAddress;
    }

    public URI getServiceId() {
        return serviceId;
    }

    public List getServiceTypes() {
        return serviceTypes;
    }

    public void setServiceTypes(List serviceTypes) {
        this.serviceTypes = serviceTypes;
    }

    public boolean update(ServiceInfo si) {
        logger.debug("siownSi.heartbeat=" + this.heartbeat + "; si.heartbeat=" + si.getHeartbeat());
        boolean older = false;

        long hb = si.getHeartbeat();
        if (hb > heartbeat) {
            setHeartbeat(hb);
        } else if (hb < heartbeat) {
            older = true;
        }

        return older;
    }

    public boolean isGossipService() {
        return serviceTypes.contains(Constants.GossipPushPortQName);
    }

    public boolean isMembershipService() {
        return serviceTypes.contains(Constants.MembershipPortTypeQName);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n\nService:");

        if(serviceId != null)
        {
            sb.append("\nId-");
            sb.append(serviceId.toString());
        }

        sb.append("\nTypes:\n");
        Iterator iter = serviceTypes.iterator();
        while (iter.hasNext()) {
            sb.append(iter.next());
            sb.append('\n');
        }

        sb.append("EPRs:\n");
        iter = endpointAddresses.iterator();
        while (iter.hasNext()) {
            sb.append(iter.next());
            sb.append('\n');
        }

        sb.append("Preferred EPR-");
        sb.append(preferredXAddress.toString());

        sb.append(super.toString());

        return sb.toString();
    }

    public boolean sameService(ServiceInfo si) {
        return ((this.preferredXAddress.equals(si.preferredXAddress))
                && (this.hostingDeviceEndpointAddress.equals(si.hostingDeviceEndpointAddress)));
    }

    public boolean equals(ServiceInfo si) {
        return (sameService(si) && (this.heartbeat == si.heartbeat));
    }

    public static List fromPVToServiceInfoList(ParameterValue pv) throws Exception {
        int numSvc = pv.getChildrenCount("Svc");
        logger.debug("PV has " + numSvc + " Svc children!");

        List list = new ArrayList(numSvc);
        ServiceInfo si = null;
        int i = 0;
        while (i < numSvc) {
            si = new ServiceInfo();

            String prefix = "Svc[" + i + "]/";

            // device reference
            String devRefURI = pv.getValue(prefix + "DevRef");
            si.setHostingDeviceEndpointAddress(new URI(devRefURI));

            // service type
            String svcTypeQName = pv.getValue(prefix + "Type");
            if((svcTypeQName == null) || (svcTypeQName.isEmpty()))
            {
                logger.error("Creating ServiceInfo for service without type!");

//                throw new Exception("Creating ServiceInfo for service without type!");
            }
            else
                si.addServiceType(QName.construct(svcTypeQName));
            
            // service reference
            String svcRefURI = pv.getValue(prefix + "Ref");
            si.setPreferredXAddress(new URI(svcRefURI));

            // service endpoints
            String svcEndpointURI = pv.getValue(prefix + "Addr");
            si.addEndpointAddress(new EndpointReference(new URI(svcEndpointURI)));

            // service heartbeat
            String heartbeatLong = pv.getValue(prefix + "Heartbeat");
            si.setHeartbeat(Long.parseLong(heartbeatLong));

            // Add serviceInfo to list
            list.add(si);

            i++;
        }
        
        return list;
    }

    public static ParameterValue fromServiceInfoListToPV(List sis, ParameterValue pv)
    {
        Iterator iter = sis.iterator();
        int i = 0;
        ServiceInfo si = null;
        while(iter.hasNext())
        {
            si = (ServiceInfo) iter.next();

            if(si != null)
            {
                String prefix = "Svc[" + i + "]/";

                // device reference
                URI devRef = si.getHostingDeviceEndpointAddress();
                pv.setValue(prefix + "DevRef", devRef.toString());

                // service type
                // TODO corrigir type inexistente! -> verificar se corrigido
                QName svcType = (QName) si.getServiceTypes().get(0);
                pv.setValue(prefix + "Type", svcType.getNamespace() + "/" + svcType.getLocalPart());

                // service reference
                URI svcRef = si.getPreferredXAddress();
                pv.setValue(prefix + "Ref", svcRef.toString());

                // service endpoints
                EndpointReference epr = (EndpointReference) si.getEndpointAddresses().get(0);
                pv.setValue(prefix + "Addr", epr.getAddress().toString());

                // service heartbeat
                long heartbeat = si.getHeartbeat();
                pv.setValue(prefix + "Heartbeat", Long.toString(heartbeat));

                i++;
            }
            else
                logger.error("Error! si is null!");
        }
        return pv;
    }
}
