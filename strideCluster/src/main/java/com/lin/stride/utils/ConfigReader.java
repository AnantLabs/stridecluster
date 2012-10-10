package com.lin.stride.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

public class ConfigReader {
	
	private static Logger LOG = Logger.getLogger(ConfigReader.class);
	private static Properties prop = new Properties();
	private static List<FieldInfo> list = new ArrayList<FieldInfo>();
	
	static{
		InputStream in = ConfigReader.class.getClassLoader().getResourceAsStream("stride.properties");
		SAXReader saxReader = new SAXReader();
		try {
			prop.load(in);
		} catch (IOException e) {
			e.printStackTrace();
		} 
		in = ConfigReader.class.getClassLoader().getResourceAsStream("fields.xml");
		try {
			Document doc = saxReader.read(in);
			List<Element> fields = doc.selectNodes("/fields/field");
			/*try {
				for(Element ele : fields){
					FieldInfo fi = new FieldInfo();
					fi.setIndexName(ele.attributeValue("name"));
					fi.setType(ele.attributeValue("type"));
					fi.setIndexClass(Class.forName(ele.elementText("class")));
					fi.setStore(new Boolean(ele.elementText("store")));
					list.add(fi);
				}
			} catch (ClassNotFoundException e) {
				LOG.error(e.getMessage(),e);
			}*/
		} catch (DocumentException e) {
			LOG.error(e.getMessage(),e);
		}
		try {
			in.close();
		} catch (IOException e) {
			LOG.error(e.getMessage(),e);
		}
	}
	
	public static String getEntry(String key){
		return prop.getProperty(key);
	}
	
	public static String getEntry(String key, String defaultValue){
		return prop.getProperty(key, defaultValue);
	}

	public static List<FieldInfo> getFields(){
		return list;
	}
	
}
