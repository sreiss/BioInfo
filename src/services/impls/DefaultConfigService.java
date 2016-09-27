package services.impls;

import com.google.inject.Inject;
import org.jdeferred.DeferredManager;
import org.jdeferred.FailCallback;
import org.jdeferred.Promise;
import services.contracts.ConfigService;

import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.Callable;

public class DefaultConfigService implements ConfigService {
    private final DeferredManager deferredManager;

    @Inject
    public DefaultConfigService(DeferredManager deferredManager) {
        this.deferredManager = deferredManager;
    }

    @Override
    public Promise<String, Throwable, Void> getProperty(final String name) {
        return deferredManager.when(new Callable<String>() {
            @Override
            public String call() throws Exception {
                Properties properties = new Properties();
                String propertiesFileName = "config.properties";
                InputStream inputStream = getClass().getClassLoader().getResourceAsStream(propertiesFileName);

                properties.load(inputStream);

                String property = properties.getProperty(name);
                inputStream.close();

                return property;
            }
        });
    }
}
