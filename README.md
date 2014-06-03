WS-Gossip
=========

Gossip framework for Web Services implemented on [WS4D](http://ws4d.org/ "Web Services for Devices") 
[JMEDS Framework v2.0 beta3a](http://ws4d.e-technik.uni-rostock.de/jmeds/ "WS4D.org Java Multi Edition DPWS Stack")

Build files will be uploaded soon.


## Release History

* **0.1**: Initial release. (26 May, 2014)


## Installing and using WS-Gossip 

Follow the instructions on the INSTALL file to build the binary distribution package.
It will be located at ./target/ws_gossip-0.1-bin.zip.
Extract the contents of the zip file and follow the following instructions to execute a Gossip device.

- To run a UDP Push Gossip device:

 $ ./bin/udp_push_gossip_device <port> <ip> <lowest_port_number> <run_number> <producers_number> <total_devices> <iters> <period(ms)> <fanout> <dissemination_type>


- To run a TCP Push Gossip device:

 $ ./bin/push_gossip_device <port> <ip> <lowest_port_number> <run_number> <producers_number> <total_devices> <iters> <period(ms)> <fanout> <dissemination_type>