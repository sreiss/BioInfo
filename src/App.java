import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.RateLimiter;
import com.google.inject.*;
import controllers.MainController;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import services.contracts.*;
import services.impls.*;
import java.util.concurrent.Executors;

public class App extends AbstractModule {

    @Override
    protected void configure() {
        bind(HttpService.class).to(DefaultHttpService.class).asEagerSingleton();
        bind(HttpTransport.class).to(NetHttpTransport.class).asEagerSingleton();
        bind(ParseService.class).to(DefaultParseService.class).asEagerSingleton();
        bind(FileService.class).to(DefaultFileService.class).asEagerSingleton();
        bind(ConfigService.class).to(DefaultConfigService.class).asEagerSingleton();
        bind(StatisticsService.class).to(DefaultStatisticsService.class).asEagerSingleton();
        bind(GeneService.class).to(DefaultGeneService.class).asEagerSingleton();
        bind(OrganismService.class).to(DefaultOrganismService.class).asEagerSingleton();
        bind(KingdomService.class).to(DefaultKingdomService.class).asEagerSingleton();
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
    @Singleton
    ListeningExecutorService providelisteningExecutorService() {
        return MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(60));
    }

    public static void main(String[] args) {
        Injector injector = Guice.createInjector(new App());

        MainController mainController = injector.getInstance(MainController.class);
    }
}
