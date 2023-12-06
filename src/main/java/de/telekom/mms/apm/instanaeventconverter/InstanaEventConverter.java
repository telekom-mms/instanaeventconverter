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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;


/**
 * This is just the main class to start the converter
 * 
 * @author Kay Koedel
 */
public class InstanaEventConverter {
	private static final Logger log = LogManager.getLogger("standard");
	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) {
		
		ConverterServer converter = new ConverterServer();
		
		LoggerContext context = (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
		File file = new File("config/log4j2.config");
		context.setConfigLocation(file.toURI());
		
		log.info("*************************************************************************");
		log.info("Starting InstanaEventConverter ... ");
		log.info("*************************************************************************");
		converter.start();

	}

}
