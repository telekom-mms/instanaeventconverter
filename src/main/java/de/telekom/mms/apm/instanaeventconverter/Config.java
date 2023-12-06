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

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

public class Config {
	
 	 private static final Logger log = LogManager.getLogger("standard");
	 private static final Config config = new Config(); 
	 private Properties properties;
	 
	  private Config() {

      }
          
      public static Config getInstance() {
        return config;
      } 
      
      public String getValue(String key) {
    	  return properties.getProperty(key);
      }

	public void setConfig(Properties properties) {
		this.properties = properties; 

	}
	@Deprecated 
	public JsonObject readServicesApplicationsMapping() {
		
		// read the servicesApplicationsMapping
		JsonObject servicesApplicationsMapping;
		FileReader reader;
		try {
			reader = new FileReader("config/servicesapplications.json");
			servicesApplicationsMapping = Json.parse(reader).asObject();
			return servicesApplicationsMapping;

		} catch (FileNotFoundException e) {

			log.error(e);
		} catch (IOException e) {

			log.error(e);
		}

		return null;
	}
	
	private String readRequestFromURL(String url) {
	    try {
			URLConnection conn = new URL(url).openConnection();
			String token = properties.getProperty("serviceapplication_token");
			conn.setRequestProperty("Authorization", "apitoken "+ token);
	        conn.setReadTimeout(60 * 1000);
	        conn.setConnectTimeout(60 * 1000);
	        InputStream is = conn.getInputStream();
	        
	        ByteArrayOutputStream result = new ByteArrayOutputStream();
	        byte[] buffer = new byte[1024];
	  
	        for (int length; (length = is.read(buffer)) != -1; ) {
				result.write(buffer, 0, length);
			}
	        return result.toString();
	    } catch (Exception e) {
			// TODO Auto-generated catch block
			log.error(e.getMessage());
			log.error(e.getStackTrace());
		}
	    return null;
        
	}

	private JsonObject getServicesApplicationsMappingFromInstana() {
		JsonObject json;
		String urlString = properties.getProperty("serviceaplications_url");
		String responseString = readRequestFromURL(urlString);
        json = Json.parse(responseString.toString()).asObject();
	    return json;
	}

	public JsonObject createApplicationsMappingFromInstana() {
		
		JsonObject servicefilter = Config.getInstance().readServiceFilter();
		JsonObject sourceJson = getServicesApplicationsMappingFromInstana();
		JsonObject applicationJson = null;
		JsonArray  servicesApplicationsList = new JsonArray();
		Iterator<JsonValue> applicationsIterator = sourceJson.get("items").asArray().iterator();

		
		while (applicationsIterator.hasNext()) {

			applicationJson = applicationsIterator.next().asObject();
			String serviceUrl = applicationJson.get("_links").asObject().get("services").asString();
			String applicationName = applicationJson.get("label").asString();
			if (serviceUrl != null) {
				String responseString = readRequestFromURL(serviceUrl);
				JsonObject servicesJson = Json.parse(responseString.toString()).asObject();
				if (!servicesJson.isEmpty()) {
					Iterator<JsonValue> servicesIterator = servicesJson.get("items").asArray().iterator();
	
					while (servicesIterator.hasNext()) {
						String serviceName = servicesIterator.next().asObject().get("label").asString();
						JsonObject json = new JsonObject();
						Iterator<JsonValue> filterIterator = servicefilter.get("servicefilter").asArray().iterator();
						boolean filtered = false;
						    while (filterIterator.hasNext()) {
								JsonObject elem = filterIterator.next().asObject();
								String filterServiceName = elem.get("service").asString();
							    String filterApplicationName = elem.get("application").asString();
							    if 	((filterServiceName.equals(serviceName) && filterApplicationName.equals(applicationName))) {
									filtered = true;
								}
							}
							if (!filtered)  {
 						    	json.add("service", serviceName);
								json.add("application", applicationName);							
								servicesApplicationsList.add(json);
							}
					}
				}
			}

		}
		
		JsonValue value = Json.parse(servicesApplicationsList.toString());
        return new JsonObject().add("serviceapplicationmapping", value);
	}
	
	public JsonObject readFieldMapping() {
		// read the Fieldmapping
		JsonObject fieldmapping;
		FileReader reader;
		try {
			reader = new FileReader("config/fieldmapping.json");
			fieldmapping = Json.parse(reader).asObject();
			return fieldmapping;

		} catch (FileNotFoundException e) {

			log.error(e);
		} catch (IOException e) {

			log.error(e);
		}

		return null;

	}
	
	public JsonObject readVServiceMapping() {
		// read the vservicemapping
		JsonObject vservicemapping;
		FileReader reader;
		try {
			reader = new FileReader("config/vservicemapping.json");
			vservicemapping = Json.parse(reader).asObject();
			return vservicemapping;

		} catch (FileNotFoundException e) {

			log.error(e);
		} catch (IOException e) {

			log.error(e);
		}

		return null;

	}
	
	public JsonObject readServiceFilter() {
		JsonObject servicefilter;
		FileReader reader;
		try {
			reader = new FileReader("config/servicefilter.json");
			servicefilter = Json.parse(reader).asObject();
			return servicefilter;

		} catch (FileNotFoundException e) {

			log.error(e);
		} catch (IOException e) {

			log.error(e);
		}

		return null;

	}
	
}
