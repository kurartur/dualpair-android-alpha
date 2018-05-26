package lt.dualpair.android.data.repository;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import lt.dualpair.android.accounts.AccountUtils;
import lt.dualpair.android.data.local.DualPairRoomDatabase;
import lt.dualpair.android.data.local.dao.SociotypeDao;
import lt.dualpair.android.data.local.dao.UserDao;
import lt.dualpair.android.data.local.entity.FullUserSociotype;
import lt.dualpair.android.data.local.entity.PurposeOfBeing;
import lt.dualpair.android.data.local.entity.RelationshipStatus;
import lt.dualpair.android.data.local.entity.Sociotype;
import lt.dualpair.android.data.local.entity.User;
import lt.dualpair.android.data.local.entity.UserAccount;
import lt.dualpair.android.data.local.entity.UserLocation;
import lt.dualpair.android.data.local.entity.UserPhoto;
import lt.dualpair.android.data.local.entity.UserPurposeOfBeing;
import lt.dualpair.android.data.local.entity.UserSearchParameters;
import lt.dualpair.android.data.local.entity.UserSociotype;
import lt.dualpair.android.data.mapper.UserResourceMapper;
import lt.dualpair.android.data.remote.client.authentication.LogoutClient;
import lt.dualpair.android.data.remote.client.user.GetSearchParametersClient;
import lt.dualpair.android.data.remote.client.user.GetUserPrincipalClient;
import lt.dualpair.android.data.remote.client.user.SetLocationClient;
import lt.dualpair.android.data.remote.client.user.SetPhotosClient;
import lt.dualpair.android.data.remote.client.user.SetSearchParametersClient;
import lt.dualpair.android.data.remote.client.user.SetUserSociotypesClient;
import lt.dualpair.android.data.remote.client.user.UpdateUserClient;
import lt.dualpair.android.data.resource.Location;
import lt.dualpair.android.data.resource.Photo;
import lt.dualpair.android.data.resource.SearchParameters;
import lt.dualpair.android.ui.accounts.AccountType;

public class UserPrincipalRepository {

    private static final String TAG = UserPrincipalRepository.class.getName();

    private UserDao userDao;
    private SociotypeDao sociotypeDao;
    private Long userId;
    private DualPairRoomDatabase database;

    public UserPrincipalRepository(Application application) {
        database = DualPairRoomDatabase.getDatabase(application);
        userDao = database.userDao();
        sociotypeDao = database.sociotypeDao();
        userId = AccountUtils.getUserId(application);
    }

    public Single<User> getUser() {
        Maybe<User> localUser = userDao.getUserMaybe(userId);
        Single<User> remoteUser = new GetUserPrincipalClient().observable()
                .subscribeOn(Schedulers.io())
                .map(new Function<lt.dualpair.android.data.resource.User, User>() {
                    @Override
                    public User apply(lt.dualpair.android.data.resource.User userResource) {
                        UserResourceMapper.Result mappingResult = saveUserResource(userResource);
                        return mappingResult.getUser();
                    }
                }).singleOrError();
        return Maybe.concat(localUser, remoteUser.toMaybe()).firstElement().toSingle();
    }

    public Single<List<UserSociotype>> getSociotypes() {
        Maybe<List<UserSociotype>> local = userDao.getUserSociotypesMaybe(userId)
                .filter(list -> !list.isEmpty())
                .doOnSuccess(new Consumer<List<UserSociotype>>() {
            @Override
            public void accept(List<UserSociotype> sociotypes) {
                Log.d(TAG, sociotypes.size() + "");
            }
        });
        Single<List<UserSociotype>> remote = new GetUserPrincipalClient().observable()
                .subscribeOn(Schedulers.io())
                .map(new Function<lt.dualpair.android.data.resource.User, List<UserSociotype>>() {
                    @Override
                    public List<UserSociotype> apply(lt.dualpair.android.data.resource.User userResource) {
                        UserResourceMapper.Result mappingResult = saveUserResource(userResource);
                        return mappingResult.getUserSociotypes();
                    }
                }).singleOrError();
        return Maybe.concat(local, remote.toMaybe()).firstElement().toSingle();
    }

    public Single<List<FullUserSociotype>> getFullUserSociotypes() {
        return getSociotypes()
                .map(new Function<List<UserSociotype>, List<FullUserSociotype>>() {
                    @Override
                    public List<FullUserSociotype> apply(List<UserSociotype> sociotypes) {
                        return loadSociotypes(sociotypes);
                    }
                });
    }

    private List<FullUserSociotype> loadSociotypes(List<UserSociotype> userSociotypes) {
        List<FullUserSociotype> fullSociotypes = new ArrayList<>();
        for (UserSociotype userSociotype : userSociotypes) {
            fullSociotypes.add(new FullUserSociotype(userSociotype, sociotypeDao.getSociotypeById(userSociotype.getSociotypeId())));
        }
        return fullSociotypes;
    }


    private UserResourceMapper.Result saveUserResource(lt.dualpair.android.data.resource.User userResource) {
        UserResourceMapper.Result mappingResult = new UserResourceMapper(sociotypeDao).map(userResource);
        database.runInTransaction(() -> {
            userDao.saveUser(mappingResult.getUser());
            userDao.saveUserAccounts(mappingResult.getUserAccounts());
            userDao.saveUserPhotos(mappingResult.getUserPhotos());
            userDao.saveUserSociotypes(mappingResult.getUserSociotypes());
        });
        return mappingResult;
    }

