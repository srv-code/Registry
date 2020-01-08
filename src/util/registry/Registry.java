package util.registry;

import static tester.Main.verbose;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Objects;
import java.util.Hashtable;


public class Registry {
    private final Map<Key,String>   registryMap = new Hashtable<>();
    private final String            operationMode;
    private boolean                 mapHasUpdated;
    private final boolean           resetDb, repairDb, mergeDb, isExternalDb,
                                    entryMode, forceEntry, queryMode;
    private final String[]          keyValPair;
    private final Key               queryKey;
    private Path                    dbFile, mergeSrcFile;
    
    /**
     * Sole private constructor.
     * <p> Simply initializes the object internal properties. </p>
     * */
    public Registry(    final String    operationMode,
                        final boolean   resetDb,
                        final boolean   repairDb,
                        final boolean   mergeDb,
                        final String    mergeToDbFromFileName,
                        final String    dbFileName,
                        final boolean   isExternalDb,
                        final boolean   entryMode,
                        final boolean   forceEntry,
                        final String[]  keyValPair,
                        final boolean   queryMode,
                        final String    queryKey) {
        /* Sets internal object properties */
        this.operationMode  = operationMode;
        this.resetDb        = resetDb;
        this.repairDb       = repairDb;
        this.mergeDb        = mergeDb;
        this.mergeSrcFile   = mergeToDbFromFileName == null ? null : Paths.get(mergeToDbFromFileName);
        this.dbFile         = Paths.get(dbFileName);
        this.isExternalDb   = isExternalDb;
        this.entryMode      = entryMode;
        this.forceEntry     = forceEntry;
        this.keyValPair     = keyValPair;
        this.queryMode      = queryMode;
        this.queryKey       = queryKey == null ? null : new Key(queryKey);
    }
    
    
    /* Factory methods */
    
    /**
     * To get Registry object to repair registry database.
     * <p> Repair operation deletes the corrupted rows from registry file
     * (rows that doesn't comply to the internal recognizable data structures). </p>
     * <p> It will apply best efforts to not to delete the other rows if possible. </p>
     * @param dbFileName Name of registry database to load data from.
     * @param isExternalDb States if an external registry database is
      *                        specified, ignores the default registry database
     * @return Registry object to carry out the repair operation.
     * @throws NullPointerException In case any of the object parameter is null.
     * */
    public static Registry forDbRepair( String dbFileName,
                                        boolean isExternalDb) throws NullPointerException {
        return new Registry(    "repair-db",
                                false, true, false, null,
                                Objects.requireNonNull(dbFileName, "database file name"),
                                isExternalDb,
                                false, false, null, false, null);
    }
    
    /**
     * To get Registry object to reset registry database.
     * <p> Reset operation simply truncates the default registry
     * database file to zero size. </p>
     * @param dbFileName Name of registry database to load data from.
     * @return Registry object to carry out the reset operation.
     * @throws NullPointerException In case any of the object parameter is null.
     * */
    public static Registry forDbReset(String dbFileName) throws NullPointerException {
        return new Registry(    "reset-db",
                                true,
                                false, false, null,
                                Objects.requireNonNull(dbFileName, "database file name"), 
                                false, false, false, null, false, null);
    }
    
    /**
     * To get Registry object to merge from provided source
     * file to application default registry database.
     * <p> Merge operation appends all valid rows from specified
     * source database file to application default database file. </p>
     * @param dbFileName Name of registry database to load data from.
     * @param mergeToDbFromFileName Source filename to merge the data from.
     * @return Registry object to carry out the merge operation.
     * @throws NullPointerException In case any of the object parameter is null.
     * */
    public static Registry forDbMerge( String dbFileName,
                                String mergeToDbFromFileName) throws NullPointerException {
        return new Registry(    "merge-to-db",
                                false, false, true, 
                                Objects.requireNonNull(mergeToDbFromFileName, "source file to merge from"),
                                Objects.requireNonNull(dbFileName, "database file name"), 
                                false, false, false, null, false, null);
    }
    
    /**
     * To get Registry object to query from registry database.
     * <p> Query operation queries the specified/default registry
     * database and returns the value as result if found else
     * returns null </p>.
     * @param queryKey Key whose corresponding value is queried.
     * @param dbFileName Name of registry database to load data from.
     * @param isExternalDb States if an external registry database is
     *                        specified, ignores the default registry database
     * @return Registry object to carry out the query operation.
     * @throws NullPointerException In case any of the object parameter is null.
     * */
    public static Registry forDbQuery( String queryKey,
                                String dbFileName,
                                boolean isExternalDb) throws NullPointerException {
        return new Registry(    "query",
                                false, false, false, null,
                                Objects.requireNonNull(dbFileName, "database file name"),
                                isExternalDb,
                                false, false, null, true, 
                                Objects.requireNonNull(queryKey, "query key"));
    }
    
