package tester;

import util.registry.Registry;
import util.registry.CorruptRegistryDataException;
import static util.registry.Registry.getInValidKeyOrValueFormat;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;


/**
 * Main caller for Registry
 * */
public class Main {
    private static final float   APP_VERSION = 1.0f;
    private static final boolean debugModeEnabled = false;
    
    static {
        if(debugModeEnabled)
            System.out.println("  *** [ Disable debug mode before final deployment ] ***");
    }
    
    private static boolean  verboseModeEnabled, resetDb, repairDb,
                            mergeDb, isExternalDb, entryMode, forceEntry,
                            queryMode, interactiveModeEnabled = true;
    private static String   operationMode = null;
    private static String[] keyValPair = null;
    private static String   queryKey, mergeToDbFromFileName,
                            dbFileName =
                                System.getProperty("java.io.tmpdir") +
                                        File.separator +
                                            "registry/data/db"; /* default value set - centralised db file */
    
    public static void main(String[] args) {
        Registry registry = null;
        
        try {
            try {
                setOptions(args);
                switch(operationMode) {
                    case "reset-db": /* For db reset */
                        registry = Registry.forDbReset(dbFileName);
                        break;
                        
                    case "repair-db": /* For db repair */
                        registry = Registry.forDbRepair(dbFileName, isExternalDb);
                        break;
                    
                    case "merge-to-db": /* For db merge */
                        registry = Registry.forDbMerge(dbFileName, mergeToDbFromFileName);
                        break;
                        
                    case "entry": /* For db entry */
                        registry = Registry.forDbEntry(forceEntry, keyValPair, dbFileName, isExternalDb);
                        break;
                        
                    case "query": /* For db query */
                        registry = Registry.forDbQuery(queryKey, dbFileName, isExternalDb);
                        break;
                        
                    default:
                        throw new AssertionError("Should not get here: " +
                                                    "Invalid operationMode value=" + operationMode);
                }
            } catch(IllegalArgumentException e) {
                showError(e, "Error: Invalid argument: ");
                System.exit( StandardExitCodes.ERROR );
            }
            
            $diagnoseOptionsAndArguments(); /* for developer diagnostics only */
            String response = registry.process();
            if(response != null)
                System.out.println(response);
            
        } catch(IOException e) {
            showError(e, "I/O Error: ");
            System.exit( StandardExitCodes.FILE );
        } catch(IllegalArgumentException e) {
            showError(e, "Error: Invalid argument: ");
            System.exit( StandardExitCodes.ERROR );
        } catch(CorruptRegistryDataException e) {
            showError(e, "Error: Registry data corrupted! \n");
            System.err.println("Suggestion: Either repair or reset registry database to avoid future errors");
            System.exit(StandardExitCodes.FILE);
        } catch(Throwable t) { /* Catching Throwable to catch both errors and exceptions */
            System.err.println("Fatal Error: Unknown application error");
            System.err.println("!Contact developers!");
            showError(t, "Error details: ");
            
            if(debugModeEnabled) {
                System.err.println("\nFull error stacktrace:");
                t.printStackTrace();
            }
            
            System.exit( StandardExitCodes.FATAL );
        }
    }
    
    private static void showError(final Throwable error, final String headerMessage) {
        if(error == null)
            throw new AssertionError("Should not get here: " + "Thrown error is null");
        
        System.err.println(headerMessage + error.getMessage());
        
        Throwable causeError = error.getCause();
        if(causeError != null)
            System.err.println("Cause: " +
                    causeError.getClass().getSimpleName() +
                    " (" + causeError.getMessage() + ")");
        for(Throwable err : error.getSuppressed()) {
            System.err.println("Error: " +
                    err.getClass().getSimpleName() +
                    " (" + err.getMessage() + ")");
        }
    }
    
    public static void verbose(final String line, final Object... args) {
        if(verboseModeEnabled) {
            System.out.printf("  [" + line + "] %n", args);
        }
    }

