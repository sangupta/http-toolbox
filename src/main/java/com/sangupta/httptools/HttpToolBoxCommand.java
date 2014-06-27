/**
 *
 * http-toolbox: Command line HTTP tools
 * Copyright (c) 2014, Sandeep Gupta
 * 
 * http://sangupta.com/projects/http-toolbox
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * 		http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package com.sangupta.httptools;

import io.airlift.command.Option;
import io.airlift.command.OptionType;

/**
 * 
 * @author sangupta
 *
 */
public class HttpToolBoxCommand implements Runnable {
	
	@Option(type = OptionType.GLOBAL, name = "-v", description = "Verbose mode")
    public boolean verbose;

	@Override
	public void run() {
		System.out.println("htb");
	}

}
