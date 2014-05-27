/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.uminho.di.gsd.membership.device;

import java.io.IOException;
import org.apache.log4j.Logger;
import org.uminho.di.gsd.common.RunConstants;
import org.uminho.di.gsd.common.device.BasicDevice;
import org.uminho.di.gsd.membership.info.MembershipRepository;
import org.uminho.di.gsd.membership.service.MembershipService;
import org.ws4d.java.DPWSFramework;
import org.ws4d.java.communication.HTTPBinding;
import org.ws4d.java.types.URI;

public class MembershipDevice extends BasicDevice {
    static Logger logger = Logger.getLogger(MembershipDevice.class);

    protected MembershipService membershipService;

    public MembershipService getMembershipService() {
        return membershipService;
    }

    public void setMembershipService(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    public void initializeMembershipService() {
        membershipService = new MembershipService();
        membershipService.addBinding(new HTTPBinding(new URI("http://" + IP + ":" + PORT + "/membership/service")));

        this.addService(membershipService);
    }

    @Override
    public void startServices()
    {
        if(membershipService != null)
            startMembershipService();
    }

    protected void startMembershipService() {
        try {
            membershipService.start();
        } catch (IOException ex) {
            logger.error(idStr + ex.getMessage(), ex);
        }
    }

    @Override
    public void stopServices()
    {
        try {
            membershipService.stop();
        } catch (IOException ex) {
            logger.error(idStr + ex.getMessage(), ex);
        }
    }

    public static void main(String[] args) throws Exception {

        if (args.length >= 2) {
            RunConstants constants = new RunConstants(args);

            MembershipDevice device = null;
            
            try {
                // always start the framework first
                DPWSFramework.start(args);

                // create a simple device ...
                device = new MembershipDevice();
                device.setConstants(constants);

                device.initializeBinding();

                // ... and a service
                device.initializeMembershipService();

                device.startDevice();

                // initialize repository
                MembershipRepository repository = new MembershipRepository();
                repository.initializeWithDevice(device);

                logger.info(device.getIdStr() + "Printing created repository\n" + repository.toString());

                device.getMembershipService().setRepository(repository);

                repository = device.getMembershipService().getRepository();
                if(repository != null)
                    logger.info(device.getIdStr() + "Printing set repository\n" + repository.toString());
                else
                    logger.info(device.getIdStr() + "Repository is null!");
            }
            catch (Exception e)
            {
                logger.error(e.getMessage(), e);
            }
        }
//        DPWSFramework.stop();
    }
}
