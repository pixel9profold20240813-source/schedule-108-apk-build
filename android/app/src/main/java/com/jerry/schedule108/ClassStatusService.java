package com.jerry.schedule108;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.util.Calendar;
import java.util.List;

public class ClassStatusService extends Service {

    private static final String CHANNEL_ID = "class_status_channel";
    private static final int NOTIF_ID = 1001;
    private static final long UPDATE_INTERVAL_MS = 30 * 1000; // 每 30 秒重算一次

    private Handler handler;
    private Runnable updateRunnable;
    private final List<ScheduleData.Entry> entries = ScheduleData.buildEntries();

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
        handler = new Handler(Looper.getMainLooper());
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                Notification n = buildNotification();
                NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (nm != null) nm.notify(NOTIF_ID, n);
                NextClassWidgetProvider.updateAllWidgets(getApplicationContext());
                handler.postDelayed(this, UPDATE_INTERVAL_MS);
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIF_ID, buildNotification());
        NextClassWidgetProvider.updateAllWidgets(getApplicationContext());
        handler.removeCallbacks(updateRunnable);
        handler.postDelayed(updateRunnable, UPDATE_INTERVAL_MS);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null) handler.removeCallbacks(updateRunnable);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Intent restart = new Intent(getApplicationContext(), ClassStatusService.class);
        ContextCompat.startForegroundService(getApplicationContext(), restart);
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "課程狀態常駐通知", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("持續顯示目前/下一節課程與倒數時間");
            channel.setShowBadge(false);
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        ScheduleData.Status status = ScheduleData.computeStatus(entries, Calendar.getInstance());
        ScheduleData.Entry current = status.current;
        ScheduleData.Entry next = status.next;

        Calendar cal = Calendar.getInstance();
        int dow = cal.get(Calendar.DAY_OF_WEEK) - 1;
        int nowMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);

        String title;
        String body;

        if (current != null) {
            int remain = Math.max(0, current.period.endMin - nowMin);
            title = current.period.label + " · " + current.subject;
            String teacherPart = (current.teacher != null && !current.teacher.isEmpty()) ? current.teacher + " · " : "";
            body = teacherPart + "還有 " + remain + " 分鐘下課";
            if (next != null) body += "　下一節：" + next.subject;
        } else if (next != null && next.weekday == dow) {
            int remain = Math.max(0, next.period.startMin - nowMin);
            title = "下課中";
            body = "還有 " + remain + " 分鐘上課　下一節：" + next.period.label + " " + next.subject;
        } else if (next != null) {
            String[] wd = {"日", "一", "二", "三", "四", "五", "六"};
            title = "目前沒有課程";
            body = "下次上課：週" + wd[next.weekday] + " " + next.period.label + " " + next.subject;
        } else {
            title = "108班課表";
            body = "目前沒有課程";
        }

        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        PendingIntent pendingIntent = null;
        if (launchIntent != null) {
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
            pendingIntent = PendingIntent.getActivity(this, 0, launchIntent, flags);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_clock)
                .setContentTitle(title)
                .setContentText(body)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setShowWhen(false);

        if (pendingIntent != null) builder.setContentIntent(pendingIntent);

        return builder.build();
    }
}
