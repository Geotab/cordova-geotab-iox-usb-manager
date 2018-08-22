package android.geotab.ioxusbmanager;

import android.view.Gravity;
import android.widget.Toast;
import android.content.Context;
import android.app.Activity;
import android.util.Log;

public class ShowToastUtil {
    private static Activity activity;
    private static Context ctx;

    public ShowToastUtil(Context context, Activity act) {
        activity = act;
        ctx = context;
    }
    public static void showToastFromThread(String TAG, final String sToast) {
        Log.i(TAG, sToast);

        ShowToastUtil.activity.runOnUiThread(new Runnable() {
            public void run() {
                Toast DisplayMessage = Toast.makeText(ShowToastUtil.ctx, sToast, Toast.LENGTH_LONG);
                DisplayMessage.setGravity(Gravity.CENTER_VERTICAL | Gravity.BOTTOM, 0, 0);
                DisplayMessage.show();
            }
        });
    }
}