package org.comroid.auth.user;

import org.comroid.api.ContextualProvider;
import org.comroid.api.UUIDContainer;
import org.comroid.api.io.FileHandle;
import org.comroid.api.io.FileProcessor;
import org.comroid.restless.MimeType;
import org.comroid.uniform.Context;
import org.comroid.uniform.node.UniNode;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UserDataStorage extends UUIDContainer.Base implements FileProcessor {
    private final Map<String, UniNode> data = new ConcurrentHashMap<>();
    private final Context context;
    private final FileHandle dir;

    @Override
    public FileHandle getFile() {
        return dir;
    }

    private UserDataStorage(ServiceMap storage, UUID serviceId) {
        this.context = storage.upgrade(Context.class);
        this.dir = storage.getFile().createSubDir(serviceId.toString());
    }

    @Override
    public int storeData() throws IOException {
        for (Map.Entry<String, UniNode> dataEntry : data.entrySet()) {
            FileHandle file = dir.createSubFile(dataEntry.getKey() + ".json");
            file.setContent(dataEntry.getValue());
        }

        return data.size();
    }

    @Override
    public int reloadData() throws IOException {
        int c = 0;

        for (File file : dir.listFiles()) {
            String name = file.getName();
            if (!name.endsWith(".json"))
                continue;
            name = name.substring(0, name.lastIndexOf('.'));

            String content = new FileHandle(file).getContent();
            UniNode data = context.parse(MimeType.JSON, content);

            if (this.data.put(name, data) != data)
                c++;
        }

        return c;
    }

    public UniNode getData(String storageName) {
        return data.getOrDefault(storageName, null);
    }

    public UniNode putData(String storageName, UniNode data) {
        UniNode cached = getData(storageName);
        cached.copyFrom(data);
        return cached;
    }

    public static class ServiceMap extends UUIDContainer.Base implements FileProcessor, ContextualProvider.Underlying {
        private final Map<UUID, UserDataStorage> storages = new ConcurrentHashMap<>();
        private final UserAccount account;
        private final FileHandle dir;

        @Override
        public FileHandle getFile() {
            return dir;
        }

        @Override
        public ContextualProvider getUnderlyingContextualProvider() {
            return account;
        }

        public ServiceMap(UserAccount account) {
            this.account = account;
            this.dir = account.getFile().createSubDir("data");
        }

        @Override
        public int storeData() throws IOException {
            int c = 0;
            for (UserDataStorage storage : storages.values())
                c += storage.storeData();
            return c;
        }

        @Override
        public int reloadData() throws IOException {
            int c = 0;
            for (UserDataStorage storage : storages.values())
                c += storage.reloadData();
            return c;
        }

        public UserDataStorage getForService(UUID serviceId) {
            return storages.computeIfAbsent(serviceId, k -> new UserDataStorage(this, k));
        }
    }
}
