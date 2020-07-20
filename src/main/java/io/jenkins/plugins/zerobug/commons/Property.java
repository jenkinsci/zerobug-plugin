package io.jenkins.plugins.zerobug.commons;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Property {
	public static String getByKey(final String key) throws IOException {
		ClassLoader classLoader = Property.class.getClassLoader();
		InputStream input = classLoader.getResourceAsStream("configuration.properties");
		Properties prop = new Properties();
		prop.load(input);
		return prop.getProperty(key);
	}
}
