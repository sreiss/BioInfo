import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.RateLimiter;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import controllers.MainController;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import services.contracts.*;
import services.impls.*;
import java.util.concurrent.Executors;

public class App extends AbstractModule {

    @Override
    protected void configure() {
        bind(HttpService.class).to(DefaultHttpService.class);
        bind(HttpTransport.class).to(NetHttpTransport.class);
        bind(ParseService.class).to(DefaultParseService.class);
        bind(FileService.class).to(DefaultFileService.class);
        bind(ConfigService.class).to(DefaultConfigService.class);
        bind(StatisticsService.class).to(DefaultStatisticsService.class);
        bind(GeneService.class).to(DefaultGeneService.class);
        bind(OrganismService.class).to(DefaultOrganismService.class);
        bind(KingdomService.class).to(DefaultKingdomService.class);
        bind(ProgressService.class).to(DefaultProgressService.class).asEagerSingleton();
    }

    @Provides
    RateLimiter provideRateLimiter() {
        return RateLimiter.create(2);
    }

    @Provides
    CloseableHttpAsyncClient provideHttpAsyncClient() {
        return HttpAsyncClients.createDefault();
    }

    @Provides
    ListeningExecutorService providelisteningExecutorService() {
        return MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(16));
    }

    public static void main(String[] args) {
        Injector injector = Guice.createInjector(new App());

        MainController mainController = injector.getInstance(MainController.class);
    }
}
