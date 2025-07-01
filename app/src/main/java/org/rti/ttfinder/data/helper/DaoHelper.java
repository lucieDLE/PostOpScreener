package org.rti.ttfinder.data.helper;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import org.rti.ttfinder.data.dao.AssessmentDao;
import org.rti.ttfinder.data.entity.Assessment;

/**
 * Created by theReza on 7/4/2018.
 */

@Database(entities = {Assessment.class}, version = 3,exportSchema = true )
public abstract class DaoHelper extends RoomDatabase {
    public static final String DATABASE_NAME = "tt-screener.db";
    // commands
    public static final int INSERT_ALL = 1, FETCH_ALL = 2, DELETE = 3, EDIT = 5, DELETE_ALL = 6,FETCH_SINGLE = 7;

    public abstract AssessmentDao getAssessmentDao();


}
