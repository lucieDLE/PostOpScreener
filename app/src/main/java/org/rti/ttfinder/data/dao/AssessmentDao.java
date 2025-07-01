package org.rti.ttfinder.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import org.rti.ttfinder.data.entity.Assessment;

import java.util.List;

@Dao
public interface AssessmentDao {

    @Query("SELECT * FROM Assessment ORDER BY id DESC")
    List<Assessment> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Assessment> products);

    @Query("SELECT COUNT(id) FROM Assessment WHERE tt_tracker_id LIKE :ttId")
    int isTTIDExist(String ttId);

    @Update
    void update(Assessment product);

    @Delete
    void delete(Assessment product);

    @Query("DELETE FROM Assessment")
    void clearAll();

}
