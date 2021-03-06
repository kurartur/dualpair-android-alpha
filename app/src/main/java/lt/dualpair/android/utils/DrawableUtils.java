package lt.dualpair.android.utils;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;

import lt.dualpair.android.R;
import lt.dualpair.android.ui.accounts.AccountType;

public class DrawableUtils {

    public static Drawable getActionBarIcon(Context context, int drawableId) {
        Drawable drawable = context.getResources().getDrawable(drawableId);
        drawable.setColorFilter(context.getResources().getColor(R.color.actionBarIcons), PorterDuff.Mode.SRC_ATOP);
        return drawable;
    }

    public static void setAccentColorFilter(Context context, Drawable drawable) {
        drawable.setColorFilter(context.getResources().getColor(R.color.colorAccent), PorterDuff.Mode.SRC_ATOP);
    }

    public static void setActionBarIconColorFilter(Context context, Drawable drawable) {
        drawable.mutate();
        drawable.setColorFilter(context.getResources().getColor(R.color.actionBarIcons), PorterDuff.Mode.SRC_ATOP);
    }

    public static int getAccountTypeColor(Context context, AccountType accountType) {
        String colorId = "";
        if (accountType == AccountType.FB) {
            colorId = "facebookColor";
        } else {
            colorId = "vkontakteColor";
        }
        return context.getResources().getIdentifier(colorId, "color", context.getPackageName());
    }
}
