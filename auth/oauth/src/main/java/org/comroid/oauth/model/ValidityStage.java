package org.comroid.oauth.model;

public interface ValidityStage {
    boolean isValid();

    boolean invalidate();

    default void checkValid() throws IllegalAccessError {
        if (!isValid())
            throw new IllegalAccessError(this + " is not valid any more");
    }
}
