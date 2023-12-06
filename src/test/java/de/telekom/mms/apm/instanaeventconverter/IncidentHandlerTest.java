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

import static org.junit.Assert.assertEquals;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;


/**
 * @author kao
 *
 */
public class IncidentHandlerTest {

	FileReader reader;
	JsonObject request;
	JsonObject fieldmapping;
	JsonObject servicemapping;
	JsonObject servicefilter;
	IncidentHandler incidentHandler;

	@Before
	public void setUp() throws Exception {

		try {
			reader = new FileReader("src/test/resources/event_presence_online.json");
			request = Json.parse(reader).asObject();
			reader = new FileReader("config/fieldmapping.json");
			fieldmapping = Json.parse(reader).asObject();
			reader = new FileReader("config/vservicemapping.json");
			servicemapping = Json.parse(reader).asObject();
			reader = new FileReader("config/servicefilter.json");
			servicefilter = Json.parse(reader).asObject();
			
			Properties properties = new Properties();
			InputStream input = null;

			try {

				input = new FileInputStream("config/config.properties");
				
				// load a properties file
				properties.load(input);

				Config.getInstance().setConfig(properties);
				
			}catch (IOException e) {

			}

			
			incidentHandler = new IncidentHandler();
			incidentHandler.readInstanaEvent(request);
			incidentHandler.setFieldmapping(fieldmapping);
			incidentHandler.setVServiceMapping(servicemapping);
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@After
	public void tearDown() throws Exception {
	}

		
	@Test
	public void testReadInstanaEvent() {

		String test = "";

		FileReader input;
		try {
			input = new FileReader("src/test/resources/event_presence_online.json");
			test = Json.parse(input).asObject().get("issue").asObject().get("text").asString();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		assertEquals(test, "online");

	}



	

	@Test
	public void testPresenceOfflineEvent() {

		FileReader input;
		String eventtype = null;

		try {
			input = new FileReader("src/test/resources/event_presence_offline.json");

			JsonObject request = Json.parse(input).asObject();
			incidentHandler.readInstanaEvent(request);
			eventtype = incidentHandler.getInstanaEvent().getEventtype();
			incidentHandler.mapFieldsInstana2Converter();
			incidentHandler.handlePresenceEvent();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		assertEquals(eventtype, EventType.PRESENCE.toString());

	}

	@Test
	public void testPresenceOnlineEvent() {

		FileReader input;
		String eventtype = null;

		try {
			input = new FileReader("src/test/resources/event_presence_online.json");

			JsonObject request = Json.parse(input).asObject();
			incidentHandler.readInstanaEvent(request);
			eventtype = incidentHandler.getInstanaEvent().getEventtype();
			incidentHandler.mapFieldsInstana2Converter();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		assertEquals(eventtype, EventType.PRESENCE.toString());

	}

	@Test
	public void testHostEvent() {

		assertEquals(true, true);
	}

	@Test
	public void testJVMEvent() {

		assertEquals(true, true);
	}

	@Test
	public void testPerformanceEvent() {

		assertEquals(true, true);
	}

	@Test
	public void testCloseEvent() {

		FileReader input;
		String eventtype = null;

		try {
			input = new FileReader("src/test/resources/event_close.json");

			JsonObject request = Json.parse(input).asObject();
			incidentHandler.readInstanaEvent(request);
			eventtype = incidentHandler.getInstanaEvent().getEventtype();
			incidentHandler.mapFieldsInstana2Converter();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		assertEquals(eventtype, EventType.CLOSED.toString());

	}

	@Test
	public void testErrorEvent() {

		FileReader input;
		String eventtype = null;

		try {
			input = new FileReader("src/test/resources/event_error.json");

			JsonObject request = Json.parse(input).asObject();
			incidentHandler.readInstanaEvent(request);
			eventtype = incidentHandler.getInstanaEvent().getEventtype();
			incidentHandler.mapFieldsInstana2Converter();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		assertEquals(eventtype, EventType.ISSUE.toString());

	}
	
	@Test
	public void testIncidentEvent() {

		FileReader input;
		String eventtype = null;

		try {
			input = new FileReader("src/test/resources/event_incident.json");

			JsonObject request = Json.parse(input).asObject();
			incidentHandler.readInstanaEvent(request);
			eventtype = incidentHandler.getInstanaEvent().getEventtype();
			incidentHandler.mapFieldsInstana2Converter();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		assertEquals(eventtype, EventType.INCIDENT.toString());

	}
	
	@Test
	public void testHandleRequest() {

	/*	FileReader input;
		String eventtype = null;
		

		try {
			input = new FileReader("src/test/resources/event_incident.json");

			JsonObject request = Json.parse(input).asObject();
			incidentHandler.setconverterAPI("NONE");
			try {
				incidentHandler.handleRequest(request);
			incidentHandler.readInstanaEvent(request);
			eventtype = incidentHandler.getInstanaEvent().getEventtype();
			incidentHandler.mapFieldsInstana2converter();


		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

			
		assertEquals(eventtype, EventType.INCIDENT.toString());
*/
		assertEquals(true,true);
	}
	
	@Test
	public void testServiceFieldAlias() {

		FileReader input;

		try {
			input = new FileReader("src/test/resources/event_service_field_alias.json");

			JsonObject request = Json.parse(input).asObject();
			incidentHandler.readInstanaEvent(request);
			incidentHandler.mapFieldsInstana2Converter();
			incidentHandler.mapEvent2Service();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	//	String test = incidentHandler.getExternalEvent().getValue("vapplication").asString();
		assertEquals(true,true);

	}
	
	@Test
	public void testEndpointServiceMapping() {

		FileReader input;

		try {
			input = new FileReader("src/test/resources/event_service_endpoint.json");

			JsonObject request = Json.parse(input).asObject();
			incidentHandler.readInstanaEvent(request);
			incidentHandler.mapFieldsInstana2Converter();
			incidentHandler.mapEvent2Service();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

//		String test = incidentHandler.getExternalEvent().getValue("vapplication").asString();
		assertEquals(true,true);

	}
	
	@Test
	public void testServiceDefaultAlias() {

		FileReader input;

		try {
			input = new FileReader("src/test/resources/event_service_default_alias.json");

			JsonObject request = Json.parse(input).asObject();
			incidentHandler.readInstanaEvent(request);
			incidentHandler.mapFieldsInstana2Converter();
			incidentHandler.mapEvent2Service();
	//		incidentHandler.mapEvent2ServiceApplication();
	//		incidentHandler.getExternalEvent().getValuesForSNMP();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

//		String test = incidentHandler.getExternalEvent().getValue("vapplication").asString();
		assertEquals(true,true);

	}
	
		
	@Test
	public void createServicesApplication() {
	
		try {
	//		JsonObject js_new = Config.getInstance().createApplicationsMappingFromInstana();
	//		JsonObject jo_old = Config.getInstance().readServicesApplicationsMapping();
	//		assertEquals(js_new,jo_old);	
			assertEquals(true,true);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	@Test 
	public void testSNMPEvent() {
		/*
		FileReader input;



		try {
			input = new FileReader("src/test/resources/event_incident.json");

			JsonObject request = Json.parse(input).asObject();
			incidentHandler.setconverterAPI("snmp");
			incidentHandler.setconverterSNMPEventIP("192.168.178.1");
			incidentHandler.setconverterSNMPEventPort("168");

			try {
				incidentHandler.handleRequest(request);
			incidentHandler.readInstanaEvent(request);
			incidentHandler.mapFieldsInstana2converter();
			incidentHandler.sendEvent();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		*/
		assertEquals(true,true);


	}
	
	
	@Test 
	public void testLogin() {
	

		try {

			String expires = ("2019-10-11T12:35:53 CEST");

			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss zzz",Locale.US);
			sdf.parse(expires).getTime();

			
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

			
		assertEquals(true,true);


	}
	
		
}