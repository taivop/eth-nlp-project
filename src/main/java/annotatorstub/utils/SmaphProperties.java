package annotatorstub.utils;

import annotatorstub.annotator.smaph.SmaphSAnnotator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Properties;

public class SmaphProperties {

    private static final Logger logger = LoggerFactory.getLogger(SmaphProperties.class);

    private String bingApiKey;

    public SmaphProperties(String filename) throws IOException {

        Properties prop = new Properties();
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filename);

        if (inputStream != null) {
            prop.load(inputStream);
        } else {
            throw new FileNotFoundException("property file '" + filename + "' not found in the classpath");
        }

        // Load the property values
        this.bingApiKey = prop.getProperty("bingApiKey");
    }

    public String getBingApiKey() {
        return bingApiKey;
    }
}
