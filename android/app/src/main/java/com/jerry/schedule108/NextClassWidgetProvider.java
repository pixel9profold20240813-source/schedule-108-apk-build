package com.jerry.schedule108;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.RemoteViews;

import java.util.Calendar;
import java.util.List;

public class NextClassWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            updateWidget(context, appWidgetManager, id);
        }
    }

    /** 給 ClassStatusService 每次重算時呼叫，讓小工具跟通知同步更新，不用等系統的半小時排程。 */
    static void updateAllWidgets(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName component = new ComponentName(context, NextClassWidgetProvider.class);
        int[] ids = manager.getAppWidgetIds(component);
        for (int id : ids) {
            updateWidget(context, manager, id);
        }
    }

    private static void updateWidget(Context context, AppWidgetManager manager, int widgetId) {
        List<ScheduleData.Entry> entries = ScheduleData.buildEntries();
        ScheduleData.Status status = ScheduleData.computeStatus(entries, Calendar.getInstance());
        ScheduleData.Entry next = status.next;

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_next_class);

        if (next != null) {
            views.setTextViewText(R.id.widget_subject, next.subject);
            String[] wd = {"日", "一", "二", "三", "四", "五", "六"};
            String meta = next.period.label + " · " + next.period.toClock();
            // 如果下一節不是今天（跨日/跨週），標上星期幾
            Calendar cal = Calendar.getInstance();
            int dow = cal.get(Calendar.DAY_OF_WEEK) - 1;
            if (next.weekday != dow) meta = "週" + wd[next.weekday] + " " + meta;
            views.setTextViewText(R.id.widget_meta, meta);
        } else {
            views.setTextViewText(R.id.widget_subject, "本週課程已結束");
            views.setTextViewText(R.id.widget_meta, "");
        }

        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        if (launchIntent != null) {
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, launchIntent, flags);
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent);
        }

        manager.updateAppWidget(widgetId, views);
    }
}
