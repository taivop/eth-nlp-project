package annotatorstub.utils.caching;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

public class SimpleCache<K, V> implements Closeable {
    private final static Logger logger = LoggerFactory.getLogger(SimpleCache.class);

    private Map<K, V> cache = new HashMap<>();
    private int flushEvery;
    private String cacheFilename;
    private String name;

    private int flushCounter;

    public SimpleCache(String cacheFilename, String name, int flushEvery) throws IOException {
        this.cacheFilename = cacheFilename;
        this.name = name;
        this.flushEvery = flushEvery;

        try {
            load();
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException("Attempted to load incompatible serialized cache.", e);
        }
    }

    public boolean containsKey(K key) {
        return cache.containsKey(key);
    }

    public V get(K key) {
        return cache.get(key);
    }

    /**
     *
     * @return true if a flush occurred, and false otherwise.
     * @throws IOException If a flush is triggered buy fails.
     */
    public boolean put(K key, V value) throws IOException {
        // TODO-LOW(andrei): This should maybe also be synchronized in a general use case.
        cache.put(key, value);
        flushCounter += 1;

        if (flushCounter % flushEvery == 0 && cacheFilename != null) {
            flush();
            return true;
        }

        return false;
    }

    public synchronized void flush() throws IOException {
            logger.info("Flushing cache [{}]...", name);
//            new File(cacheFilename).createNewFile();
            try(
                FileOutputStream fileOut = new FileOutputStream(cacheFilename);
                ObjectOutputStream oos = new ObjectOutputStream(fileOut)
            ){
                oos.writeObject(cache);
            }
            logger.info("Finished flushing cache [{}].", name);
    }

    @Override
    public void close() throws IOException {
        flush();
    }

    @SuppressWarnings("unchecked")
    private void load() throws IOException, ClassNotFoundException {
        logger.info("Loading cache [{}] from [{}].", name, cacheFilename);
        if (new File(cacheFilename).exists()) {
            logger.info("Found cache file to load.");
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(cacheFilename));
            cache = (Map<K, V>) ois.readObject();
            ois.close();
            logger.info("Loaded cache file with {} entries.", this.cache.size());
        }
        else {
            logger.info(
                "No cache found in file {} for {}. Will write to it on the next flush.",
                cacheFilename,
                name);
        }

    }
}
