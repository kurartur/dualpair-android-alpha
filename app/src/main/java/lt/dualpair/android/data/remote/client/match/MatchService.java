package lt.dualpair.android.data.remote.client.match;

import io.reactivex.Completable;
import io.reactivex.Observable;
import lt.dualpair.android.data.remote.resource.Match;
import lt.dualpair.android.data.remote.resource.ResourceCollection;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface MatchService {

    @GET("/api/user/{userId}/matches?mt=mu&page=0&size=1000")
    Observable<ResourceCollection<Match>> getUserMutualMatches(@Path("userId") Long userId, @Query("timestamp") Long timestamp);

    @GET("/api/user/{userId}/matches/{matchId}")
    Observable<Match> getUserMatch(@Path("userId") Long userId, @Path("matchId") Long matchId);

    @DELETE("/api/user/{userId}/matches/{matchId}")
    Completable unmatch(@Path("userId") Long userId, @Path("matchId") Long matchId);
}
