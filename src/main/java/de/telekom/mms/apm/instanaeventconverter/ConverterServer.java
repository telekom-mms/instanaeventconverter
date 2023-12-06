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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
/**
 * 
 * This class 
 * * starting the jetty 
 * * reading the config file
 * * reading the mappingfile
 * 
 * @author Kay Koedel
 * 
 */
public class ConverterServer {

	private static final Logger log = LogManager.getLogger("standard");
	private final Properties properties = new Properties();
	private IncidentHandler incidentHandler = new IncidentHandler();
	
	public void start() {
		readConfig();

		Server server = newJettyServer();
		incidentHandler.setFieldmapping(Config.getInstance().readFieldMapping());
		incidentHandler.setVServiceMapping(Config.getInstance().readVServiceMapping());

		
		if (properties.get("reload_serviceapplicationmapping").equals("true")) {
			incidentHandler.setServicesApplicationMapping(Config.getInstance().createApplicationsMappingFromInstana());			
		}
		else {
			incidentHandler.setServicesApplicationMapping(Config.getInstance().readServicesApplicationsMapping());			
		}	
		server.setHandler(incidentHandler);

		try {
			server.start();
			log.info("Starting InstanaEventConverter complete ");
			log.info("*************************************************************************");
			server.join();
		}
		catch (Exception e) {
			log.error(e);
		}
	}

	
	public void readConfig() {

		InputStream input = null;
		try {

			input = this.getClass()
					.getResourceAsStream("/META-INF/maven/com.tsystems.mms.apm.InstanaEventConverter/InstanaEventConverter/pom.properties");
			properties.load(input);
			if (properties.containsKey("version")) {
				log.info("Version: " + properties.getProperty("version"));
			}
		} catch (Exception e) {
			log.error(e);
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					log.error(e);
				}
			}
		}

		try {

			input = new FileInputStream("config/config.properties");
			
			// load a properties file
			properties.load(input);

			Config.getInstance().setConfig(properties);
			
			// get the property value and print it out
			log.info("http_port: " + properties.getProperty("http_port"));
			log.info("https_port: " + properties.getProperty("https_port"));
			log.info("enable_ssl: " + properties.getProperty("enable_ssl"));
			log.info("keystore_path: " + properties.getProperty("keystore_path"));
			log.debug("keystore_pass: " + properties.getProperty("keystore_pass"));

			log.info("api: " + properties.getProperty("api"));

			log.info("snmp_event_url: " + properties.getProperty("snmp_event_url"));
			log.info("snmp_event_ip: " + properties.getProperty("snmp_event_ip"));
			log.info("snmp_event_port: " + properties.getProperty("snmp_event_port"));
			log.info("snmp_securityname: " + properties.getProperty("snmp_securityname"));
			log.debug("snmp_authpassphrase: " + properties.getProperty("snmp_authpassphrase"));
			log.debug("snmp_privpassphrase: " + properties.getProperty("snmp_privpassphrase"));
			log.info("snmp_engineid: " + properties.getProperty("snmp_engineid"));
			log.info("default_endpoint: " + properties.getProperty("default_endpoint"));
			log.info("snmp_privcypher: " + properties.getProperty("snmp_privcypher"));
			log.info("snmp_authcypher: " + properties.getProperty("snmp_authcypher"));
			log.info("snmp_oid: " + properties.getProperty("snmp_oid"));
			log.info("snmp_extra_oid: " + properties.getProperty("snmp_extra_oid"));
			log.info("reload_serviceapplicationmapping: "+ properties.getProperty("reload_serviceapplicationmapping"));			
			log.info("serviceaplications_url: "+ properties.getProperty("serviceaplications_url"));			
			log.debug("serviceapplication_token: "+ properties.getProperty("serviceapplication_token"));			

			
			
		} catch (IOException e) {
			log.error(e.getClass().toString(), e);
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (Exception e) {
					log.error(e);
				}
			}
		}

	}
	
	
	/**
	 * @return
	 */
	public Server newJettyServer() {

		Server server = new Server();
		
		HttpConfiguration httpConfiguration = new HttpConfiguration();
    
		ConnectionFactory connectionFactory = new HttpConnectionFactory(httpConfiguration);
		ServerConnector serverConnector = new ServerConnector(server, connectionFactory);
    	
		serverConnector.setPort(Integer.valueOf(properties.get("http_port").toString()));
		server.setConnectors(new Connector[] { serverConnector});
		
	
		log.info("Starting server at port: " + properties.get("http_port").toString() + " please wait ...");

		return server;

	}

}