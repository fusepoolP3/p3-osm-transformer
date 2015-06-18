package eu.fusepool.p3.osm;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

public interface XsltProcessor {

	public InputStream processXml(InputStream xsltUrl, InputStream xmlDataIn, String locationHeader) throws TransformerException, TransformerConfigurationException, 
	FileNotFoundException, IOException;
	
}
