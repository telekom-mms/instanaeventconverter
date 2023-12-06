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

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.ParseException;
import com.instana.sdk.annotation.Span;
import com.instana.sdk.annotation.Span.Type;
import com.instana.sdk.support.SpanSupport;

/**
 * This class handles incomming request and is the Basclass for doing the stuff
 * 
 * * handle incomming requests * read the json from the request * creates
 * instanaEvent object from the json * creates externalEvent object from the
 * instanaEvent according to the eventtype specific mapping * to some stuff to
 * 
 * @author Kay Koedel
 *
 */
@SuppressWarnings("deprecation")
public class IncidentHandler extends Handler.Abstract.NonBlocking {
	private static final Logger log = LogManager.getLogger("standard");
	private static final Logger logevent_instana = LogManager.getLogger("instanaevent");
	private static final Logger logevent_external = LogManager.getLogger("externalevent");

	private EventsMap eventsMap = new EventsMap();
	private JsonObject fieldMapping = new JsonObject();
	private JsonObject vserviceMapping = new JsonObject();
	private JsonObject serviceApplicationMapping = new JsonObject();

	private String defaultEndpoint;

	private InstanaEventObject instanaEventObject;
	private ExternalEventObject externalEventObject;

	private String responseString;
	private boolean eventActive;
	private boolean addNotMappedKeys;

	private List<String> applicationList = null;

	@Span(type = Type.ENTRY, value = "InstanaEventConverter")
	@Override
	public boolean handle(Request request, Response response, Callback callback) throws Exception {
		String target = request.getHttpURI().getPath();
		if (target == null) {
			target = defaultEndpoint;
		}
		
		response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		responseString = "valid endpoints are / /instana /direct or /eif";

		// direct snmp
		if (target.contains("direct")) {
			log.debug("sending request direct as snmptrap");
			String requestAsString = Content.Source.asString(request, Charset.defaultCharset());
			JsonValue event;
			try {
				event = Json.parse(requestAsString);
				externalEventObject = new ExternalEventObject();
				externalEventObject.setJsonDirect(event.asObject().toString());
				response.setStatus(HttpServletResponse.SC_OK);
				responseString = "JsonEvent erhalten and sending direct";
			} catch (ParseException pe) {
				log.error("No valid json received: " + requestAsString);

				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				responseString = "No valid JsonEvent erhalten";
			}

			sendEvent(externalEventObject);

		}

		if (target.contains("instana")) {

			SpanSupport.annotate(Type.ENTRY, "instanaEventconverter", "tags.http.url", "/instanaEventconverter/");
			String requestAsString = Content.Source.asString(request, Charset.defaultCharset());
			JsonValue event;
			try {
				event = Json.parse(requestAsString);
				handleRequest(event.asObject());
				JsonValue state = externalEventObject.getValue("state");
				if (state != null) {
					if (!state.asString().equals("CLOSED")) {
						eventsMap.saveActiveEvent();
						responseString = "Instana Event erhalten und verarbeitet";
						response.setStatus(HttpServletResponse.SC_OK);
					}
					else {
						responseString = "Instana Event erhalten und verarbeitet";
						response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
					}
						
				}
				else {
					log.error("state must not be null");
					responseString = "state must not be null";
					response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
				}

			} catch (ParseException pe) {
				log.error("no valid json received: " + requestAsString);
				log.error(pe.toString());
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				responseString = "Kein JsonEvent oder InstanaEvent erhalten";
			} catch (Exception ex) {
				log.error("no valid json received: " + requestAsString);
				log.error(ex.getMessage(), ex);
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				responseString = "Kein JsonEvent oder InstanaEvent erhalten";
			}
		}
		response.write(true, ByteBuffer.wrap(responseString.getBytes()), callback);
		// baseRequest.setHandled(true);
		log.debug("Response: " + responseString);
	
		return true;
	}