    /**
     * To get Registry object to provide entry into registry database.
     * <p> Entry operation inserts provided key and value pair into the
     * specified/default registry database. </p>
     * <p> Note: Null or blank value is not allowed for either key or
     * value. </p>
     * @param forceEntry Forces replacement of the value if the key is
     *                      already present in the registry database.
     * @param keyValuePair The key and value pair in a string array.
     * @param dbFileName Name of registry database to load data from.
     * @param isExternalDb States if an external registry database is
     *                       specified, ignores the default registry database
     * @return Registry object to carry out the merge operation.
     * @throws NullPointerException In case any of the object parameter is null.
     * */
    public static Registry forDbEntry( boolean forceEntry,
                                String[] keyValuePair,
                                String dbFileName,
                                boolean isExternalDb) throws NullPointerException {
        return new Registry(    "entry",
                                false, false, false, null,
                                Objects.requireNonNull(dbFileName, "database file name"),
                                isExternalDb,
                                true, 
                                forceEntry, 
                                Objects.requireNonNull(keyValuePair, "entry key-value pair"), 
                                false, null);
    }

    /**
     * <p> Entry point of object to start processing the specified operation. </p>
     * <p> External operations: </p>
     * <p>    - resetDb -> updateDb() </p>
     * <p>    - repairDb -> updateDb() </p>
     * <p>    - copAsDb -> updateDb() </p>
     * <p>    - loadAsDb -> loadDb() </p>
     * <p>    - entryInDb(boolean force) -> updateDb() </p>
     * <p>    - queryFromDb -> queryDb() </p>
     * <p> </p>
     * <p> Internal operations: </p>
     * <p>    - updateDb(): reset, repair, merge, entry/update </p>
     * <p>    - loadDb(): loads data from designated file </p>
     * <p>    - queryDb(): queries from loaded data and returns </p>
     * <p> </p>
     * <p> Performs operations as required: </p>
     * <p>   Checks the presence of default and/or external db files, creates for default db if reqd </p>
     * <p>    queryDb(): prints value if found else prints no line </p>
     * <p>     updateDb(): if key-value pair provided then enter directly (check for force entry option) else prompts for key and value </p>
     * <p>     loadDb(): checks file's presence, loads data, checks for corruption, updates internal hashtable </p>
     *
     * @throws IOException For any I/O error.
     * @throws CorruptRegistryDataException For data corruption.
     * @return Response, if necessary, else null.
     * */
    public String process() throws  IOException, CorruptRegistryDataException {
        String response = null;
        boolean requireDbLoad = !operationMode.equals("reset-db"); // db reset requires no load operation;
        
        verbose("Requested operation: %s", operationMode);
        // resolve file's presence
        if(Files.notExists(dbFile)) {
            verbose("Database file (%s) not found!", dbFile);
            if(isExternalDb)
                throw new IllegalArgumentException("External database not found: " + dbFile);
            else { // create only default database file
                requireDbLoad = false; // disable db load operation
                verbose("Creating database file (%s)...", dbFile);
                // create parent dirs if required
                Path parentDirs = dbFile.getParent();
                if(parentDirs != null)
                    Files.createDirectories(parentDirs);
                Files.createFile( dbFile );
                verbose("  -- Done");
            }
        }
        
        // for db load operation
        if(requireDbLoad) {
            verbose("Loading registry database (%s)...", dbFile);
            int pairsLoaded = loadDb(dbFile, !repairDb);
            verbose("%d pair(s) loaded", pairsLoaded);
            verbose("  -- Done");
            
            if(operationMode.equals("merge-to-db")) {
                // load src file also into internal hashtable
                verbose("Loading external source file to merge from (%s)...", mergeSrcFile);
                pairsLoaded = loadDb(mergeSrcFile, false);
                mapHasUpdated = pairsLoaded > 0;
                verbose("%d new pair(s) loaded, file writing required: %b", pairsLoaded, mapHasUpdated);
                verbose("  -- Done");
                response = pairsLoaded + " new " + (pairsLoaded > 1 ? "entries" : "entry") + " merged";
            }
        }
        
        if(repairDb) {
            verbose("Db audit complete, require file writing: " + mapHasUpdated);
        }
        
        // for db query operation
        if(operationMode.equals("query") && requireDbLoad) {
            verbose("Querying registry database (%s)...", dbFile);
            response = queryDb();
            verbose("Query returned value: %b", response != null);
            verbose("  -- Done");
            // no other work, simply return the result
        }
        
        // for db entry
        if(entryMode) {
            verbose("Inserting key-value pair in internal map...");
            entryInMap();
            verbose("  -- Done");
        }
        
        // for db update operation
        if(mapHasUpdated || resetDb) { // at all cost avoid fs I/O to speedy app performance
            verbose("Updating registry database (%s)...", dbFile);
            int pairsWritten = updateDb();
            verbose("%d pair(s) written in registry database file (%s)", pairsWritten, dbFile);
            verbose("  -- Done");
        }
        
        return response;
    }

    // Internal worker methods -- all private
    
