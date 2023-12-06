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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EventsMap {

	private static final Logger log = LogManager.getLogger("file");
	private ActiveEvent activeEvent;

	public void saveActiveEvent() {

		try {

			FileOutputStream fileOut = new FileOutputStream("openEvents/" + activeEvent.getId());
			ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);
			objectOut.writeObject(activeEvent);
			objectOut.close();

		} catch (Exception ex) {
			log.error(ex.getMessage());
			log.error(ex.getMessage(),ex);
		}

	}

	public void addActiveEvent(String id, List<String> applications, String type) {
		activeEvent = new ActiveEvent();
		activeEvent.setId(id);
		activeEvent.setApplications(applications);
		activeEvent.setType(type);
	}

	public void put(String id, String new_vapplication, String type) {

		ArrayList<String> vapplications = null;

		if (activeEvent == null) {
			activeEvent = new ActiveEvent();
			vapplications = new ArrayList<String>();
			activeEvent.setApplications(vapplications);
		} else {
			vapplications = (ArrayList<String>) activeEvent.getApplications();
		}
		
		if (!vapplications.contains(new_vapplication)) {
			vapplications.add(new_vapplication.trim());
		}
		activeEvent.setApplications(vapplications);
		activeEvent.setType(type);
		activeEvent.setId(id);
		
	}

	public boolean eventExists(String id) {
		File file = new File("openEvents/" + id);

		if (file.exists()) {
			return true;
		}
		return false;

	}

	public void loadActiveEventString(String id) {

		try {
			FileInputStream fileIn = new FileInputStream("openEvents/" + id);
			ObjectInputStream objectIn = new ObjectInputStream(fileIn);
			activeEvent = (ActiveEvent) objectIn.readObject();
			objectIn.close();
		} catch (Exception ex) {
			log.error(ex.getMessage(),ex);

		}

	}

	public String getEventType() {

		return activeEvent.getType();

	}

	public List<String> getApplicationList() {
	
		return activeEvent.getApplications();

	}

	public void remove(String id) {
		File file = new File("openEvents/" + id);
		file.delete();

	}
}