	@Span(value = "instanaEventconverter", captureArguments = true, captureReturn = false)
	public void handleRequest(JsonObject json) throws Exception {
		log.debug("json : " + Json.parse(json.toString()));

		applicationList = null;

		readInstanaEvent(json);
		if (fieldMapping != null)
			mapFieldsInstana2Converter();
		if (vserviceMapping != null)
			mapEvent2Service();
		if (serviceApplicationMapping != null)
			mapEvent2ServiceApplication();

		// log the received instana event
		log.debug(Json.parse(instanaEventObject.getEvent().toString()));
		logevent_instana.info(Json.parse(instanaEventObject.getEvent().toString()));

		String id = externalEventObject.getValue("id").asString();
		String eventType = null;
		boolean eventExist = true;

		// set type if it is not a closed event
		JsonValue state = externalEventObject.getValue("state");
		if (state != null) { 
			if (state.asString().equals("CLOSED")) {
	
				if (eventsMap.eventExists(id)) {
					eventsMap.loadActiveEventString(id);
					applicationList = eventsMap.getApplicationList();
					eventType = eventsMap.getEventType();
				} else {
					eventExist = false;
					log.warn("Event does not exist");
				}
	
			} else {
				eventType = externalEventObject.getValue("type").asString();
				eventsMap = new EventsMap();
			}
		}
		else {
			log.error("state must not be null");
			responseString = "state must not be null";
		}

		// run if config is active for eventtype and eventtyp
		if (eventActive && eventExist) {

			// send multiple events if multiple applications are involved
			String vapplication;
			if (applicationList == null) {
				applicationList = new ArrayList<String>();
			}
			Iterator<String> iterator = applicationList.iterator();
			if (!applicationList.isEmpty())
				while (iterator.hasNext()) {
					vapplication = (String) iterator.next();
					externalEventObject.setValue("vapplication",
							(new JsonObject().set("key", vapplication)).get("key"));
					externalEventObject.setValue("type", (new JsonObject().set("key", eventType)).get("key"));
					externalEventObject.createSNMPEvent();

					handleEventtype();

					log.debug(externalEventObject.getValuesForSNMP());
					sendEvent(externalEventObject);

				}
			else {

				vapplication = externalEventObject.getValue("vservice").asString();
				externalEventObject.setValue("vapplication", (new JsonObject().set("key", vapplication)).get("key"));
				handleEventtype();
				sendEvent(externalEventObject);
			}
		}
	}

	private void handleEventtype() {

		switch (instanaEventObject.eventtype) {
		case CLOSED:
			handleCloseEvent();
			break;
		case PRESENCE:
			handlePresenceEvent();
			break;
		case INCIDENT:
			handleIncidentEvent();
			break;
		case ISSUE:
			handleIssueEvent();
			break;
		case CHANGE:
			handleChangeEvent();
			break;
		default:
			log.error("Somthing strange has happend while switching eventtype");
			break;

		}
	}

	private void handleChangeEvent() {
		eventsMap.put(externalEventObject.getValue("id").asString(),
				externalEventObject.getValue("vapplication").asString(),
				externalEventObject.getValue("type").asString());
	}

	private void handleIssueEvent() {
		eventsMap.put(externalEventObject.getValue("id").asString(),
				externalEventObject.getValue("vapplication").asString(),
				externalEventObject.getValue("type").asString());
	}

	private void handleIncidentEvent() {
		eventsMap.put(externalEventObject.getValue("id").asString(),
				externalEventObject.getValue("vapplication").asString(),
				externalEventObject.getValue("type").asString());
	}

	private void handleCloseEvent() {
		JsonValue id = externalEventObject.getValue("id");
		eventsMap.remove(id.asString());
	}

	public void handlePresenceEvent() {

		if (instanaEventObject.getValue("text").asString().equals("online"))
			externalEventObject.setValue("status", (new JsonObject().set("key", "CLOSED")).get("key"));
		else
			externalEventObject.setValue("status", (new JsonObject().set("key", "OPEN")).get("key"));

		JsonValue entity = instanaEventObject.getValue("entity");
		JsonValue entityLabel = instanaEventObject.getValue("entityLabel");
		JsonValue fqdn = instanaEventObject.getValue("fqdn");

		String identifier = "";
		if (entity != null)
			identifier = identifier.concat(entity.asString().trim()) + "-";
		if (entityLabel != null)
			identifier = identifier.concat(entityLabel.asString().trim()) + "-";
		if (fqdn != null)
			identifier = identifier.concat(fqdn.asString().trim());

		// Node fqdn, cluser entiy_label
		if (fqdn.asString().equals("not available")) {
			externalEventObject.setValue("vservice", entityLabel);
		} else {
			externalEventObject.setValue("vservice", fqdn);
		}
		eventsMap.put(externalEventObject.getValue("id").asString(),
				externalEventObject.getValue("vservice").asString(), 
				externalEventObject.getValue("type").asString());

	}

