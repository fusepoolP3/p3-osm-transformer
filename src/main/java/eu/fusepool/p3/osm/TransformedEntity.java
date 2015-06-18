package eu.fusepool.p3.osm;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

import eu.fusepool.p3.transformer.commons.util.InputStreamEntity;

public class TransformedEntity extends InputStreamEntity {
	
	private InputStream dataIn = null;
	private String mediaType = ""; //MIME type

	public TransformedEntity(InputStream is, String mediaType){
		this.dataIn = is;
		if(mediaType != null && ! "".equals(mediaType))
		   this.mediaType = mediaType;
	}
	
	@Override
	public MimeType getType() {
		MimeType mimeType = null;
		try {
			mimeType = new MimeType(mediaType);
			
		} catch (MimeTypeParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return mimeType;
	}

	@Override
	public InputStream getData() throws IOException {
		// TODO Auto-generated method stub
		return dataIn;
	}
	
	

}
