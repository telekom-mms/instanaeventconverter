/*Copyright 2023 Deutsche Telekom MMS GmbH (https://www.t-systems-mms.com/) 

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Author: Kay Koedel
*/

package de.telekom.mms.apm.instanaeventconverter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.snmp4j.CommandResponder;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.PDUv1;
import org.snmp4j.ScopedPDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.UserTarget;
import org.snmp4j.mp.MPv3;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.AuthHMAC128SHA224;
import org.snmp4j.security.AuthHMAC192SHA256;
import org.snmp4j.security.AuthHMAC256SHA384;
import org.snmp4j.security.AuthHMAC384SHA512;
import org.snmp4j.security.AuthMD5;
import org.snmp4j.security.AuthSHA;
import org.snmp4j.security.Priv3DES;
import org.snmp4j.security.PrivAES128;
import org.snmp4j.security.PrivAES192;
import org.snmp4j.security.PrivAES256;
import org.snmp4j.security.PrivDES;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.SecurityModels;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.security.USM;
import org.snmp4j.security.UsmUser;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.IpAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.TimeTicks;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;


/**
 * @author kao
 *
 */
public class ConverterSNMP {

	private static final Logger log = LogManager.getLogger("standard");
	private String snmpip = Config.getInstance().getValue("snmp_event_ip");
	private int port = Integer.valueOf(Config.getInstance().getValue("snmp_event_port")).intValue();
	private String snmpVersion = Config.getInstance().getValue("snmp_version");
	private String snmpSecurityName = Config.getInstance().getValue("snmp_securityname");
	private String snmpAuthPassphrase = Config.getInstance().getValue("snmp_authpassphrase");
	private String snmpPrivPassphrase = Config.getInstance().getValue("snmp_privpassphrase");
	private String snmpconverterSNMPEngineID = Config.getInstance().getValue("snmp_engineid");
	private String snmpPrivCypher = Config.getInstance().getValue("snmp_privcypher");
	private String snmpAuthCypher = Config.getInstance().getValue("snmp_authcypher");
	private String trapOid = Config.getInstance().getValue("snmp_oid");
	private String extraOid = Config.getInstance().getValue("snmp_extra_oid");
	
	private ExternalEventObject externalEventObject;

	public static final String community = "public";


	public void postEvent2Converter() {

		if (snmpVersion.equals("1")) {
			sendSnmpV1Trap();
		}

		if (snmpVersion.equals("2c")) {
			sendSnmpV2Trap();

		}
		if (snmpVersion.equals("3")) {
			sendSnmpV3Trap();

		}
		if (snmpVersion.equals("3Command")) {
			sendSnmpV3TrapCommand();

		}

	}

	public void sendSnmpV1Trap() {
		try {
			// Create Transport Mapping
			TransportMapping<?> transport = new DefaultUdpTransportMapping();
			transport.listen();

			// Create Target
			CommunityTarget<UdpAddress> comtarget = new CommunityTarget<UdpAddress>();
			comtarget.setCommunity(new OctetString(community));
			comtarget.setVersion(SnmpConstants.version1);
			comtarget.setAddress(new UdpAddress(snmpip + "/" + port));
			comtarget.setRetries(2);
			comtarget.setTimeout(5000);

			// Create PDU for V1
			PDUv1 pdu = new PDUv1();
			pdu.setType(PDU.V1TRAP);
			pdu.setEnterprise(new OID(trapOid));
			pdu.setGenericTrap(PDUv1.ENTERPRISE_SPECIFIC);
			pdu.setSpecificTrap(1);
			pdu.setAgentAddress(new IpAddress(snmpip));
			pdu.add(new VariableBinding(new OID(trapOid), new OctetString(externalEventObject.getValuesForSNMP())));
			long sysUpTime = 111111;
			pdu.setTimestamp(sysUpTime);

			// Send the PDU
			Snmp snmp = new Snmp(transport);
			log.info("Sending V1 Trap to " + snmpip);
			snmp.send(pdu, comtarget);
			snmp.close();
		} catch (Exception e) {
			log.error("Error in Sending V1 Trap to " + snmpip);
			log.error("Exception Message = " + e.getMessage());
		}
	}