	public void setFieldmapping(JsonObject json) {
		fieldMapping = json;
	}

	public void setVServiceMapping(JsonObject json) {
		vserviceMapping = json;
	}
	
	public void setServicesApplicationMapping(JsonObject json) {
		serviceApplicationMapping = json;

	}

	public void readInstanaEvent(JsonObject json) {
		externalEventObject = new ExternalEventObject();
		instanaEventObject = new InstanaEventObject();
		instanaEventObject.readEvent(json);

	}

	public void sendEvent(ExternalEventObject eventObject) {

		if (Config.getInstance().getValue("api").equals("snmp")) {
			try {
				TimeUnit.MILLISECONDS.sleep(Long.valueOf(Config.getInstance().getValue("send_delay")));
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				log.error("Error during SendDelay");
				log.error(e.getMessage());
				log.error(e.getStackTrace());
			}
			log.debug("now sending with snmp implemented");
			ConverterSNMP converterSNMP = new ConverterSNMP();
			converterSNMP.setAdatperEventObject(eventObject);
			converterSNMP.postEvent2Converter();
			log.debug(externalEventObject.getValuesForSNMP());
			logevent_external.info(externalEventObject.getValuesForSNMP());
			log.debug("sending with snmp done");
		}

	}

	// every service is mapped to one or more application
	// applicationList contains all application for vservice in externalApplication
	public void mapEvent2ServiceApplication() {
		JsonArray serviceApplicationMappingConfig;
		serviceApplicationMappingConfig = serviceApplicationMapping.get("serviceapplicationmapping").asArray();
		JsonValue mappingElement;
		applicationList = new ArrayList<String>();
		Iterator<JsonValue> serviceApplicationMappingConfigIterator = serviceApplicationMappingConfig.iterator();

		while (serviceApplicationMappingConfigIterator.hasNext()) {

			mappingElement = serviceApplicationMappingConfigIterator.next();
			if (mappingElement.asObject().get("service") == null)
				log.debug("test");
			if (mappingElement.asObject().get("service").asString()
					.equals(externalEventObject.getValue("vservice").asString())) {
				applicationList.add(mappingElement.asObject().get("application").asString());

			}
		}
	}

	/*
	 * This is specially to map an Event or something to a service
	 */
	public void mapEvent2Service() {
		JsonArray vServiceMappingArray;

		vServiceMappingArray = vserviceMapping.get("vservicemapping").asArray();

		log.debug("VServiceMappingArray: " + vServiceMappingArray.toString());

		Iterator<JsonValue> vserviceMappingIterator = vServiceMappingArray.iterator();

		JsonValue vserviceJson;
		JsonValue mappingFildsJson;
		JsonArray mappingFields;

		String instanaKey;
		String instanaMappingField;
		JsonValue instanaEventValue;
		boolean mapservice;
		String vservicename;
		boolean foundServiceName = false;

		while (vserviceMappingIterator.hasNext() && !foundServiceName) {

			vserviceJson = vserviceMappingIterator.next();
			mappingFields = vserviceJson.asObject().get("mapping_fields").asArray();

			Iterator<JsonValue> mappingFildsIterator = mappingFields.iterator();
			mapservice = true;
			vservicename = null;

			while (mappingFildsIterator.hasNext() && mapservice) {

				mappingFildsJson = mappingFildsIterator.next();
				instanaKey = mappingFildsJson.asObject().get("instana_field_key").asString();
				instanaMappingField = mappingFildsJson.asObject().get("instana_field_value").asString();
				instanaEventValue = instanaEventObject.getValue(instanaKey);

				if (instanaEventValue != null) {
					if (instanaMappingField.equals(instanaEventValue.asString())
							|| (instanaMappingField.equals("notnull"))) {
						mapservice = true;

						if (mappingFildsJson.asObject().get("value_is_vservice_name").asBoolean())
							vservicename = instanaEventValue.asString();

					} else
						mapservice = false;
				} else
					mapservice = false;

			}

			if (mapservice && (vservicename == null)) {
				externalEventObject.setValue("vservice", vserviceJson.asObject().get("converter_alias"));
				foundServiceName = true;
			}
			if (mapservice && (vservicename != null)) {
				externalEventObject.setValue("vservice", JsonObject.valueOf(vservicename));
				foundServiceName = true;
			}
			if (!mapservice)
				externalEventObject.setValue("vservice", JsonObject.valueOf("none"));

		}

	}

