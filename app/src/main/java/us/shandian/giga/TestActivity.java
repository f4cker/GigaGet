package us.shandian.giga;

import android.app.*;
import android.widget.TextView;
import android.os.*;

import java.util.concurrent.TimeUnit;

import us.shandian.giga.get.DownloadManager;
import us.shandian.giga.get.DownloadMission;
import us.shandian.giga.util.Utility;

public class TestActivity extends Activity implements DownloadMission.MissionListener
{
	private DownloadManager mManager;
	private TextView mText;
	private long mLastTimeStamp;
	private long mLastDone = 0;
	
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test);
		mText = (TextView) findViewById(R.id.hello);
		mLastTimeStamp = System.currentTimeMillis();
		
		mManager = new DownloadManager(this, "/storage/sdcard0/GigaGet");
		
		int id = 0;
		
		if (mManager.getCount() == 0) {
			id = mManager.startMission("http://dldir1.qq.com/qqfile/QQIntl/QQi_wireless/Android/qqi_5.0.10.6046_android_office.apk", "QQ.apk");
		} else {
			mManager.resumeMission(id);
		}
		
		mManager.getMission(id).addListener(this);
		
    }

	@Override
	public void onProgressUpdate(long done, long total) {
		long nowTimeStamp = System.currentTimeMillis();
		long delta = nowTimeStamp - mLastTimeStamp;
		
		// Prevent from too fast refresh rate
		if (delta < 1000) return;
		
		float speed = (float) (done - mLastDone) / delta * 1000;
		mText.setText(String.format("%.2f%% speed:%s %s of %s", (float) done / total * 100, Utility.formatSpeed(speed), Utility.formatBytes(done), Utility.formatBytes(total)));
		mLastDone = done;
		mLastTimeStamp = nowTimeStamp;
	}

	@Override
	public void onFinish() {
		mText.setText("done");
	}
}
