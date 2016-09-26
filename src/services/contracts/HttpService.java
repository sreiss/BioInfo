package services.contracts;

import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpResponse;
import org.jdeferred.Promise;

import java.util.List;

public interface HttpService {
    Promise<HttpResponse, Throwable, Object> get(final String url);
    Promise<HttpResponse, Throwable, Object> post(final String url, HttpContent content);
    Promise<List<HttpResponse>, Throwable, Object> get(final List<String> urls);
}
