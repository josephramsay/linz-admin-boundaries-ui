package nz.govt.linz.AdminBoundaries;
/**
 * AdminBoundaries
 *
 * Copyright 2014 Crown copyright (c)
 * Land Information New Zealand and the New Zealand Government.
 * All rights reserved
 *
 * This program is released under the terms of the new BSD license. See the
 * LICENSE file for more information.
 */

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;


public class ResourceLoader {

	/**
	 * Making the default (no arg) constructor private
	 * ensures that this class cannnot be instantiated.
	 */
	private ResourceLoader() {}

	public static Properties getAsProperties(String fname) {
		Properties props = new Properties();
		URL url = ResourceLoader.getAsUrl(fname);
		if (url != null) {
			try {
				props.load(url.openStream());
			} 
			catch (FileNotFoundException fnfe){
				System.out.println("Can't find properties file "+fname);
				fnfe.printStackTrace();
			}
			catch (IOException ioe) {
				System.out.println("Error reading properties file "+fname);
				ioe.printStackTrace();
			}
		}
		return props;
	}

	public static URL getAsUrl(String name) {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		return classLoader.getResource(name);
	}
}