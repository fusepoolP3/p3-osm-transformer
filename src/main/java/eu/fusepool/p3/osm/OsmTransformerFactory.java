/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.fusepool.p3.osm;

import eu.fusepool.p3.transformer.Transformer;
import eu.fusepool.p3.transformer.TransformerFactory;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author reto
 */
public class OsmTransformerFactory implements TransformerFactory {

    private final Map<String, Transformer> data2Transformer = 
            new HashMap<>();
    
    private final XsltProcessor processor;

    public OsmTransformerFactory() throws IOException {
        this.processor = new XsltProcessorImpl();
    }
    
    @Override
    public Transformer getTransformer(HttpServletRequest request) {
        final String xmlUri = request.getParameter("xml");
        return getTransfomerFor(xmlUri);
    }

    private synchronized Transformer getTransfomerFor(String xmlUri) {
        if (data2Transformer.containsKey(xmlUri)) {
            return data2Transformer.get(xmlUri);
        }
        final Transformer newTransformer = new OsmRdfTransformer(processor, xmlUri);
        data2Transformer.put(xmlUri, newTransformer);
        return newTransformer;
    }
    
}
