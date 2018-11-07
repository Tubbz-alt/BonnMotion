package edu.bonn.cs.iv.bonnmotion.models.TIMM_Tools;

/**
 * 
 * An exception class for exceptions taking place in settings classes. Most common: settings set
 * with wrong values.
 */
public class SettingsException extends Exception {
    private static final long serialVersionUID = 7400729020356176157L;

    public SettingsException() {
    }

    public SettingsException(String msg) {
        super(msg);
    }
    
    public SettingsException(String msg, boolean fatal) {
        super(msg);
        if (fatal) {
            System.exit(-1);
        }
    }

    public SettingsException(String name, String value, String howValueShouldBe) {
        super(String.format("Error: %s is %s but should be %s", name, value, howValueShouldBe));
    }
    
    public SettingsException(String name, String value, String howValueShouldBe, boolean fatal) {
        super(String.format("Error: %s is %s but should be %s", name, value, howValueShouldBe));
        if (fatal) {
            System.exit(-1);
        }
    }    
}