    /**
     * @throws IOException For any I/O error.
      * @throws IllegalArgumentException For any invalid argument.
     */	
    private static void setOptions(final String[] args) throws  IOException, IllegalArgumentException {
        String requireArgumentForOption = null, requireOptionalArgumentForOption = null;
        for(String arg : args) {
            if(requireArgumentForOption != null) {
                switch(requireArgumentForOption) {
                    case "--merge-to-db":
                        if(arg == null || arg.trim().length() == 0 || arg.startsWith("-"))
                            throw new IllegalArgumentException("Invalid external source database name: " + arg);
                        mergeToDbFromFileName = arg;
                        break;
                    
                    case "--db": /* get external db name */
                        if(arg == null || arg.trim().length() == 0 || arg.startsWith("-"))
                            throw new IllegalArgumentException("Invalid external database name: " + arg);
                        dbFileName = arg;
                        isExternalDb = true;
                        break;
                        
//					case 'q':
//						if(arg.startsWith("-"))
//							throw new IllegalArgumentException("Wrong key name (should not start with a hyphen (-))");
//						queryKey = arg;
//						break;
                    
                    default:
                        throw new AssertionError("Should not get here: " +
                                                    "Invalid requireArgumentForOption value=" + requireArgumentForOption);
                }
                requireArgumentForOption = null; /* reset after each use */
            } else {
                switch(arg) {
                    case "-v":
                    case "--verbose":
                        verboseModeEnabled = true;
                        break;
                    
                    case "-n":
                    case "--dnd":
                        interactiveModeEnabled = false;
                        break;
                        
                    case "-R":
                    case "--reset-db":
                        resetDb = true;
                        break;
                        
                    case "-r":
                    case "--repair-db":
                        repairDb = true;
//						requireOptionalArgumentForOption = 'r';
                        break;
                        
                    case "-m":
                    case "--merge-to-db":
                        mergeDb = true;
                        requireArgumentForOption = "--merge-to-db";
                        break;
                        
                    case "-d":
                    case "--db":
                        requireArgumentForOption = "--db";
                        break;
                    
                    case "-e":
                    case "--entry":
                        entryMode = true;
                        requireOptionalArgumentForOption = "--entry";
                        break;
                    
                    case "-f":
                    case "--force-entry":
                        entryMode = forceEntry = true;
                        requireOptionalArgumentForOption = "--force-entry";
                        break;
                            
                    case "-q":
                    case "--query":
                        queryMode = true;
                        requireOptionalArgumentForOption = "--query";
                        break;
                            
                    case "-h":
                    case "--help":
                        showHelpAndExit();
                        
                    default:
                        if(requireOptionalArgumentForOption == null)
                            throw new IllegalArgumentException(arg);
                        switch(requireOptionalArgumentForOption) {
//							case 'r':
//								dbFileName = arg;
//								isExternalDb = true;
//								break;
                                
                            case "--query":
                                if(queryKey == null) {
                                    queryKey = getInValidKeyOrValueFormat(arg);
                                    if(queryKey == null)
                                        throw new IllegalArgumentException("Invalid query key format: " + arg);
                                }
                                else
                                    throw new IllegalArgumentException("Query key already provided: " + queryKey);
                                break;
                                
                            case "--entry":
                            case "--force-entry":
                                if(keyValPair == null) {
                                    keyValPair = new String[2];
                                    keyValPair[0] = getInValidKeyOrValueFormat(arg);
                                    if(keyValPair[0] == null)
                                        throw new IllegalArgumentException("Invalid key format: " + arg);
                                    continue; /* skip to avoid resetting the value of requireOptionalArgumentForOption */
                                }
                                if(keyValPair[0] != null && keyValPair[1] != null) {
                                    throw new IllegalArgumentException("Key and value pair already provided: " +
                                            Arrays.toString(keyValPair));
                                }
                                keyValPair[1] = getInValidKeyOrValueFormat(arg);
                                if(keyValPair[1] == null)
                                    throw new IllegalArgumentException("Invalid value format: " + arg);
                                break;
                                
                            default:
                                throw new AssertionError("Should not get here: " +
                                        "Invalid requireOptionalArgumentForOption value=" + requireOptionalArgumentForOption);
                        }
                        requireOptionalArgumentForOption = null; /* reset after each use */
                }
            }
        }
        
//		verbose("*** Verbose mode enabled ***");
        
        if(requireArgumentForOption != null) {
            throw new IllegalArgumentException("Argument not specified for provided option: " + requireArgumentForOption);
        }
        
        checkOptionValidity();
        
        if(operationMode == null) {
            throw new IllegalArgumentException("No valid operation provided");
        }
        
        /* for any user input */
        switch(operationMode) {
            case "entry":
                if(keyValPair == null) {
                    keyValPair = new String[] { readUserInput("KEY:   "), readUserInput("VALUE: ") };
                } else if(keyValPair[1] == null) {
                    keyValPair[1] = readUserInput("VALUE: ");
                }
                break;
                
            case "reset-db":
                if(interactiveModeEnabled) {
                    if(!readUserInput("Critical operation: Sure to reset registry database? [Y for Yes]  ").trim().equals("Y")) {
                        System.out.println("    [Aborting operation...]");
                        System.exit( StandardExitCodes.NORMAL );
                    }
                }
                break;
            
            case "merge-to-db":
                if(interactiveModeEnabled) {
                    if(!readUserInput("Critical operation: Sure to merge file data in default registry database? [Y for Yes]  ").trim().equals("Y")) {
                        System.out.println("    [Aborting operation...]");
                        System.exit( StandardExitCodes.NORMAL );
                    }
                }
                break;
                
            case "query":
                if(queryKey == null) {
                    queryKey = readUserInput("KEY: ");
                }
                break;
                
            default:
                if(!operationMode.equals("repair-db")) { /* db resetting requires no user input */
                    throw new AssertionError("Should not get here: " +
                                            "Invalid operationMode value=" + operationMode);
                }
        }
    }