	/**
	 * This methods sends the V2 trap
	 */
	public void sendSnmpV2Trap() {
		try {
			// Create Transport Mapping
			TransportMapping<?> transport = new DefaultUdpTransportMapping();
			transport.listen();

			// Create Target
			CommunityTarget<UdpAddress> comtarget = new CommunityTarget<UdpAddress>();
			comtarget.setCommunity(new OctetString(community));
			comtarget.setVersion(SnmpConstants.version2c);
			comtarget.setAddress(new UdpAddress(snmpip + "/" + port));
			comtarget.setRetries(2);
			comtarget.setTimeout(5000);

			// Create PDU for V2
			PDU pdu = new PDU();

			// need to specify the system up time
			long sysUpTime = 111111;
			pdu.add(new VariableBinding(SnmpConstants.sysUpTime, new TimeTicks(sysUpTime)));

			pdu.add(new VariableBinding(SnmpConstants.snmpTrapOID, new OID(trapOid)));
			pdu.add(new VariableBinding(SnmpConstants.snmpTrapAddress, new IpAddress(snmpip)));

			// variable binding for Enterprise Specific objects, Severity (should be defined
			// in MIB file)
			pdu.add(new VariableBinding(new OID(trapOid), new OctetString(externalEventObject.getValuesForSNMP())));

			pdu.setType(PDU.NOTIFICATION);

			// Send the PDU
			Snmp snmp = new Snmp(transport);
			log.info("Sending V2 Trap to " + snmpip + " on Port " + port);
			snmp.send(pdu, comtarget);
			snmp.close();
		} catch (Exception e) {
			log.error("Error in Sending V2 Trap to " + snmpip + " on Port " + port);
			log.error("Exception Message = " + e.getMessage());
		}
	}

	
	/**
	 * Sends the v3 trap
	 */
	private void sendSnmpV3Trap() {
		try {
			Address targetAddress = GenericAddress.parse("udp:" + snmpip + "/" + port);
			TransportMapping<?> transport = new DefaultUdpTransportMapping();
			Snmp snmp = new Snmp(transport);

			SecurityProtocols.getInstance().addDefaultProtocols();
			SecurityProtocols.getInstance().addAuthenticationProtocol(new AuthSHA());
			SecurityProtocols.getInstance().addAuthenticationProtocol(new AuthMD5());
			SecurityProtocols.getInstance().addAuthenticationProtocol(new AuthHMAC128SHA224());
			SecurityProtocols.getInstance().addAuthenticationProtocol(new AuthHMAC192SHA256());
			SecurityProtocols.getInstance().addAuthenticationProtocol(new AuthHMAC256SHA384());
			SecurityProtocols.getInstance().addAuthenticationProtocol(new AuthHMAC384SHA512());
			SecurityProtocols.getInstance().addPrivacyProtocol(new Priv3DES());
			SecurityProtocols.getInstance().addPrivacyProtocol(new PrivAES128());
			SecurityProtocols.getInstance().addPrivacyProtocol(new PrivAES192());
			SecurityProtocols.getInstance().addPrivacyProtocol(new PrivAES256());


			
			OctetString securityName = new OctetString(snmpSecurityName);
			OID authProtocol = getAuth(snmpAuthCypher);
			OID privProtocol = getPriv(snmpPrivCypher);
			OctetString authPassphrase = new OctetString(snmpAuthPassphrase);
			OctetString privPassphrase = new OctetString(snmpPrivPassphrase);
			
	        OctetString localEngineId = new OctetString(
					MPv3.createLocalEngineID(new OctetString(snmpconverterSNMPEngineID)));
			
			USM usm = new USM(SecurityProtocols.getInstance(),localEngineId,0);
			SecurityModels.getInstance().addSecurityModel(usm);

			transport.listen();

			
			snmp.getUSM().addUser(securityName, new UsmUser(securityName, authProtocol,
							authPassphrase, privProtocol, privPassphrase));
			
			// thats important
			snmp.setLocalEngine(localEngineId.getValue(), 0, 0);
			
			log.debug("securityName: " + securityName.toString());
			log.debug("Auth OID: " + authProtocol.toString());
			log.debug("Priv OID: " + privProtocol.toString());
			log.debug("authPassphrase: " + authPassphrase.toString());
			log.debug("privPassphrase: " + privPassphrase.toString());
			log.info("localEngineId: " + localEngineId.toString());		
			log.debug("targetAddress: " + targetAddress.toString());		
			
			
			// Create Target
			UserTarget<Address> target = new UserTarget<Address>();
			target.setAddress(targetAddress);
			target.setRetries(1);
			target.setTimeout(11500);
			target.setVersion(SnmpConstants.version3);
			target.setSecurityLevel(SecurityLevel.AUTH_PRIV);
			target.setSecurityModel(MPv3.ID);
			target.setSecurityName(securityName);
			target.setAuthoritativeEngineID(localEngineId.getValue());

			
			// Create PDU for V3
			ScopedPDU pdu = new ScopedPDU();
			pdu.setType(ScopedPDU.TRAP);
			pdu.setContextEngineID(localEngineId);
			pdu.setRequestID(new Integer32(1234));
			pdu.add(new VariableBinding(SnmpConstants.sysUpTime));
			pdu.add(new VariableBinding(SnmpConstants.snmpTrapOID, SnmpConstants.linkDown));
			pdu.add(new VariableBinding(new OID(trapOid), new OctetString(externalEventObject.getValuesForSNMP())));


			// Send the PDU
			snmp.send(pdu, target);
			log.info("Sending Trap to (IP:Port)=> " + snmpip + ":" + port);
			snmp.addCommandResponder(new CommandResponder() {
				public void processPdu(CommandResponderEvent arg0) {
					log.debug(arg0);
				}
			});
			snmp.close();
		} catch (Exception e) {
			log.error("Error in Sending Trap to (IP:Port)=> " + snmpip + ":" + port);
			log.error("Exception Message = " + e.getMessage());

			
		}
	}

