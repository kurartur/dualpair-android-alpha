package lt.dualpair.android.data.remote.client.socionics;

import java.util.Map;

import io.reactivex.Observable;
import lt.dualpair.android.data.remote.client.ObservableClient;
import lt.dualpair.android.data.remote.resource.Sociotype;
import retrofit2.Retrofit;

public class EvaluateTestClient extends ObservableClient<Sociotype> {

    private Map<String, String> choices;

    public EvaluateTestClient(Map<String, String> choices) {
        this.choices = choices;
    }

    @Override
    protected Observable<Sociotype> getApiObserable(Retrofit retrofit) {
        return retrofit.create(SocionicsService.class).evaluateTest(choices);
    }
}
