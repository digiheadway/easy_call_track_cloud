package tera.videodownloader.box.Utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.Cursor;
import android.content.ContentValues;

import com.jeffmony.downloader.model.VideoTaskItem;

import java.util.ArrayList;

public class VideoTaskDBHelper extends android.database.sqlite.SQLiteOpenHelper {

    private static final String DATABASE_NAME = "video_tasks.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_NAME = "video_tasks";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_URL = "url";
    public static final String COLUMN_COVER_URL = "cover_url";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_GROUP_NAME = "group_name";

    public VideoTaskDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_URL + " TEXT,"
                + COLUMN_COVER_URL + " TEXT,"
                + COLUMN_TITLE + " TEXT,"
                + COLUMN_GROUP_NAME + " TEXT" + ")";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public void addVideoTaskItem(VideoTaskItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_URL, item.getUrl());
        values.put(COLUMN_COVER_URL, item.getCoverUrl());
        values.put(COLUMN_TITLE, item.getTitle());
        values.put(COLUMN_GROUP_NAME, item.getGroupName());

        db.insert(TABLE_NAME, null, values);
        db.close();
    }


    @SuppressLint("Range")
    public ArrayList<VideoTaskItem> getAllVideoTasks() {
        ArrayList<VideoTaskItem> taskList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, null, null, null, null, null, null);

        if (cursor != null) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                @SuppressLint("Range") VideoTaskItem item = new VideoTaskItem(
                        cursor.getString(cursor.getColumnIndex(COLUMN_URL)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_COVER_URL)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_TITLE)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_GROUP_NAME))
                );
                taskList.add(item);
                cursor.moveToNext();
            }
            cursor.close();
        }
        db.close();
        return taskList;
    }

    public boolean isVideoAlreadyAdded(String groupName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, null, COLUMN_GROUP_NAME + " = ?", new String[]{groupName}, null, null, null);

        boolean exists = cursor.getCount() > 0;  // If count > 0, the video exists

        cursor.close();
        db.close();

        return exists;
    }

}
