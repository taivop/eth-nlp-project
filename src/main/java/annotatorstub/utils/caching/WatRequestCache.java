package annotatorstub.utils.caching;

import annotatorstub.utils.Pair;

import java.io.IOException;
import java.net.URL;

public class WATRequestCache extends SimpleCache<Pair<URL, String>, String> {
    public WATRequestCache(String cacheFilename, String name, int flushEvery) throws
        IOException {
        super(cacheFilename, name, flushEvery);
    }
}