    /**
     * <p> Load key-value pairs from db into internal hashtable </p>
     * @param file File to load registry data from
     * @param raiseException True if required to throw exception in case of data corruption
     * @throws IOException For any I/O error
     * @throws CorruptRegistryDataException if data corruption detected
     * @return Count of the pairs loaded into internal hashtable
     */
    private int loadDb( final Path file, 
                        final boolean raiseException) throws  IOException, CorruptRegistryDataException {
        int pairsLoaded = 0;
        try {
            String key = null, loadedLine = null;
            for(String line : Files.readAllLines(file)) {
                try {
                    if(line.startsWith("K: ")) { /* key line found */
                        if(key == null) {
                            loadedLine = line;
                            key = getInValidKeyOrValueFormat(loadedLine.substring(3));
                            if(key == null)
                                throw new CorruptRegistryDataException("Invalid key format", loadedLine, file);
                        }
                        else
                            throw new CorruptRegistryDataException("Expecting a VALUE line", line, file);
                    } else if(line.startsWith("V: ")) {
                        if(key != null) {
                            loadedLine = line;
                            if(registryMap.containsKey(key))
                                throw new CorruptRegistryDataException("Duplicate key", key, file);
                            String value = getInValidKeyOrValueFormat(loadedLine.substring(3));
                            if(value == null)
                                throw new CorruptRegistryDataException("Invalid value format", loadedLine, file);
                            registryMap.put(new Key(key), value);
                            
                            key = null; /* prepare for next key */
                            pairsLoaded++; /* one pair completed loading */
                        } else
                            throw new CorruptRegistryDataException("Expecting a KEY line", line, file);
                    } else {
                        throw new CorruptRegistryDataException("Invalid line format", line, file);
                    }
                } catch(Exception e) {
                    mapHasUpdated = true;
                    key = null; /* prepare for next key input, reset in case db repair mode is enabled */
                    if(raiseException)
                        throw e;
                  }
            }
            
            if(key != null) {
                mapHasUpdated = true;
                if(raiseException)
                    throw new CorruptRegistryDataException("Couldn't find corresponsing value of key='" + key + "'", loadedLine, file);
            }
        } catch(IOException e) {
            throw new IOException("While loading data from file: " + file, e);
        }
        
        return pairsLoaded;
    }
    
    /**
     * @return Trimmed value else null
     * */
    public static String getInValidKeyOrValueFormat(final String arg) {
        String trimmedArg = arg == null ? null : arg.trim();
        return (trimmedArg == null || trimmedArg.length() == 0 || trimmedArg.startsWith("-")) ? null : trimmedArg;
    }
    
    /**
     * Updates internal hash table with the provided
     * key-value pair, checks force entry option
     * for replacement with new value, if any. Marks
     * for map update if modified.
     * @throws IllegalArgumentException If force entry option
     *          is disabled and a value with duplicate key
     *          is attempted to insert into internal hash table.
     * */
    private void entryInMap() throws  IllegalArgumentException {
        Key keyToInsert = new Key(keyValPair[0]);
        String valueToInsert = keyValPair[1];
        
        if(registryMap.containsKey(keyToInsert)) {
            verbose("Key already present, enforcing entry");
            if(!forceEntry)
                throw new IllegalArgumentException("Key already present: " + keyToInsert);
            if(registryMap.get(keyToInsert).equals(valueToInsert)) {
                verbose("Same value already present, file writing aborted");
                return;
            }
        }
        registryMap.put(keyToInsert, valueToInsert);
        mapHasUpdated = true;
    }
    
    /**
     * <p> Updates the registry database file </p>
     * @return Total number of pairs written on registry database
     * @throws IOException In case of any I/O error while file writing
     * */
    private int updateDb() throws IOException {
        int pairsWritten = 0;
        
        try {
            if(resetDb) {
                /* Simply write in TRUNCATE_EXISTING mode */
                Files.write(dbFile, new byte[0], StandardOpenOption.TRUNCATE_EXISTING);
            } else {
                /* For: db repair | merge | entry */
                /* Simply truncate existing file and write internal table */
                try (BufferedWriter writer =
                             Files.newBufferedWriter(dbFile,
                                     StandardOpenOption.TRUNCATE_EXISTING,
                                     StandardOpenOption.WRITE)) {
                    for(Map.Entry<Key,String> pair : registryMap.entrySet()) {
                        writer.write("K: " + pair.getKey()); writer.newLine();
                        writer.write("V: " + pair.getValue()); writer.newLine();
                        pairsWritten++;
                    }
                }
            }
        } catch(IOException e) {
            throw new IOException("While writing to registry database file (" + dbFile + ")", e);
        }
        
        mapHasUpdated = false; /* In case of any future code additions, safe option */
        return pairsWritten;
    }
    
    /**
     * <p> Queries internal hash table using key
     * @return The corresponding value to the key specified (null if not found) </p>
     */
    private String queryDb() {
        return registryMap.get(queryKey);
    }
}
