package io.myf.fortune.expansion.audience;

/** RevelationFix supplies the concrete override on AttributeInstance when installed. */
public interface DoomAttributeState {
    default boolean revelationfix$alwaysBase() {
        return false;
    }
}
