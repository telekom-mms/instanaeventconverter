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

import java.io.Serializable;
import java.util.List;

public class ActiveEvent implements Serializable  {

    //default serialVersion id
    private static final long serialVersionUID = 1L;
    
	private String id;
	private String type;
	private List<String> applications;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}

	
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}

	
	public List<String> getApplications() {
		return applications;
	}
	public void setApplications(List<String> applications) {
		this.applications = applications;
	}
	

}
