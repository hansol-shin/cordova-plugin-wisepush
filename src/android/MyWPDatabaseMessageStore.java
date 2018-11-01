package kr.co.itsm.plugin;

/**
 * Created by rjxjr on 2018-03-13.
 */

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.wp.android.service.WPMessage;

/**
 * Created by wp on 17. 7. 26.
 */

public class MyWPDatabaseMessageStore extends SQLiteOpenHelper {

  private static MyWPDatabaseMessageStore INSTANCE;

  private static SQLiteDatabase mDb;

  private static final String DATABASE_NAME = "WPAndroidService.db";

  private static final int DATABASE_VERSION = 1;

  private static final String TAG = "";

  public static MyWPDatabaseMessageStore getInstance(Context context) {
    if (INSTANCE == null) {
      INSTANCE = new MyWPDatabaseMessageStore(context.getApplicationContext());
      mDb = INSTANCE.getWritableDatabase();
    }

    return  INSTANCE;
  }

  private MyWPDatabaseMessageStore(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  public void open() {
    if (!mDb.isOpen()) {
      INSTANCE.onOpen(mDb);
    }
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL("CREATE TABLE WPArrivedMessageTable(ID INTEGER PRIMARY KEY);");
    Log.d(TAG, "created the table");
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    db.execSQL("DROP TABLE IF EXISTS WPArrivedMessageTable");
    onCreate(db);
  }

  /**
   *
   * @param msg
   */
  void insert(WPMessage msg) {

    if (msg == null || msg.getId() == null)
      throw new IllegalArgumentException("Invalid WPMessage:" + msg.toString());

    open();
    mDb.execSQL("INSERT INTO WPArrivedMessageTable VALUES(" + msg.getId() + ");");
  }

  /**
   *
   * @param id
   * @return
   */
  int count(Long id) {
    int count = 0;
    open();
    final Cursor cursor = mDb.rawQuery("SELECT * FROM WPArrivedMessageTable WHERE ID=" + id, null);
    try {
      count = cursor.getCount();
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }

    return count;
  }
}
