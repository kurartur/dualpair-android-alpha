package lt.dualpair.android.ui.user;


import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import lt.dualpair.android.R;
import lt.dualpair.android.data.local.entity.FullUserSociotype;
import lt.dualpair.android.data.local.entity.UserPhoto;
import lt.dualpair.android.data.local.entity.UserPurposeOfBeing;
import lt.dualpair.android.ui.ImageSwipe;
import lt.dualpair.android.utils.LabelUtils;

public class OpponentUserView extends LinearLayout {

    @Bind(R.id.photos)      ImageSwipe photosView;
    @Bind(R.id.sociotypes)  TextView sociotypes;
    @Bind(R.id.description) TextView description;
    @Bind(R.id.photos_wrapper) RelativeLayout photosWrapper;
    @Bind(R.id.purposes_of_being) TextView purposesOfBeing;
    @Bind(R.id.relationship_status) TextView relationshipStatus;

    public OpponentUserView(Context context) {
        super(context);
        initView(context);
    }

    public OpponentUserView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public OpponentUserView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    public OpponentUserView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView(context);
    }

    protected void initView(Context context) {
        LayoutInflater  mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mInflater.inflate(R.layout.opponent_user_view, this, true);
        ButterKnife.bind(this);
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

    private void setPurposesOfBeing(List<UserPurposeOfBeing> purposesOfBeing) {
        this.purposesOfBeing.setVisibility(GONE);
        if (!purposesOfBeing.isEmpty()) {
            this.purposesOfBeing.setText(getResources().getString(R.string.i_am_here_to, getPurposesText(purposesOfBeing)));
            this.purposesOfBeing.setVisibility(VISIBLE);
        }
    }

    private void setRelationshipStatus(lt.dualpair.android.data.local.entity.RelationshipStatus relationshipStatus) {
        this.relationshipStatus.setVisibility(GONE);
        if (relationshipStatus != lt.dualpair.android.data.local.entity.RelationshipStatus.NONE) {
            this.relationshipStatus.setText(LabelUtils.getRelationshipStatusLabel(getContext(), relationshipStatus));
            this.relationshipStatus.setVisibility(VISIBLE);
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

    public View setPhotoOverlay(int layoutId) {
        return LayoutInflater.from(getContext()).inflate(layoutId, photosWrapper, true);
    }

}