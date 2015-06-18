package eu.fusepool.p3.osm;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.TransformerConfigurationException;

import org.xml.sax.InputSource;

/**
 * Implements a transformation from XML to RDF 
 * @author Luigi Selmi
 *
 */

public class XsltProcessorImpl implements XsltProcessor {
	@Override
	public InputStream processXml(InputStream xslt, InputStream xmlDataIn, String locationHeader) throws TransformerException, TransformerConfigurationException, 
	FileNotFoundException, IOException {
		
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		
		TransformerFactory tFactory = TransformerFactory.newInstance();
		
		Transformer transformer = tFactory.newTransformer(new StreamSource( xslt ));
		
		if (locationHeader != null && ! locationHeader.equals("")) {		
		    transformer.setParameter("locationHeader", locationHeader);
		}
		
		transformer.transform(new StreamSource( xmlDataIn ), new StreamResult( outputStream ));
		
		return new ByteArrayInputStream(outputStream.toByteArray());
	}

}
