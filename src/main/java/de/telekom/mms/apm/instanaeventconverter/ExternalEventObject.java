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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.snmp4j.smi.OctetString;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

/**
 * 
 * @author Kay Koedel
 *
 */
public class ExternalEventObject {

	private static final Logger log = LogManager.getLogger("standard");
	HashMap<String, JsonValue> hashMap = new HashMap<String, JsonValue>();
	private String jsonForSNMP;
	private String jsonEvent;

	public void setJsonDirect(String json) {
		jsonEvent = json.replace(",", ", ");
	}


	public void setValue(String key, JsonValue value, boolean concate) {
		// what is with duplicated Key?
		if (concate && hashMap.containsKey(key))
			value = Json.value(hashMap.get(key).asString() + " - " + value.asString());
		hashMap.put(key, value);

	}

	public void setValue(String key, JsonValue value) {
		hashMap.put(key, value);
	}

	public JsonValue getValue(String key) {
		if (hashMap.containsKey(key))
			return hashMap.get(key);
		else
			return null;
	}

	public OctetString getValuesForSNMP() {
	
		if (jsonEvent == null)
			createSNMPEvent();
	
		return new OctetString(jsonEvent);
	}

	public void createSNMPEvent() {
		correctconverterSeverity();
		correctEndpoint();
		JsonArray array = new JsonArray();
		JsonObject json = new JsonObject();
		JsonObject attributes = new JsonObject();

		Entry<String, JsonValue> entry;
		Iterator<Entry<String, JsonValue>> iterator = hashMap.entrySet().iterator();
		if (hashMap.containsKey("vapplication")) {
			attributes.add("vapplication", hashMap.get("vapplication"));
		}
		if (hashMap.containsKey("type")) {
			attributes.add("type", hashMap.get("type"));
		}

		while (iterator.hasNext()) {
			entry = iterator.next();
			if (!entry.getKey().equals("vapplication") && !entry.getKey().equals("type"))
				attributes.add(entry.getKey().toString(), entry.getValue());

		}

		json.add("attributes", attributes);
		array.add(json);
		jsonForSNMP = array.toString();

		jsonForSNMP = jsonForSNMP.replace("ä", "ae");
		jsonForSNMP = jsonForSNMP.replace("ü", "ue");
		jsonForSNMP = jsonForSNMP.replace("ö", "oe");
		jsonForSNMP = jsonForSNMP.replace("ß", "ss");
		jsonForSNMP = jsonForSNMP.replace("Ä", "Ae");
		jsonForSNMP = jsonForSNMP.replace("Ü", "Ue");
		jsonForSNMP = jsonForSNMP.replace("Ö", "Oe");
		jsonForSNMP = jsonForSNMP.replace(",", ", ");
		
		this.jsonEvent = jsonForSNMP;

	}


	private void correctEndpoint() {
		if (hashMap.containsKey("vservice")) {
			if (hashMap.get("vservice").asString().contains("T /")) {
				String endpoint = hashMap.get("vservice").toString();
				String fixedEndpoint = endpoint.substring(endpoint.indexOf("/") + 1, endpoint.length() - 1);
				hashMap.put("vservice", Json.value(fixedEndpoint));

			}

		}
	}

	// the Severity can be Critical, Major, Minor, Warning, Information, OK
	private void correctconverterSeverity() {

		JsonObject json = new JsonObject();
		int value = -1;
		if (hashMap.containsKey("severity")) {
			if (hashMap.get("severity").isNumber()) {
				value = Integer.valueOf(hashMap.get("severity").asInt()).intValue();


			switch (value) {
			case -1:
				json.add("severity", "INFORMATION");
				break;
			case 5:
				json.add("severity", "WARNING");
				break;
			case 10:
				json.add("severity", "CRITICAL");
				break;
			default:
				json.add("severity", "INFORMATION");
				break;
			}
			hashMap.put("severity", json.get("severity"));
			}
		}
	}


}
