package lt.dualpair.android.ui.main;

import android.Manifest;
import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import lt.dualpair.android.R;
import lt.dualpair.android.data.LocationLiveData;
import lt.dualpair.android.data.LocationSettingsLiveData;
import lt.dualpair.android.data.local.entity.FullUserSociotype;
import lt.dualpair.android.data.local.entity.User;
import lt.dualpair.android.data.local.entity.UserForView;
import lt.dualpair.android.data.local.entity.UserLocation;
import lt.dualpair.android.data.local.entity.UserPhoto;
import lt.dualpair.android.data.local.entity.UserPurposeOfBeing;
import lt.dualpair.android.data.repository.UserPrincipalRepository;
import lt.dualpair.android.data.repository.UserRepository;
import lt.dualpair.android.ui.BaseFragment;
import lt.dualpair.android.ui.CustomActionBarFragment;
import lt.dualpair.android.ui.ErrorConverter;
import lt.dualpair.android.ui.ImageSwipe;
import lt.dualpair.android.ui.Resource;
import lt.dualpair.android.ui.UserFriendlyErrorConsumer;
import lt.dualpair.android.ui.search.SearchParametersActivity;
import lt.dualpair.android.ui.user.UserViewActionBarViewHolder;
import lt.dualpair.android.utils.DrawableUtils;
import lt.dualpair.android.utils.LabelUtils;
import lt.dualpair.android.utils.LocationUtil;

public class ReviewFragment extends BaseFragment implements CustomActionBarFragment {

    private static final String TAG = ReviewFragment.class.getName();

    private static final int SP_REQ_CODE = 1;

    private static final int LOCATION_PERMISSIONS_REQ_CODE = 1;
    private static final int LOCATION_SETTINGS_REQ_CODE = 4;
    private static final int PERMISSION_SETTING_REQ_CODE = 5;

    @Bind(R.id.review) RelativeLayout reviewLayout;
    @Bind(R.id.name) TextView name;
    @Bind(R.id.age) TextView age;
    @Bind(R.id.city) TextView city;
    @Bind(R.id.distance) TextView distance;
    @Bind(R.id.photos) ImageSwipe photosView;
    @Bind(R.id.sociotypes)  TextView sociotypes;
    @Bind(R.id.description) TextView description;
    @Bind(R.id.purposes_of_being) TextView purposesOfBeing;
    @Bind(R.id.relationship_status) TextView relationshipStatus;

    @Bind(R.id.progress_layout) LinearLayout progressLayout;
    @Bind(R.id.progress_bar) ProgressBar progressBar;
    @Bind(R.id.progress_text) TextView progressText;
    @Bind(R.id.retry_button) ImageView retryButton;

    private UserViewActionBarViewHolder actionBarViewHolder;

    private ReviewViewModel viewModel;

    private UserLocation lastPrincipalLocation;
    private UserLocation lastReviewedUserLocation;