	/**
	 * sets all the fields for mapping from the configfile for the eventtype
	 */
	public void mapFieldsInstana2Converter() {

		String eventtype = instanaEventObject.getEventtype().toLowerCase();

		// take the relevant mapping from file (maybe this can later be in the init for
		// the types

		JsonArray fieldMappingArray;
		JsonObject fieldMappingObject;

		try {
			fieldMappingObject = fieldMapping.get(eventtype).asObject();
		} catch (Exception e) {
			log.info("Eventtype not defined using default!");
			fieldMappingObject = fieldMapping.get("default").asObject();
		}

		eventActive = fieldMappingObject.get("active").asBoolean();
		addNotMappedKeys = fieldMappingObject.get("addNotMappedKeys").asBoolean();
		fieldMappingArray = fieldMappingObject.get("keys").asArray();

		log.debug("MappingArray: " + fieldMappingArray.toString());
		log.debug("Eventtype is active: " + eventActive);

		Iterator<JsonValue> fieldMappingIterator = fieldMappingArray.iterator();

		JsonValue fieldMapValue;
		String instanaKey;
		JsonValue instanaValue;
		String converterKey;
		boolean concat = false;

		while (fieldMappingIterator.hasNext()) {

			fieldMapValue = fieldMappingIterator.next();
			instanaKey = fieldMapValue.asObject().get("instana").asString();
			if (fieldMapValue.asObject().get("concate") != null)
				concat = fieldMapValue.asObject().get("concate").asBoolean();

			if (instanaEventObject.contains(instanaKey)) {

				instanaValue = instanaEventObject.getValue(instanaKey);
				if (instanaValue == null) {
					instanaValue = fieldMapValue.asObject().get("default");
				}
				converterKey = fieldMapValue.asObject().get("converter").asString();
				externalEventObject.setValue(converterKey, instanaValue, concat);

			}

		}

		if (addNotMappedKeys) {
			HashMap<String, JsonValue> remainingValues = new HashMap<String, JsonValue>();

			for (Map.Entry<String, JsonValue> entry : remainingValues.entrySet()) {
				externalEventObject.setValue(entry.getKey(), entry.getValue());
			}
		}

	}

	public ExternalEventObject getExternalEvent() {
		return externalEventObject;
	}

	public InstanaEventObject getInstanaEvent() {
		return instanaEventObject;
	}

	/*
	 * we need to set open or close event depending on online or offline we need to
	 * set the status correct (status)
	 */

	/*
	 * just for debugging
	 */
	private String httpServletRequestToString(HttpServletRequest request) {
		StringBuilder sb = new StringBuilder();

		sb.append("Request Method = [" + request.getMethod() + "], ");
		sb.append("Request URL Path = [" + request.getRequestURL() + "], ");

		String headers = Collections.list(request.getHeaderNames()).stream()
				.map(headerName -> headerName + " : " + Collections.list(request.getHeaders(headerName)))
				.collect(Collectors.joining(", "));

		if (headers.isEmpty()) {
			sb.append("Request headers: NONE,");
		} else {
			sb.append("Request headers: [" + headers + "],");
		}

		String parameters = Collections.list(request.getParameterNames()).stream()
				.map(p -> p + " : " + Arrays.asList(request.getParameterValues(p))).collect(Collectors.joining(", "));

		if (parameters.isEmpty()) {
			sb.append("Request parameters: NONE.");
		} else {
			sb.append("Request parameters: [" + parameters + "].");
		}

		return sb.toString();
	}

}
