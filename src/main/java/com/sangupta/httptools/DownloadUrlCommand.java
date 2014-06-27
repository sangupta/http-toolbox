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

import io.airlift.command.Command;
import io.airlift.command.Option;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sangupta.jerry.http.WebInvoker;
import com.sangupta.jerry.http.WebResponse;
import com.sangupta.jerry.util.AssertUtils;
import com.sangupta.jerry.util.GsonUtils;

/**
 * Download all URLs and persist them to disk.
 * 
 * @author sangupta
 *
 */
@Command(name = "download", description = "Download URLs to the disk")
public class DownloadUrlCommand extends HttpToolBoxCommand {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DownloadUrlCommand.class);
	
	@Option(name = { "-u", "--urlFile" }, description = "File containing one URL per line", required = true)
	public String urlFile;
	
	@Option(name = { "-p", "--prefix" }, description = "Prefix to be appended to each URL")
	public String prefix;
	
	@Option(name = { "-s", "--suffix" }, description = "Suffix to be appended to each URL")
	public String suffix;
	
	@Option(name = { "-n", "--numThreads" }, description = "Number of threads to spawn for crawling, default is 20")
	public int numThreads = 10;
	
	@Option(name = { "-o", "--output" }, description = "Output folder where individual files are written", required = true)
	public String outputFolder;
	
	/**
	 * The runnable tasks that we create before we fire threads for downloading
	 */
	private final List<Runnable> downloadTasks = new ArrayList<Runnable>();
	
	/**
	 * The suffix for each filename that we write to disk
	 */
	private final String storeSuffix = "." + UUID.randomUUID().toString() + ".response";
	
	/**
	 * Keeps track of current progress
	 */
	private AtomicInteger count = new AtomicInteger();
	
	/**
	 * The base directory where we need to write data
	 */
	private File outputDir;
	
	/**
	 * Indicates if we need to split folders as number of files per folder will be huge
	 */
	private boolean splitFolders;
	
	/**
	 * Total number of tasks that we have created
	 */
	private int numTasks;
	
	@Override
	public void run() {
		File file = new File(this.urlFile);
		if(file == null || !file.exists()) {
			System.out.println("URL file cannot be found.");
			return;
		}
		
		if(!file.isFile()) {
			System.out.println("URL file does not represent a valid file.");
			return;
		}
		
		if(this.numThreads <=0 || this.numThreads > 50) {
			System.out.println("Number of assigned threads should be between 1 and 50");
			return;
		}
		
		outputDir = new File(this.outputFolder);
		if(outputDir.exists() && !outputDir.isDirectory()) {
			System.out.println("Output folder does not represent a valid directory");
			return;
		}
		
		if(!outputDir.exists()) {
			outputDir.mkdirs();
		}

		// try and parse and read all URLs
		int line = 1;
		try {
			LineIterator iterator = FileUtils.lineIterator(file);
			while(iterator.hasNext()) {
				++line;
				String readURL = iterator.next();
				createURLTask(readURL);
			}
		} catch(IOException e) {
			System.out.println("Unable to read URLs from the file at line: " + line);
			return;
		}
		
		// all set - create number of threads
		// and start fetching
		ExecutorService service = Executors.newFixedThreadPool(this.numThreads);
		
		final long start = System.currentTimeMillis();
		for(Runnable runnable : this.downloadTasks) {
			service.submit(runnable);
		}
		
		// intialize some variables
		this.numTasks = this.downloadTasks.size();
		this.downloadTasks.clear();
		
		if(this.numTasks > 1000) {
			this.splitFolders = true;
		}
		
		// shutdown
		shutdownAndAwaitTermination(service);
		final long end = System.currentTimeMillis();
		
		// everything done
		System.out.println(this.downloadTasks.size() + " urls downloaded in " + (end - start) + " millis.");
	}

	/**
	 * Create a {@link Runnable} task for downloading and storage for the given
	 * URL.
	 * 
	 * @param url
	 *            the url to be downloaded
	 */
	private void createURLTask(String url) {
		if(AssertUtils.isEmpty(url)) {
			return;
		}
		
		if(AssertUtils.isNotEmpty(this.prefix)) {
			url = this.prefix + url;
		}
		
		if(AssertUtils.isNotEmpty(this.suffix)) {
			url = url + this.suffix;
		}
		
		final String downloadURL = url;
		this.downloadTasks.add(new Runnable() {

			@Override
			public void run() {
				downloadAndStoreURL(downloadURL);
			}
			
		});
	}

	/**
	 * Download the URL from web and then ask for storage.
	 * 
	 * @param url
	 *            the URL to be downloaded
	 */
	private void downloadAndStoreURL(String url) {
		int current = count.incrementAndGet();
		System.out.println("Download " + current + "/" + this.numTasks + " url: " + url + "...");
		WebResponse response = WebInvoker.getResponse(url);
		if(response == null) {
			LOGGER.debug("Unable to fetch response for URL from server: {}", url);
			return;
		}
		
		if(!response.isSuccess()) {
			LOGGER.debug("Non-success response for URL from server: {}", url);
			return;
		}
		
		store(current, url, response);
	}

	/**
	 * Store the downloaded web response to disk.
	 * 
	 * @param current
	 *            the current index count
	 * 
	 * @param url
	 *            the URL that was downloaded
	 * 
	 * @param response
	 *            the response from the server
	 */
	private void store(int current, String url, WebResponse response) {
		String json = GsonUtils.getGson().toJson(response);
		try {
			if(this.splitFolders) {
				int first = current % 16;
				int second = (current / 16) % 16;
				File folder = new File(this.outputDir.getAbsolutePath() + File.separator + first + File.separator + second);
				folder.mkdirs();
				FileUtils.write(new File(folder, "url-" + current + this.storeSuffix), json);
			} else {
				FileUtils.write(new File(this.outputDir, "url-" + current + this.storeSuffix), json);
			}
		} catch (IOException e) {
			LOGGER.error("Unable to write web response from URL {} to disk: {}", url, json);
		}
	}

	/**
	 * Terminate the thread pool
	 * 
	 * @param pool
	 *            the thread pool to terminate
	 */
	private void shutdownAndAwaitTermination(ExecutorService pool) {
		pool.shutdown(); // Disable new tasks from being submitted
		try {
			// Wait a while for existing tasks to terminate
			if (!pool.awaitTermination(1, TimeUnit.DAYS)) {
				pool.shutdownNow(); // Cancel currently executing tasks
				
				// Wait a while for tasks to respond to being cancelled
				if (!pool.awaitTermination(60, TimeUnit.SECONDS))
					System.err.println("Pool did not terminate");
			}
		} catch (InterruptedException ie) {
			// (Re-)Cancel if current thread also interrupted
			pool.shutdownNow();
			// Preserve interrupt status
			Thread.currentThread().interrupt();
		}
	}
	
}