    private CompositeDisposable disposable = new CompositeDisposable();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.review_layout, container, false);
        ButterKnife.bind(this, view);

        showLoading();

        ButterKnife.findById(view, R.id.yes_button).setOnClickListener(v -> {
            disposable.add(
                viewModel.respondWithYes()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(viewModel::loadNext, new UserFriendlyErrorConsumer(this))
            );
        });
        ButterKnife.findById(view, R.id.no_button).setOnClickListener(v -> {
            disposable.add(
                viewModel.respondWithNo()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(viewModel::loadNext, new UserFriendlyErrorConsumer(this))
            );
        });

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        viewModel = ViewModelProviders.of(this, new ReviewViewModelFactory(getActivity().getApplication())).get(ReviewViewModel.class);
        subscribeUi();

        doLocationChecks();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        actionBarViewHolder = new UserViewActionBarViewHolder(getActivity().getLayoutInflater().inflate(R.layout.review_action_bar_layout, null), getContext());
    }

    private void doLocationChecks() {
        if (!canAccessLocation()) {
            askForPermissionToAccessLocation();
        } else {
            onLocationAccessGranted();
        }
    }

    private void subscribeUi() {
        viewModel.getUserToReview().observe(this, new Observer<Resource<UserForView>>() {
            @Override
            public void onChanged(@Nullable Resource<UserForView> userForView) {
                if (userForView.isLoading()) {
                    showLoading();
                } else if (userForView.isError()) {
                    showError(new ErrorConverter(ReviewFragment.this).convert(userForView.getError()));
                } else if (userForView.isSuccess()) {
                    UserForView data = userForView.getData();
                    if (data.getUser() == null) {
                        showNoMatches();
                    } else {
                        renderReview(data);
                    }
                }
            }
        });
        viewModel.getLastStoredLocation().observe(this, new Observer<UserLocation>() {
            @Override
            public void onChanged(@Nullable UserLocation userLocation) {
                lastPrincipalLocation = userLocation;
                actionBarViewHolder.setLocation(userLocation, lastReviewedUserLocation);
            }
        });
    }

    private boolean canAccessLocation() {
        return ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void askForPermissionToAccessLocation() {
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSIONS_REQ_CODE);
    }

    @OnClick(R.id.retry_button) void onRetryClick() {
        viewModel.retry();
    }

    @Override
    public View getActionBarView() {
        return actionBarViewHolder.actionBarView;
    }

    public void renderReview(UserForView userForView) {
        User opponentUser = userForView.getUser();
        progressLayout.setVisibility(View.GONE);
        reviewLayout.setVisibility(View.VISIBLE);

        lastReviewedUserLocation = userForView.getLastLocation();
        setUserData(opponentUser);
        setLocation(lastPrincipalLocation, lastReviewedUserLocation);

        setData(
                userForView.getSociotypes(),
                userForView.getUser().getDescription(),
                userForView.getPhotos(),
                userForView.getUser().getRelationshipStatus(),
                userForView.getPurposesOfBeing()
        );
    }

    public void setData(List<FullUserSociotype> userSociotypes,
                        String description,
                        List<UserPhoto> photos,
                        lt.dualpair.android.data.local.entity.RelationshipStatus relationshipStatus,
                        List<UserPurposeOfBeing> purposesOfBeing) {
        StringBuilder sb = new StringBuilder();
        String prefix = "";
        for (FullUserSociotype sociotype : userSociotypes) {
            sb.append(prefix);
            prefix = ", ";
            String code = sociotype.getSociotype().getCode1();
            int titleId = getResources().getIdentifier(code.toLowerCase() + "_title", "string", getContext().getPackageName());
            sb.append(getContext().getString(titleId) + " (" + sociotype.getSociotype().getCode1() + ")");
        }
        sociotypes.setText(sb);
        this.description.setText(description);
        photosView.setPhotos(photos);
        setRelationshipStatus(relationshipStatus);
        setPurposesOfBeing(purposesOfBeing);
    }

    public void setUserData(User user) {
        name.setText(user.getName());
        age.setText(getString(R.string.review_age, user.getAge()));
    }

    public void setLocation(UserLocation principalLocation, UserLocation opponentLocation) {
        if (opponentLocation != null) {
            city.setText(getString(R.string.review_city, opponentLocation.getCity()));
        }
        if (principalLocation != null && opponentLocation != null) {
            Double distance = LocationUtil.calculateDistance(
                    principalLocation.getLatitude(),
                    principalLocation.getLongitude(),
                    opponentLocation.getLatitude(),
                    opponentLocation.getLongitude()
            );
            this.distance.setText(getString(R.string.review_distance, distance.intValue() / 1000));
        } else {
            this.distance.setText("");
        }
    }

    private void setPurposesOfBeing(List<UserPurposeOfBeing> purposesOfBeing) {
        this.purposesOfBeing.setVisibility(View.GONE);
        if (!purposesOfBeing.isEmpty()) {
            this.purposesOfBeing.setText(getResources().getString(R.string.i_am_here_to, getPurposesText(purposesOfBeing)));
            this.purposesOfBeing.setVisibility(View.VISIBLE);
        }
    }

    private void setRelationshipStatus(lt.dualpair.android.data.local.entity.RelationshipStatus relationshipStatus) {
        this.relationshipStatus.setVisibility(View.GONE);
        if (relationshipStatus != lt.dualpair.android.data.local.entity.RelationshipStatus.NONE) {
            this.relationshipStatus.setText(LabelUtils.getRelationshipStatusLabel(getContext(), relationshipStatus));
            this.relationshipStatus.setVisibility(View.VISIBLE);
        }
    }

    private String getPurposesText(List<UserPurposeOfBeing> purposesOfBeing) {
        String text = "";
        String prefix = "";
        for (UserPurposeOfBeing purposeOfBeing : purposesOfBeing) {
            text += prefix + LabelUtils.getPurposeOfBeingLabel(getContext(), purposeOfBeing.getPurpose());
            prefix = ", ";
        }
        return text.toLowerCase();
    }

    public void showLoading() {
        progressText.setText("");
        retryButton.setVisibility(View.GONE);
        progressLayout.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);
        reviewLayout.setVisibility(View.GONE);
        progressLayout.setVisibility(View.VISIBLE);
    }

    public void showNoMatches() {
        progressLayout.setVisibility(View.VISIBLE);
        progressText.setText(getResources().getString(R.string.no_matches_found));
        progressBar.setVisibility(View.GONE);
        retryButton.setVisibility(View.VISIBLE);
    }

    public void showError(String error) {
        progressLayout.setVisibility(View.VISIBLE);
        progressText.setText(error);
        progressBar.setVisibility(View.GONE);
        retryButton.setVisibility(View.VISIBLE);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.review_fragment_menu, menu);
        for(int i=0 ; i < menu.size(); i++) {
            MenuItem menuItem = menu.getItem(i);
            DrawableUtils.setActionBarIconColorFilter(getActivity(), menuItem.getIcon());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.search_parameters_menu_item:
                startActivityForResult(SearchParametersActivity.createIntent(getActivity(), true), SP_REQ_CODE);
                break;
            case R.id.history_menu_item:
                startActivity(ReviewHistoryActivity.createIntent(getActivity()));
                break;
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case LOCATION_SETTINGS_REQ_CODE:
                onLocationSettingsOk();
                break;
            case PERMISSION_SETTING_REQ_CODE:
                doLocationChecks();
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case LOCATION_PERMISSIONS_REQ_CODE:
                for (int i = 0, len = permissions.length; i < len; i++) {
                    String permission = permissions[i];
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), permission);
                        if (!showRationale) {
                            showLocationAccessDeniedNotification();
                        } else {
                            showLocationAccessExplanation();
                        }
                    } else {
                        onLocationAccessGranted();
                    }
                }
                break;
        }
    }

    private void onLocationAccessGranted() {
        checkLocationSettings();
    }

    private void showLocationAccessExplanation() {
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(getContext(), android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(getContext());
        }
        builder.setTitle(R.string.location_permissions_explanation_title)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage(R.string.location_permission_explanation_message)
                .setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        askForPermissionToAccessLocation();
                    }
                })
                .setNegativeButton(R.string.decline_anyway, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        getActivity().finishAffinity();
                    }
                })
                .show();
    }

    private void showLocationAccessDeniedNotification() {
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(getContext(), android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(getContext());
        }
        builder.setTitle(R.string.location_permissions_explanation_title)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage(R.string.location_permission_denied_explanation_message)
                .setPositiveButton(R.string.open_app_settings,  new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getActivity().getPackageName(), null);
                        intent.setData(uri);
                        startActivityForResult(intent, PERMISSION_SETTING_REQ_CODE);
                    }
                })
                .setNegativeButton(R.string.close_app, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getActivity().finishAffinity();
                    }
                })
                .show();
    }

    public void checkLocationSettings() {
        viewModel.getLocationSettings().observe(this, new Observer<LocationSettingsResult>() {
            @Override
            public void onChanged(@Nullable LocationSettingsResult result) {
                final Status status = result.getStatus();
                final LocationSettingsStates states = result.getLocationSettingsStates();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can
                        // initialize location requests here.
                        onLocationSettingsOk();
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied, but this can be fixed
                        // by showing the user a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            startIntentSenderForResult(status.getResolution().getIntentSender(), LOCATION_SETTINGS_REQ_CODE, null, 0, 0, 0, null);
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way
                        // to fix the settings so we won't show the dialog.
                        break;
                }
                viewModel.getLocationSettings().removeObserver(this);
            }
        });
    }

    private void onLocationSettingsOk() {
        viewModel.loadNext();
    }

    @Override
    public String getActionBarTitle() {
        return null;
    }

    private static LocationRequest createLocationRequest() {
        return LocationUtil.createLocationRequest();
    }

    public static ReviewFragment newInstance() {
        return new ReviewFragment();
    }

    private static class ReviewViewModelFactory extends ViewModelProvider.AndroidViewModelFactory {

        private Application application;

        public ReviewViewModelFactory(@NonNull Application application) {
            super(application);
            this.application = application;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass.isAssignableFrom(ReviewViewModel.class)) {
                LocationSettingsLiveData locationSettingsLiveData = new LocationSettingsLiveData(application, createLocationRequest());
                UserPrincipalRepository userPrincipalRepository = new UserPrincipalRepository(application);
                UserRepository userRepository = new UserRepository(application);
                LiveData<android.location.Location> location = new LocationLiveData(application);
                return (T) new ReviewViewModel(userPrincipalRepository, userRepository, locationSettingsLiveData, location);
            }
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }

}
