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

import io.airlift.command.Cli;
import io.airlift.command.Cli.CliBuilder;
import io.airlift.command.Help;
import io.airlift.command.ParseOptionMissingException;

/**
 * Command line tool to download all URLs present in a given
 * file adding a prefix or a suffix to the URL as needed.
 * 
 * @author sangupta
 *
 */
public class HttpToolBoxMain {

	public static void main(String[] args) {
		
		@SuppressWarnings("unchecked")
		CliBuilder<Runnable> builder = new CliBuilder<Runnable>("htb")
											.withDescription("HTTP toolbox")
											.withDefaultCommand(Help.class)
											.withCommands(Help.class, DownloadUrlCommand.class);
		
//		builder.withGroup("download")
//				.withDescription("Download URLs from net")
//				.wi
		
		Cli<Runnable> htbParser = builder.build();
		try {
			htbParser.parse(args).run();
		} catch(ParseOptionMissingException e) {
			System.out.println("HTTP Toolbox: " + e.getMessage());
			System.out.println("Use -h for usage instructions.");
//			Help.help(command);
		}
	}
}
