package services.contracts;

import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpResponse;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.concurrent.Future;

public interface HttpService {
    ListenableFuture<HttpResponse> get(final String url);
}