    /**
     * @throws IOException  For any I/O related error.
     */	
    private static String readUserInput(final String msg) throws IOException {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new InputStreamReader( System.in ));
            System.out.print(msg);
            String input;
            while( (input = reader.readLine()) == null || input.trim().length() == 0 ) { // check in case of null or blank input
                System.out.print("\n" + msg);
            }
            return input;
        } catch(IOException e) {
            throw new IOException("Unable to read input from user", e);
        }
    }
    
    /**
     * @throws IllegalArgumentException For invalid option combination.
     */
    private static void checkOptionValidity() throws IllegalArgumentException {
        /*
         * Checks the option validities:
         * Only permit these combinations for the following operations:
         *        - verboseModeEnabled: (ignore)
         *        - interactiveModeEnabled: (ignored)
         *        - db reset:     {resetDb}
         *        - db repair:    {repairDb}, [dbFileName]
         *        - db merge:     {mergeDb}, dbFileName
         *        - db query:     {queryMode}, queryKey, [dbFileName]
         *        - db entry:     {entryMode}, [forceEntry], [keyValPair]
         */
        
        if(resetDb) {
            operationMode = "reset-db";
//			System.out.println("  [in "+operationMode+"]");
            if(repairDb || mergeDb || isExternalDb || entryMode || forceEntry || queryMode)
                throw new IllegalArgumentException("Wrong option combinations: " + getInvalidOptionCombinations());
            return;
        }
        
        if(repairDb) {
            operationMode = "repair-db";
//			System.out.println("  [in "+operationMode+"]");
            if(resetDb || mergeDb || entryMode || forceEntry || queryMode)
                throw new IllegalArgumentException("Wrong option combinations: " + getInvalidOptionCombinations());
            return;
        }
        
        if(mergeDb) {
            operationMode = "merge-to-db";
//			System.out.println("  [in "+operationMode+"]");
            if(resetDb || repairDb || isExternalDb || entryMode || forceEntry || queryMode)
                throw new IllegalArgumentException("Wrong option combinations: " + getInvalidOptionCombinations());
            return;
        }
        
        if(queryMode) {
            operationMode = "query";
//			System.out.println("  [in "+operationMode+"]");
            if(resetDb || repairDb || mergeDb || entryMode || forceEntry)
                throw new IllegalArgumentException("Wrong option combinations: " + getInvalidOptionCombinations());
            return;
        }
        
        if(entryMode) {
            operationMode = "entry";
//			System.out.println("  [in "+operationMode+"]");
            if(resetDb || repairDb || mergeDb || queryMode)
                throw new IllegalArgumentException("Wrong option combinations: " + getInvalidOptionCombinations());
            return;
        }
    }
    
    private static String getInvalidOptionCombinations() {
        StringBuilder sbOptionCombinations = new StringBuilder();
        int optionCounter = 0;
        
        if(resetDb) {
            sbOptionCombinations.append("--reset-db");
            optionCounter++;
        }
        
        if(repairDb) {
            if(optionCounter > 0)
                sbOptionCombinations.append(", ");
            sbOptionCombinations.append("--repair-db");
            optionCounter++;
        }
        
        if(mergeDb) {
            if(optionCounter > 0)
                sbOptionCombinations.append(", ");
            sbOptionCombinations.append("--merge-to-db");
            optionCounter++;
        }
        
        if(entryMode) {
            if(optionCounter > 0)
                sbOptionCombinations.append(", ");
            sbOptionCombinations.append("--entry");
            optionCounter++;
        }
        
        if(forceEntry) {
            if(optionCounter > 0)
                sbOptionCombinations.append(", ");
            sbOptionCombinations.append("--force-entry");
            optionCounter++;
        }
        
        if(queryMode) {
            if(optionCounter > 0)
                sbOptionCombinations.append(", ");
            sbOptionCombinations.append("--query");
            optionCounter++;
        }
        
        if(isExternalDb) {
            if(optionCounter > 0)
                sbOptionCombinations.append(", ");
            sbOptionCombinations.append("--db");
            optionCounter++;
        }
    
        sbOptionCombinations.append(" [" + optionCounter + "]");
        return sbOptionCombinations.toString();
    }
    
    /** for developer diagnostics only */
    private static void $diagnoseOptionsAndArguments() {
        if(debugModeEnabled) {
            System.out.println("Options status:");
            System.out.println("++++++++++++++++++++++++++++");
            System.out.println("verboseModeEnabled=" + verboseModeEnabled);
            System.out.println("resetDb=" + resetDb);
            System.out.println("repairDb=" + repairDb);
            System.out.println("mergeDb=" + mergeDb);
            System.out.println("isExternalDb=" + isExternalDb);
            System.out.println("entryMode=" + entryMode);
            System.out.println("forceEntry=" + forceEntry);
            System.out.println("queryMode=" + queryMode);
            System.out.println("interactiveModeEnabled=" + interactiveModeEnabled);
            System.out.println("operationMode=" + operationMode);
            System.out.println("keyValPair=" + Arrays.toString(keyValPair));
            System.out.println("queryKey=" + queryKey);
            System.out.println("mergeToDbFromFileName=" + mergeToDbFromFileName);
            System.out.println("dbFileName=" + dbFileName);
            System.out.println("----------------------------");
        }
    }
    
    private static void showHelpAndExit()
    {
        System.out.printf(
                        "Registry \n" +
                        "Version: %.2f \n" +
                        "Purpose: Maintains a registry in a key value pair \n" +
                        "Usage:   Reg [-<option1>[ -<option2...>]] [filename] \n" +
                        "Default registry database file: %s \n\n" +
                        
                        "Options: \n" +
                        "    --verbose, -v                  Enables verbose mode \n" +
                        "    --dnd, -n                      (Non-interactive mode) No prompts for confirmation in any critical operations \n" +
                        "    --reset-db, -R                 Reset whole registry database \n" +
                        "    --repair-db, -r                Delete only corrupted data from database \n" +
                        "    --merge-to-db, -m <file-name>  Includes file's contents into registry database \n" +
                        "    --db, -d <file-name>           Selects file as registry database (overrides default database) \n" +
                        "    --entry, -e [<key>] [<value>]  Enter key-value pair as entry in registry database \n" +
                        "    --force-entry, -f              Force entry of key-value pair if already exists in registry database \n" +
                        "    --query, -q <key>              Query key from registry database \n" +
                        "    --help, -h                     Shows this help menu \n\n",
                        APP_VERSION, dbFileName);
        StandardExitCodes.showMessage();
        System.exit(StandardExitCodes.NORMAL);
    }
}
