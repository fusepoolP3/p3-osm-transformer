package eu.fusepool.p3.osm;

import org.wymiwyg.commons.util.arguments.ArgumentHandler;

import eu.fusepool.p3.transformer.sample.Arguments;
import eu.fusepool.p3.transformer.server.TransformerServer;


public class Main {
        public static void main(String[] args) throws Exception {
            Arguments arguments = ArgumentHandler.readArguments(Arguments.class, args);
            if (arguments != null) {
                start(arguments);
            }
        }

        private static void start(Arguments arguments) throws Exception {
            TransformerServer server = new TransformerServer(arguments.getPort(), false);        
            server.start(new OsmTransformerFactory());       
            server.join();
        }

}
