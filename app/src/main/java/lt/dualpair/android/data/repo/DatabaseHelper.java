package lt.dualpair.android.data.repo;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import lt.dualpair.android.R;
import lt.dualpair.android.accounts.AccountUtils;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME_PREFIX = "DualPair_";
    private static final int VERSION = 4;

    private Context context;
    private static Map<Long, DatabaseHelper> instances = new HashMap<>();

    private DatabaseHelper(Context context, String userId) {
        super(context, DB_NAME_PREFIX + userId, null, VERSION);
        this.context = context;
    }

    public static DatabaseHelper getInstance(Context context) {
        Long userId = AccountUtils.getUserId(context);
        if (!instances.containsKey(userId)) {
            instances.put(userId, new DatabaseHelper(context.getApplicationContext(), userId.toString()));
        }
        return instances.get(userId);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String schema = context.getResources().getString(R.string.create_db);
        for (String statement : schema.split(";")) {
            db.execSQL(statement);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {        String schema = context.getResources().getString(R.string.create_db);
        for (String statement : schema.split(";")) {
            db.execSQL(statement);
        }
    }

    public static String getDateTimeString(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return dateFormat.format(date);
    }

    public static Date getDateFromString(String dateTimeString) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        try {
            return dateFormat.parse(dateTimeString);
        } catch (ParseException pe) {
            throw new RuntimeException(pe);
        }
    }
}