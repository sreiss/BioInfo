package services.impls;

import com.google.inject.Inject;
import services.contracts.ConfigService;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.Callable;

public class DefaultConfigService implements ConfigService {

    @Inject
    public DefaultConfigService() {

    }

    @Override
    public String getProperty(final String name) {
        Properties properties = new Properties();
        String propertiesFileName = "config.properties";
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(propertiesFileName);

        try {
            properties.load(inputStream);

            String property = properties.getProperty(name);
            inputStream.close();
            return property;
        } catch(IOException e) {
            return null;
        }
    }
}
