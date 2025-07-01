package org.rti.ttfinder.data.loader;

import android.content.Context;
import android.os.AsyncTask;

import org.rti.ttfinder.data.entity.Assessment;
import org.rti.ttfinder.data.helper.AppDB;
import org.rti.ttfinder.data.helper.DaoHelper;
import org.rti.ttfinder.data.helper.DbLoaderInterface;

import java.lang.ref.WeakReference;
import java.util.List;

public class AssessmentLoader  extends AsyncTask<Object, Void, Object> {
    private DbLoaderInterface dbLoaderInterface;
    private WeakReference<Context> weakContext;

    public AssessmentLoader(Context context) {
        weakContext = new WeakReference<Context>(context);
    }

    public void setDbLoaderInterface(DbLoaderInterface dbLoaderInterface) {
        this.dbLoaderInterface = dbLoaderInterface;
    }

    @Override
    protected Object doInBackground(Object... object) {
        Context context = weakContext.get();
        int command = (int) object[0];

        if (command == DaoHelper.INSERT_ALL) {
            List<Assessment> models = (List<Assessment>) object[1];
            AppDB.getAppDb(context).getAssessmentDao().insertAll(models);
        } else if (command == DaoHelper.FETCH_ALL) {
            return AppDB.getAppDb(context).getAssessmentDao().getAll();
        }
        else if (command == DaoHelper.DELETE) {
            Assessment model = (Assessment) object[1];
            AppDB.getAppDb(context).getAssessmentDao().delete(model);
        }
        else if (command == DaoHelper.EDIT) {
            Assessment model = (Assessment) object[1];
            AppDB.getAppDb(context).getAssessmentDao().update(model);
        }
        else if (command == DaoHelper.FETCH_SINGLE) {
            String data = (String) object[1];
            return AppDB.getAppDb(context).getAssessmentDao().isTTIDExist(data);
        }
        else if (command == DaoHelper.DELETE_ALL) {
            AppDB.getAppDb(context).getAssessmentDao().clearAll();
        }

        return null;
    }

    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);

        if(dbLoaderInterface != null) {
            dbLoaderInterface.onFinished(o);
        }
    }

}
