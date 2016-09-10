package services.contracts;

import java.util.concurrent.Callable;

public interface UtilService {
    class VoidCallable implements Callable<Void> {
        @Override
        public Void call() throws Exception {
            return null;
        }
    }
}
