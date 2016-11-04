package services.impls;

import com.google.api.client.http.*;
import com.google.common.util.concurrent.*;
import com.google.inject.Inject;
import services.contracts.HttpService;
import services.contracts.ProgressService;

import javax.annotation.Nullable;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

public class DefaultHttpService implements HttpService {
    private final HttpRequestFactory requestFactory;
    private final RateLimiter rateLimiter;
    private final ListeningExecutorService executorService;
    private final ProgressService progressService;
    private Integer totalRequests = 0;

    enum Method {
        GET,
        POST
    }

    @Inject
    public DefaultHttpService(HttpTransport transport, RateLimiter rateLimiter, ListeningExecutorService listeningExecutorService, ProgressService progressService) {
        this.requestFactory = transport.createRequestFactory(new HttpRequestInitializer());
        this.rateLimiter = rateLimiter;
        this.executorService = listeningExecutorService;
        this.progressService = progressService;
    }

    /**
     * Returns a promise to handle the HttpResponse.
//     */
//    private HttpResponse request(final Method method, final String url, final HttpContent content) {
//            public HttpResponse call() throws IOException {
//                rateLimiter.acquire();
//                GenericUrl genericUrl = new GenericUrl(url);
//                HttpRequest request;
//                switch (method) {
//                    case GET:
//                        request = requestFactory.buildGetRequest(genericUrl);
//                        break;
//                    case POST:
//                        request = requestFactory.buildPostRequest(genericUrl, content);
//                        break;
//                    default:
//                        throw new IllegalArgumentException("Unhandled request type.");
//                }
//                return request.execute();
//            }
//        }).then(new DonePipe<HttpResponse, HttpResponse, Throwable, Object>() {
//            @Override
//            public Promise<HttpResponse, Throwable, Object> pipeDone(HttpResponse response) {
//                System.out.println("[" + new Date() + "] Request finished : " + url);
//                return new DeferredObject<HttpResponse, Throwable, Object>().resolve(response);
//            }
//        }, new FailPipe<Throwable, HttpResponse, Throwable, Object>() {
//            @Override
//            public Promise<HttpResponse, Throwable, Object> pipeFail(Throwable throwable) {
//                // retry on fail
//                System.out.println("Request failed, retrying.");
//                return get(url);
//            }
//        });
//    }

    public ListenableFuture<HttpResponse> get(final String url) {
        ListenableFuture<HttpResponse> responseFuture = executorService.submit(() -> {
            rateLimiter.acquire();
            System.out.println("Request : " + url);
            GenericUrl genericUrl = new GenericUrl(url);
            HttpRequest request = requestFactory.buildGetRequest(genericUrl);
            return request.execute();
        });
        return responseFuture;
    }

    /**
     * Executes multiple get requests and returns a Promise.
     */
//    public Promise<List<HttpResponse>, Throwable, Object> get(final List<String> urls) {
//        return deferredManager.when(new DeferredCallable<Void, Object>() {
//            @Override
//            public Void call() throws Exception {
//                notify(new TaskProgress(urls.size()));
//                return null;
//            }
//        })
//                .then(new DonePipe<Void, MultipleResults, OneReject, MasterProgress>() {
//                    @Override
//                    public Promise<MultipleResults, OneReject, MasterProgress> pipeDone(Void aVoid) {
//                        List<Promise<HttpResponse, Throwable, Object>> promises = new ArrayList<Promise<HttpResponse, Throwable, Object>>();
//                        for (final String url: urls) {
//                            promises.add(get(url));
//                        }
//                        return deferredManager.when(promises.toArray(new Promise[promises.size()]));
//                    }
//                })
//                .then(new DonePipe<MultipleResults, List<HttpResponse>, Throwable, Object>() {
//                    @Override
//                    public Promise<List<HttpResponse>, Throwable, Object> pipeDone(MultipleResults oneResults) {
//                        List<HttpResponse> responses = new ArrayList<HttpResponse>();
//                        for (OneResult oneResult: oneResults) {
//                            responses.add((HttpResponse) oneResult.getResult());
//                        }
//                        return new DeferredObject<List<HttpResponse>, Throwable, Object>().resolve(responses);
//                    }
//                }, new FailPipe<OneReject, List<HttpResponse>, Throwable, Object>() {
//                    @Override
//                    public Promise<List<HttpResponse>, Throwable, Object> pipeFail(OneReject oneReject) {
//                        return new DeferredObject<List<HttpResponse>, Throwable, Object>().reject((Throwable) oneReject.getReject());
//                    }
//                })
//                .then(new DonePipe<List<HttpResponse>, List<HttpResponse>, Throwable, Object>() {
//                    @Override
//                    public Promise<List<HttpResponse>, Throwable, Object> pipeDone(List<HttpResponse> responses) {
//                        return new DeferredObject<List<HttpResponse>, Throwable, Object>().resolve(responses);
//                    }
//                });
//    }
}