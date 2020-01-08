package util.registry;

import java.util.Objects;


class Key {
    private final String keyValue;
    
    Key(final String keyValue) {
        this.keyValue = Objects.requireNonNull(keyValue, "key value");
    }
    
    @Override
    public String toString() {
        return keyValue;
    }
    
    @Override
    public int hashCode() {
        /* Ditching first hashCode() match implemented by Map.containsKey(Object)
         * 		to force keyValue (String) content match */
        return 0;
    }
    
    @Override
    public boolean equals(Object obj) {
        if(obj == this)
            return true;        
        if(! (obj instanceof Key) )
            return false;        
        return ((Key)obj).keyValue.equalsIgnoreCase(this.keyValue);
    }
}