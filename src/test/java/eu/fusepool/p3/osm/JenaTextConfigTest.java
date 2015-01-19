package eu.fusepool.p3.osm;

import static org.junit.Assert.*;

import org.apache.jena.atlas.lib.StrUtils;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.sparql.util.QueryExecUtils;

public class JenaTextConfigTest {
    
    JenaTextConfig jena = null;
    Dataset dataset = null;
    
    private static final Logger log = LoggerFactory.getLogger(JenaTextConfigTest.class);

    @Before
    public void setUp() throws Exception {
        jena = new JenaTextConfig();
        dataset = jena.getDataset();
    }

    @Test
    public void testLoadData() {
        String file = getClass().getResource("data.ttl").getFile();
        jena.loadData(dataset, file);
        queryData(dataset);
    }
    
    private void queryData(Dataset dataset){
        log.info("START") ;
        
        long startTime = System.nanoTime() ;
        
        String pre = StrUtils.strjoinNL( "PREFIX : <http://example/>"
        , "PREFIX text: <http://jena.apache.org/text#>"
        , "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>") ;
        String qs = StrUtils.strjoinNL( "SELECT * "
        , " { ?s text:query (rdfs:label 'X1') ;"
        , " rdfs:label ?label"
        , " }") ;
        
        dataset.begin(ReadWrite.READ) ;
        try {
            Query q = QueryFactory.create(pre+"\n"+qs) ;
            QueryExecution qexec = QueryExecutionFactory.create(q , dataset) ;
            QueryExecUtils.executeQuery(q, qexec) ;
        } 
        finally { 
            dataset.end() ; 
        }
        long finishTime = System.nanoTime() ;
        double time = (finishTime-startTime)/1.0e6 ;
        log.info(String.format("FINISH - %.2fms", time)) ;
    }

}
