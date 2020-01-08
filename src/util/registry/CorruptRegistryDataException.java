package util.registry;

import java.nio.file.Path;
import java.util.Objects;


public class CorruptRegistryDataException extends Exception {
    private final String corruptDataRow, errorDetail, fileLoadedFrom;
    
    /**
     * Sole constructor of this exception.
     * <p> Denotes corrupt (in unrecognizable format) registry data. </p>
     *
     * @param errorDetail Error detail.
     * @param corruptDataRow Corrupt data row.
     * @param fileLoadedFrom File the data is loaded from.
     * @throws NullPointerException If any of the parameters is null.
     * */
    CorruptRegistryDataException(final String errorDetail, final String corruptDataRow, final Path fileLoadedFrom) {
        this.errorDetail    = Objects.requireNonNull(errorDetail, "error detail");
        this.corruptDataRow = Objects.requireNonNull(corruptDataRow, "corrupt data row");
        this.fileLoadedFrom = Objects.requireNonNull(fileLoadedFrom, "file loaded from").toString();
    }
    
    public String getErrorDetail() { return errorDetail; }
    
    public String getCorruptRow() { return corruptDataRow; }
    
    public String getFileLoadedFrom() { return fileLoadedFrom; }
    
    @Override
    public String getMessage() {
        return  String.format(  "  File loaded from: %s \n" +
                                "  Corrupt row: %s \n" +
                                "  Error detail: %s \n",
                                    fileLoadedFrom,
                                    corruptDataRow,
                                    errorDetail);
    }
    
    @Override
    public String toString() {
        return getMessage();
    }
}