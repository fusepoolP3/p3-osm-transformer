package eu.fusepool.p3.osm;

import java.io.File;
import java.io.IOException;

import org.apache.jena.atlas.logging.LogCtl;
import org.apache.jena.query.text.EntityDefinition;
import org.apache.jena.query.text.TextDatasetFactory;
import org.apache.jena.query.text.TextQuery;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * This class is used to create an RDF data set for Jena TDB and a Lucene index. 
 * The data set and the index can be in-memory or persisted in two folders.
 * @author luigi
 *
 */
public class JenaTextConfig {
    
    File JENA_TDB_TEMP_FOLDER = null;  
    File LUCENE_INDEX_TEMP_FOLDER = null;
    Dataset osmDataset = null;

    static { LogCtl.setLog4j() ; }
    static Logger log = LoggerFactory.getLogger("JenaTextConfig") ;
    
    JenaTextConfig() throws IOException{        
        osmDataset = createMemDatasetFromCode() ;
        //createTemporaryDatasetIndexFolders();
        //osmDataset = createPersistentDatasetFromCode();
    }
    
    /**
     * Creates an in-memory Jena TDB data set and Lucene index from code.
     * @return
     */
    public Dataset createMemDatasetFromCode(){
        log.info("Construct an in-memory dataset with in-memory lucene index using code") ;
        TextQuery.init();
        // Build a text dataset by code.
        // Here , in-memory base data and in-memory Lucene index
        // Base data
        Dataset jenads = DatasetFactory.createMem() ;
        Property streetAddress = jenads.getDefaultModel().createProperty("http://schema.org/streetAddress");
        // Define the index mapping
        //EntityDefinition entDef = new EntityDefinition("uri", "text", RDFS.label.asNode()) ;
        EntityDefinition entDef = new EntityDefinition("uri", "text", streetAddress.asNode()) ;
        // Lucene, in memory.
        Directory dir = new RAMDirectory();
        // Join together into a dataset
        Dataset ds = TextDatasetFactory.createLucene(jenads, dir, entDef) ;
        return ds ;
    }
    /**
     * Creates a persistent Jena TDB data set and Lucene index. 
     * @return
     * @throws IOException 
     */
    public Dataset createPersistentDatasetFromCode() throws IOException{
        log.info("Construct a persistent Jena data set with lucene index using code") ;
        // Build a text dataset by code.
        TextQuery.init();
        // Remove old files and folders
        deleteFiles(JENA_TDB_TEMP_FOLDER);
        deleteFiles(LUCENE_INDEX_TEMP_FOLDER);
        // Creates new folders
        JENA_TDB_TEMP_FOLDER.mkdirs();
        LUCENE_INDEX_TEMP_FOLDER.mkdirs();
        // Creates persisted Jena data set and Lucene index
        Dataset jenaDataset = TDBFactory.createDataset(JENA_TDB_TEMP_FOLDER.getAbsolutePath()) ;
        // Lucene, persisted.
        Directory luceneIndex = FSDirectory.open(LUCENE_INDEX_TEMP_FOLDER);
        // Define the index mapping
        EntityDefinition entDef = new EntityDefinition("uri", "text", RDFS.label.asNode()) ;
        
        
        // Join together into a dataset
        return TextDatasetFactory.createLucene(jenaDataset, luceneIndex, entDef) ;
    }
    
    /**
     * Creates temporary folders for Jena TDB and Lucene index
     * @throws IOException 
     */
    private void createTemporaryDatasetIndexFolders() throws IOException{
        JENA_TDB_TEMP_FOLDER = File.createTempFile("jenatdb-", "-dataset");
        LUCENE_INDEX_TEMP_FOLDER = File.createTempFile("lucene-", "-index");
    }
    /**
     * Delete jena TDB and Lucene files
     * @param dir
     */
    private void deleteFiles(File dir) {
        if (dir.exists()) {
            emptyAndDeleteDirectory(dir);
        }
    }
    /**
     * Deletes recursively files and folders.
     * @param dir
     */
    private void emptyAndDeleteDirectory(File dir){  
        File[] contents = dir.listFiles();
        if (contents != null) {
            for (File content : contents) {
                if (content.isDirectory()) {
                    emptyAndDeleteDirectory(content);
                } else {
                    content.delete();
                }
            }
        }
        dir.delete();  
    }
    
    /**
     * Creates a data set from an assembler file. 
     * @return
     */
    public Dataset createDatasetFromAssembler(){
        log.info("Construct text dataset using an assembler description") ;
        // There are two datasets in the configuration:
        // the one for the base data and one with text index.
        // Therefore we need to name the dataset we are interested in.
        Dataset ds = DatasetFactory.assemble("text-config.ttl", "http://localhost/jena_example/#text_dataset") ;
        return ds ;
    }
    /**
     * Updates and re-index the data set when new graphs are imported. 
     * @param m
     */
    public void updateDataset(Model m){
      
        Dataset osmds = null;
        // re-create the indexed data set
        osmDataset.begin(ReadWrite.READ);
        try {
            Dataset jenads = DatasetFactory.createMem() ;
            
            Model jenaModel = jenads.getDefaultModel();
            Property streetAddress = jenaModel.createProperty("http://schema.org/streetAddress");
            // Define the index mapping
            //EntityDefinition entDef = new EntityDefinition("uri", "text", RDFS.label.asNode()) ;
            EntityDefinition entDef = new EntityDefinition("uri", "text", streetAddress.asNode()) ;              
            jenaModel.add(osmDataset.getDefaultModel());
            
            // Lucene, in memory.
            Directory dir = new RAMDirectory();
            // Join together into a dataset
            osmds = TextDatasetFactory.createLucene(jenads, dir, entDef) ;     
            
        }
        finally {
            osmDataset.end();
        }
        
        this.osmDataset = osmds;
    }
    
    public void loadData(Dataset dataset, String file){
        log.info("Start loading") ;
        long startTime = System.nanoTime() ;
        dataset.begin(ReadWrite.WRITE) ;
        try {
            Model m = dataset.getDefaultModel() ;
            RDFDataMgr.read(m, file) ;
            dataset.commit() ;
        } 
        finally { 
            dataset.end() ;
        }
        long finishTime = System.nanoTime() ;
        double time = (finishTime-startTime)/1.0e6 ;
        log.info(String.format("Finish loading - %.2fms", time)) ;
    }
    
    public Dataset getDataset() {
        return osmDataset;
    }
    
}
