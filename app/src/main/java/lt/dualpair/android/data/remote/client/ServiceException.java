package lt.dualpair.android.data.remote.client;

import java.io.IOException;
import java.lang.annotation.Annotation;

import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.HttpException;
import retrofit2.Response;
import retrofit2.Retrofit;

public class ServiceException extends RuntimeException {

    public static ServiceException httpError(String url, Response response, Retrofit retrofit) {
        String message = response.code() + " " + response.message();
        return new ServiceException(message, url, response, Kind.HTTP, null, retrofit);
    }

    public static ServiceException networkError(IOException exception) {
        return new ServiceException(exception.getMessage(), null, null, Kind.NETWORK, exception, null);
    }

    public static ServiceException unexpectedError(Throwable exception) {
        return new ServiceException(exception.getMessage(), null, null, Kind.UNEXPECTED, exception, null);
    }

    /** Identifies the event kind which triggered a {@link ServiceException}. */
    public enum Kind {
        /** An {@link IOException} occurred while communicating to the server. */
        NETWORK,
        /** A non-200 HTTP status code was received from the server. */
        HTTP,
        /**
         * An internal error occurred while attempting to execute a request. It is best practice to
         * re-throw this exception so your application crashes.
         */
        UNEXPECTED
    }

    private final String url;
    private final Response response;
    private final Kind kind;
    private final Retrofit retrofit;

    ServiceException(String message, String url, Response response, Kind kind, Throwable exception, Retrofit retrofit) {
        super(message, exception);
        this.url = url;
        this.response = response;
        this.kind = kind;
        this.retrofit = retrofit;
    }

    /** The request URL which produced the error. */
    public String getUrl() {
        return url;
    }

    /** Response object containing status code, headers, body, etc. */
    public Response getResponse() {
        return response;
    }

    /** The event kind which triggered this error. */
    public Kind getKind() {
        return kind;
    }

    /** The Retrofit this request was executed on */
    public Retrofit getRetrofit() {
        return retrofit;
    }

    /**
     * HTTP response body converted to specified {@code type}. {@code null} if there is no
     * response.
     *
     * @throws IOException if unable to convert the body to the specified {@code type}.
     */
    public <T> T getErrorBodyAs(Class<T> type) throws IOException {
        if (response == null || response.errorBody() == null) {
            return null;
        }
        Converter<ResponseBody, T> converter = retrofit.responseBodyConverter(type, new Annotation[0]);
        return converter.convert(response.errorBody());
    }

    public boolean isUnauthorized() {
        return getResponse() != null && getResponse().code() == 401;
    }

    public static ServiceException fromThrowable(Throwable throwable, Retrofit retrofit) {
        // We had non-200 http error
        if (throwable instanceof HttpException) {
            HttpException httpException = (HttpException) throwable;
            Response response = httpException.response();
            return ServiceException.httpError(response.raw().request().url().toString(), response, retrofit);
        }
        // A network error happened
        if (throwable instanceof IOException) {
            return ServiceException.networkError((IOException) throwable);
        }

        // We don't know what happened. We need to simply convert to an unknown error
        return ServiceException.unexpectedError(throwable);
    }
}
