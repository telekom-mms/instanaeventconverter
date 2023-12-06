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
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

/** 
 * @author Kay Koedel
 *
 */
public class InstanaEventObject {

	private static final Logger log = LogManager.getLogger("file");

	private JsonObject issue;
	public EventType eventtype;
	private JsonObject event;

	HashMap<String, JsonValue> hashMap = new HashMap<String, JsonValue>();

	/**
	 * @param json the incomming event reads all Elements of the json Event cheks
	 *             for the Event Type
	 */
	public void readEvent(JsonObject json) {
		event = json;
		issue = json.get("issue").asObject();
		log.debug("JsonIssue: " + json.get("issue").toString());

		List<String> names = issue.names();
		Iterator<JsonObject.Member> iterator = issue.iterator();

		for (int i = 0; iterator.hasNext(); i++) {
			hashMap.put(names.get(i), iterator.next().getValue());
		}

		defineEventTyp();

	}

	public JsonValue getValue(String key) {
		if (hashMap.containsKey(key)) {
			JsonValue val = hashMap.get(key);
			return val;
		} else
			return null;
	}

	public void removeKey(String key) {
		if (hashMap.containsKey(key)) {
			hashMap.remove(key);	
		}
	}
	
	public boolean contains(String key) {
		boolean ret;
		if (hashMap.containsKey(key)) {
			ret = true;
		}
		else { 
			ret = false;
		}
		return ret;
	}
	
	public HashMap<String, JsonValue> getRemainingValues() {
		return hashMap;
	}


	public void defineEventTyp() {

		if (isTypePresence()) {
			eventtype = EventType.PRESENCE;
		}

		if (isTypeIssue()) {
			eventtype = EventType.ISSUE;
		}

		if (isTypeIncident()) {
			eventtype = EventType.INCIDENT;
		}

		if (isTypeChange()) {
			eventtype = EventType.CHANGE;
		}

		if (isStateClose()) {
			eventtype = EventType.CLOSED;
		}

	
		if (eventtype == null) {
			eventtype = EventType.NONE;
		}

		log.debug("Set Eventtype: " + eventtype.toString());

	}

	private boolean isTypeChange() {
		if (hashMap.containsKey("type")) {
			if (hashMap.get("type").asString().equals("change")) {
				return true;
			}
		}
		return false;
	}

	public boolean isStateOpen() {

		if (hashMap.containsKey("state")) {
			if (hashMap.get("state").asString().equals("OPEN")) {
				return true;
			}
		}
		return false;

	}

	private boolean isTypeIssue() {
		if (hashMap.containsKey("type")) {
			if (hashMap.get("type").asString().equals("issue")) {
				return true;
			}
		}
		return false;
	}

	private boolean isTypeIncident() {
		if (hashMap.containsKey("type")) {
			if (hashMap.get("type").asString().equals("incident")) {
				return true;
			}
		}
		return false;
	}




	public boolean isTypePresence() {

		if (hashMap.containsKey("type")) {
			if (hashMap.get("type").asString().equals("presence")) {
				return true;
			}
		}
		return false;

	}

	public boolean isStateClose() {

		if (hashMap.containsKey("state")) {
			if (hashMap.get("state").asString().equals("CLOSED")) {
				return true;
			}
		}
		return false;
	}


	public String getHostName() {
		if (hashMap.containsKey("fqdn")) {
			return hashMap.get("fqdn").asString();
		}

		else
			return null;
	}

	public long getStartTime() {
		if (hashMap.containsKey("start")) {
			return hashMap.get("start").asLong();
		}

		else
			return 0;
	}

	public long getEndTime() {
		if (hashMap.containsKey("end")) {
			return hashMap.get("end").asLong();
		}

		else
			return 0;
	}

	public String getEventtype() {

		return eventtype.toString();
	}

	public JsonObject getEvent() {
		return event;
	}

}