	/*
	 * Example snmptrap -v3 -e 0000000000 -l authPriv -u testtest -a SHA -A testtest
	 * -x AES -X testtest 127.0.0.1:162 '' 1.3.6.1.4.1 1.3.6.1 s "testv3"
	 */
	public void sendSnmpV3TrapCommand() {
		try {
			String command = null;
			command = "/usr/bin/snmptrap -v3 -e " + snmpconverterSNMPEngineID + " -l authPriv -u " + snmpSecurityName
					+ " -a " + snmpAuthCypher + " -A " + snmpAuthPassphrase + " -x " + snmpPrivCypher + " -X "
					+ snmpPrivPassphrase + " " + snmpip + ":" + port + " '' " + trapOid + " " + extraOid + " s \'"
					+ externalEventObject.getValuesForSNMP() + "\'";

			log.info("snmpv3command: " + command);
			Process process = null;
			String filename;
			process = Runtime.getRuntime().exec(command);
			try {
				if (process.waitFor() == 0) {
					log.debug("waitfor is over");
					InputStream is = process.getInputStream();
					InputStreamReader isr = new InputStreamReader(is);
					BufferedReader br = new BufferedReader(isr);
					while ((filename = br.readLine()) != null) {
						log.debug(filename);

					}
				}
			} catch (InterruptedException e) {
				log.error(e.toString());
			}

		} catch (

		IOException e) {
			log.error("Something happen: " + e.getMessage());
		}
	}

	public void setAdatperEventObject(ExternalEventObject object) {
		externalEventObject = object;

	}
	
	/**
	 * Method to return the private protocol given the property
	 * @param privProtocol property
	 * @return protocol
	 */
	public static OID getPriv(String privProtocol) {
	    switch (privProtocol) {
	    case "DES":
	        return PrivDES.ID;
	    case "3DES":
	        return Priv3DES.ID;
	    case "AES128":
	        return PrivAES128.ID;
	    case "AES192":
	        return PrivAES192.ID;
	    case "AES256":
	        return PrivAES256.ID;
	    default:
	        return null;
	    }
	}
	
	/**
	 * Method to return the auth protocol given the property
	 * @param authProtocol property
	 * @return protocol
	 */
	public static OID getAuth(String authProtocol) {
		switch (authProtocol) {
	    case "SHA":
	        return AuthSHA.ID;
	    case "MD5":
	        return AuthMD5.ID;
	    default:
	        return null;
	    }
	}


}
