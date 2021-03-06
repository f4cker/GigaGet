package us.shandian.giga.get;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import us.shandian.giga.util.Utility;
import static us.shandian.giga.BuildConfig.DEBUG;

public class DownloadManager
{
	private static final String TAG = DownloadManager.class.getSimpleName();
	
	public static final int BLOCK_SIZE = 512 * 1024;
	
	private Context mContext;
	private String mLocation;
	private ArrayList<DownloadMission> mMissions = new ArrayList<DownloadMission>();
	
	public DownloadManager(Context context, String location) {
		mContext = context;
		mLocation = location;
		loadMissions();
	}
	
	public int startMission(String url, String name) {
		DownloadMission mission = new DownloadMission();
		mission.url = url;
		mission.name = name;
		mission.location = mLocation;
		mission.timestamp = System.currentTimeMillis();
		new Initializer(mContext, mission).start();
		return insertMission(mission);
	}
	
	public void resumeMission(int i) {
		DownloadMission d = getMission(i);
		if (!d.running && d.errCode == -1) {
			d.start(mContext);
		}
	}
	
	public void pauseMission(int i) {
		DownloadMission d = getMission(i);
		if (d.running) {
			d.pause();
		}
	}
	
	public void deleteMission(int i) {
		mMissions.remove(i);
	}
	
	private void loadMissions() {
		File f = new File(mLocation);
		
		if (f.exists() && f.isDirectory()) {
			File[] subs = f.listFiles();
			
			for (File sub : subs) {
				if (sub.isDirectory()) {
					continue;
				}
				
				if (sub.getName().endsWith(".giga")) {
					String str = Utility.readFromFile(sub.getAbsolutePath());
					if (str != null) {
						
						if (DEBUG) {
							Log.d(TAG, "loading mission " + sub.getName());
							Log.d(TAG, str);
						}
						
						DownloadMission mis = new Gson().fromJson(str, DownloadMission.class);
						
						if (mis.finished) {
							sub.delete();
							continue;
						}
						
						mis.running = false;
						mis.recovered = true;
						insertMission(mis);
					}
				} else if (!sub.getName().startsWith(".") && !new File(sub.getPath() + ".giga").exists()) {
					// Add a dummy mission for downloaded files
					DownloadMission mis = new DownloadMission();
					mis.length = sub.length();
					mis.done = mis.length;
					mis.finished = true;
					mis.running = false;
					mis.name = sub.getName();
					mis.location = mLocation;
					mis.timestamp = sub.lastModified();
					insertMission(mis);
				}
			}
		}
	}
	
	public DownloadMission getMission(int i) {
		return mMissions.get(i);
	}
	
	public int getCount() {
		return mMissions.size();
	}
	
	private int insertMission(DownloadMission mission) {
		int i = -1;
		
		DownloadMission m = null;
		
		if (mMissions.size() > 0) {
			do {
				m = mMissions.get(++i);
			} while (m.timestamp > mission.timestamp && i < mMissions.size() - 1);
			
			//if (i > 0) i--;
		} else {
			i = 0;
		}
		
		mMissions.add(i, mission);
		
		return i;
	}
	
	private class Initializer extends Thread {
		private Context context;
		private DownloadMission mission;
		
		public Initializer(Context context, DownloadMission mission) {
			this.context = context;
			this.mission = mission;
		}
		
		@Override
		public void run() {
			try {
				URL url = new URL(mission.url);
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				mission.length = conn.getContentLength();
				mission.blocks = mission.length / BLOCK_SIZE;
				
				if (mission.blocks * BLOCK_SIZE < mission.length) {
					mission.blocks++;
				}
				

				new File(mission.location).mkdirs();
				new File(mission.location + "/" + mission.name).createNewFile();
				RandomAccessFile af = new RandomAccessFile(mission.location + "/" + mission.name, "rw");
				af.setLength(mission.length);
				af.close();
				
				mission.start(context);
			} catch (Exception e) {
				// TODO Notify
				throw new RuntimeException(e);
			}
		}
	}
}
