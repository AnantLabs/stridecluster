package com.lin.stride;

import java.io.File;

import org.apache.lucene.index.CheckIndex;
import org.apache.lucene.index.CheckIndex.Status;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;



public class Maptest {

	
	public static void main(String[] args) throws Exception{
		/*		
		Map<String,String> map = System.getenv();
		for(Entry<String,String> entry : map.entrySet()){
			System.out.println(entry);
		}
		
		System.out.println("-----------------------------");
		*/
		/*Directory dir = FSDirectory.open(new File("D:/indexLucene"));
		IndexInput indexInput = dir.openInput("segments_1", IOContext.DEFAULT);
		
		System.out.println(indexInput.readInt());
		System.out.println(indexInput.readString());
		System.out.println(indexInput.readInt());
		System.out.println(indexInput.readLong());// version
		System.out.println(indexInput.readInt());// write counter
		int size = indexInput.readInt();
		System.out.println("-----------------------------");
		for(int i=0;i<size;i++){
			System.out.println(indexInput.readString());
			System.out.println(indexInput.readString());
			System.out.println(indexInput.readLong());// version
			System.out.println(indexInput.readInt());// write counter
			System.out.println("-----------------------------");
		}
		dir.close();*/
		
		
		
		
		
		
		
		/*InputStream in = ConfigReader.class.getClassLoader().getResourceAsStream("fields.xml");
		SAXReader saxReader = new SAXReader();
		Document doc = saxReader.read(in);
		List<Element> fields = doc.selectNodes("/fields/field");
		for(Element ele : fields){
			System.out.println(ele.attributeValue("name"));
			System.out.println(ele.attributeValue("type"));
			System.out.println(ele.elementText("class"));
			System.out.println(ele.elementText("store"));
			//fi.setIsStore(isStore);
		}*/
		
		
		
		
		
		Directory directory = FSDirectory.open(new File("d:/indexLucene3"));
		CheckIndex ci = new CheckIndex(directory);
		
		Status status = ci.checkIndex();
		
		System.out.println(status.cantOpenSegments);
		System.out.println(status.clean);
		System.out.println(status.missingSegments);
		System.out.println(status.missingSegmentVersion);
		System.out.println(status.partial);
		
		
		directory.close();
	}
}

 