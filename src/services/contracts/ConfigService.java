package services.contracts;

import org.jdeferred.Promise;

public interface ConfigService {
    Promise<String, Throwable, Void> getProperty(String name);
}