    public Completable setSociotype(String sociotypeCode) {
        Set<String> sociotypes = new HashSet<>();
        sociotypes.add(sociotypeCode);
        return new SetUserSociotypesClient(userId, sociotypes).completable()
                .doOnComplete(() -> {
                    Sociotype sociotype = sociotypeDao.getSociotype(sociotypeCode);
                    UserSociotype userSociotype = new UserSociotype();
                    userSociotype.setUserId(userId);
                    userSociotype.setSociotypeId(sociotype.getId());
                    userDao.saveUserSociotype(userSociotype);
                    userDao.getUserSociotypes(userId);
                });
    }

    public Completable saveLocation(android.location.Location location) {
        Location locationResource = new Location();
        locationResource.setLatitude(location.getLatitude());
        locationResource.setLongitude(location.getLongitude());
        return new SetLocationClient(userId, locationResource).completable()
                .doOnComplete(new Action() {
                    @Override
                    public void run() {
                        userDao.saveUserLocation(UserLocation.fromAndroidLocation(location, userId));
                    }
                });
    }

    public Single<UserSearchParameters> getSearchParameters() {
        Maybe<UserSearchParameters> local = userDao.getSearchParametersMaybe(userId);
        Single<UserSearchParameters> remote = new GetSearchParametersClient(userId).observable()
                .map(new Function<SearchParameters, UserSearchParameters>() {
                    @Override
                    public UserSearchParameters apply(SearchParameters searchParametersResource) {
                        UserSearchParameters userSearchParameters = new UserSearchParameters();
                        userSearchParameters.setUserId(userId);
                        userSearchParameters.setSearchFemale(searchParametersResource.getSearchFemale());
                        userSearchParameters.setSearchMale(searchParametersResource.getSearchMale());
                        userSearchParameters.setMinAge(searchParametersResource.getMinAge());
                        userSearchParameters.setMaxAge(searchParametersResource.getMaxAge());
                        userDao.saveUserSearchParameters(userSearchParameters);
                        return userSearchParameters;
                    }
                }).singleOrError();
        return Maybe.concat(local, remote.toMaybe()).firstElement().toSingle();
    }

    public Completable logout() {
        return new LogoutClient().completable();
    }

    public Completable setSearchParameters(UserSearchParameters sp) {
        SearchParameters spResource = new SearchParameters();
        spResource.setMinAge(sp.getMinAge());
        spResource.setMaxAge(sp.getMaxAge());
        spResource.setSearchFemale(sp.getSearchFemale());
        spResource.setSearchMale(sp.getSearchMale());
        return new SetSearchParametersClient(userId, spResource).completable()
                .doOnComplete(new Action() {
                    @Override
                    public void run() {
                        sp.setUserId(userId);
                        userDao.saveUserSearchParameters(sp);
                    }
                });
    }

    public Completable updateUser(String name, Date dateOfBirth, String description, RelationshipStatus relationshipStatus, List<PurposeOfBeing> purposesOfBeing) {
        lt.dualpair.android.data.resource.User userResource = new lt.dualpair.android.data.resource.User();
        userResource.setId(userId);
        userResource.setName(name);
        userResource.setDateOfBirth(dateOfBirth);
        userResource.setDescription(description);
        userResource.setRelationshipStatus(relationshipStatus.getCode());
        userResource.setPurposesOfBeing(extractCodes(purposesOfBeing));
        return new UpdateUserClient(userResource).completable();
    }

    private Set<String> extractCodes(List<PurposeOfBeing> purposesOfBeing) {
        Set<String> codes = new HashSet<>();
        for (PurposeOfBeing purposeOfBeing : purposesOfBeing) {
            codes.add(purposeOfBeing.getCode());
        }
        return codes;
    }

    public Single<List<UserPhoto>> getPhotos() {
        return new GetUserPrincipalClient().observable()
                .subscribeOn(Schedulers.io())
                .map(new Function<lt.dualpair.android.data.resource.User, List<UserPhoto>>() {
                    @Override
                    public List<UserPhoto> apply(lt.dualpair.android.data.resource.User userResource) {
                        UserResourceMapper.Result mappingResult = saveUserResource(userResource);
                        return mappingResult.getUserPhotos();
                    }
                }).singleOrError();
    }

    public Completable savePhotos(List<UserPhoto> photos) {
        List<Photo> photoResources = new ArrayList<>();
        for (UserPhoto userPhoto : photos) {
            Photo photoResource = new Photo();
            photoResource.setAccountType(AccountType.valueOf(userPhoto.getAccountType()));
            photoResource.setIdOnAccount(userPhoto.getIdOnAccount());
            photoResource.setPosition(userPhoto.getPosition());
            photoResource.setSourceUrl(userPhoto.getSourceLink());
            photoResources.add(photoResource);
        }
        return new SetPhotosClient(userId, photoResources).completable();
    }

    public Single<List<UserAccount>> getUserAccounts() {
        return new GetUserPrincipalClient().observable()
                .subscribeOn(Schedulers.io())
                .map(new Function<lt.dualpair.android.data.resource.User, List<UserAccount>>() {
                    @Override
                    public List<UserAccount> apply(lt.dualpair.android.data.resource.User userResource) {
                        UserResourceMapper.Result mappingResult = saveUserResource(userResource);
                        return mappingResult.getUserAccounts();
                    }
                }).singleOrError();
    }

    public Single<List<UserPurposeOfBeing>> getUserPurposesOfBeing() {
        return new GetUserPrincipalClient().observable()
                .subscribeOn(Schedulers.io())
                .map(new Function<lt.dualpair.android.data.resource.User, List<UserPurposeOfBeing>>() {
                    @Override
                    public List<UserPurposeOfBeing> apply(lt.dualpair.android.data.resource.User userResource) {
                        UserResourceMapper.Result mappingResult = saveUserResource(userResource);
                        return mappingResult.getUserPurposesOfBeing();
                    }
                }).singleOrError();
    }

    public LiveData<UserLocation> getLastStoredLocation() {
        return userDao.getLastLocation(userId);
    }
}