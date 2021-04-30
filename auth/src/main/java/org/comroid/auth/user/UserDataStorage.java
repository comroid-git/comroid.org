package org.comroid.auth.user;

import org.comroid.common.io.FileHandle;
import org.comroid.common.io.FileProcessor;

import java.io.IOException;
import java.util.UUID;

public class UserDataStorage implements FileProcessor {
    @Override
    public UUID getUUID() {
        return null;
    }

    @Override
    public FileHandle getFile() {
        return null;
    }

    public UserDataStorage(UserAccount userAccount) {
    }

    @Override
    public int storeData() throws IOException {
        return 0;
    }

    @Override
    public int reloadData() throws IOException {
        return 0;
    }
}
