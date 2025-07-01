package org.rti.ttfinder.data.helper;

import android.content.Context;

import androidx.room.Room;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Created by theReza on 7/4/2018.
 */

public class AppDB {

    private static DaoHelper daoHelper;

    public static DaoHelper getAppDb(Context context) {
        if(daoHelper == null) {
            daoHelper = Room.databaseBuilder(context, DaoHelper.class, DaoHelper.DATABASE_NAME)
                    .addMigrations(MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return daoHelper;
    }

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE Assessment ADD COLUMN right_image_log_file_name TEXT");
            database.execSQL("ALTER TABLE Assessment ADD COLUMN left_image_log_file_name TEXT");
        }
    };

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE Assessment ADD COLUMN have_tt_on_right_eye TEXT");
            database.execSQL("ALTER TABLE Assessment ADD COLUMN have_tt_on_left_eye TEXT");
            database.execSQL("ALTER TABLE Assessment ADD COLUMN gps_coordinate TEXT");
        }
    };

    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE Assessment ADD COLUMN have_tt_on_right_eye_confirm TEXT");
            database.execSQL("ALTER TABLE Assessment ADD COLUMN have_tt_on_left_eye_confirm TEXT");
        }
    };
}
