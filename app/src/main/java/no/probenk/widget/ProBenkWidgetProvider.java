package no.probenk.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.BroadcastReceiver.PendingResult;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProBenkWidgetProvider extends AppWidgetProvider {
    public static final String ACTION_REFRESH = "no.probenk.widget.REFRESH";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private static final int[] ROWS = {R.id.row1, R.id.row2, R.id.row3, R.id.row4, R.id.row5};
    private static final int[] TITLES = {R.id.row1Title, R.id.row2Title, R.id.row3Title, R.id.row4Title, R.id.row5Title};
    private static final int[] SUBS = {R.id.row1Sub, R.id.row2Sub, R.id.row3Sub, R.id.row4Sub, R.id.row5Sub};

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        final PendingResult pending = goAsync();
        EXECUTOR.execute(() -> {
            try {
                for (int id : appWidgetIds) updateOneBlocking(context, manager, id, false);
            } finally {
                pending.finish();
            }
        });
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager manager, int appWidgetId, Bundle newOptions) {
        final PendingResult pending = goAsync();
        EXECUTOR.execute(() -> {
            try {
                updateOneBlocking(context, manager, appWidgetId, false);
            } finally {
                pending.finish();
            }
        });
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_REFRESH.equals(intent.getAction())) {
            final PendingResult pending = goAsync();
            EXECUTOR.execute(() -> {
                try {
                    AppWidgetManager manager = AppWidgetManager.getInstance(context);
                    int[] ids = manager.getAppWidgetIds(new ComponentName(context, ProBenkWidgetProvider.class));
                    for (int id : ids) updateOneBlocking(context, manager, id, true);
                } finally {
                    pending.finish();
                }
            });
            return;
        }
        super.onReceive(context, intent);
    }

    public static void refreshAll(Context context, boolean showLoading) {
        EXECUTOR.execute(() -> {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            int[] ids = manager.getAppWidgetIds(new ComponentName(context, ProBenkWidgetProvider.class));
            for (int id : ids) updateOneBlocking(context, manager, id, showLoading);
        });
    }

    private static void updateOneBlocking(Context context, AppWidgetManager manager, int widgetId, boolean showLoading) {
        if (showLoading) {
            RemoteViews loading = baseViews(context);
            loading.setTextViewText(R.id.summaryText, "Oppdaterer …");
            setRowsVisible(loading, 0);
            manager.updateAppWidget(widgetId, loading);
        }

        try {
            String token = Prefs.token(context);
            if (token.isEmpty()) {
                manager.updateAppWidget(widgetId, disconnectedViews(context));
                return;
            }

            JSONObject data = ApiClient.fetchToday(Prefs.baseUrl(context), token);
            manager.updateAppWidget(widgetId, dataViews(context, manager, widgetId, data));
        } catch (Exception e) {
            manager.updateAppWidget(widgetId, errorViews(context, e.getMessage()));
        }
    }

    private static RemoteViews baseViews(Context context) {
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_probenk);
        rv.setOnClickPendingIntent(R.id.refreshButton, refreshIntent(context));
        return rv;
    }

    private static RemoteViews disconnectedViews(Context context) {
        RemoteViews rv = baseViews(context);
        rv.setTextViewText(R.id.dateText, "");
        rv.setTextViewText(R.id.summaryText, "Koble til ProBenk");
        rv.setTextViewText(R.id.footerText, "Trykk for oppsett");
        setRowsVisible(rv, 0);
        rv.setOnClickPendingIntent(R.id.widgetRoot, appIntent(context));
        return rv;
    }

    private static RemoteViews errorViews(Context context, String message) {
        RemoteViews rv = baseViews(context);
        rv.setTextViewText(R.id.dateText, "");
        rv.setTextViewText(R.id.summaryText, "Kunne ikke oppdatere");
        rv.setTextViewText(R.id.footerText, trim(message, 62));
        setRowsVisible(rv, 0);
        rv.setOnClickPendingIntent(R.id.widgetRoot, appIntent(context));
        return rv;
    }

    private static RemoteViews dataViews(Context context, AppWidgetManager manager, int widgetId, JSONObject data) {
        RemoteViews rv = baseViews(context);

        String date = data.optString("date", "");
        rv.setTextViewText(R.id.dateText, prettyDate(date));

        JSONArray today = data.optJSONArray("today");
        JSONArray upcoming = data.optJSONArray("upcoming");
        JSONArray unavailable = data.optJSONArray("unavailable");
        int todayCount = today == null ? 0 : today.length();
        int upcomingCount = upcoming == null ? 0 : upcoming.length();

        String summary = todayCount == 0 ? "Ingen gjøremål i dag" : todayCount + (todayCount == 1 ? " gjøremål i dag" : " gjøremål i dag");
        if (unavailable != null && unavailable.length() > 0) summary += " · Utilgjengelig";
        rv.setTextViewText(R.id.summaryText, summary);

        Bundle options = manager.getAppWidgetOptions(widgetId);
        int minHeight = options == null ? 180 : options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 180);
        int rowsToShow = minHeight < 145 ? 1 : (minHeight < 245 ? 3 : 5);

        JSONArray source = todayCount > 0 ? today : upcoming;
        int available = source == null ? 0 : source.length();
        int count = Math.min(rowsToShow, available);
        setRowsVisible(rv, count);

        for (int i = 0; i < count; i++) {
            JSONObject item = source.optJSONObject(i);
            if (item == null) continue;

            String time = item.optString("time", "");
            String label = item.optString("label", "Oppdrag");
            String customer = item.optString("customer_name", "");
            String title = item.optString("project_title", "");
            String address = item.optString("address", "");
            int projectId = item.optInt("project_id", 0);

            String top = (time.isEmpty() ? "--:--" : time) + "  " + label;
            String bottom = "#" + projectId + " " + (customer.isEmpty() ? title : customer);
            if (!address.isEmpty()) bottom += " · " + address;

            rv.setTextViewText(TITLES[i], trim(top, 45));
            rv.setTextViewText(SUBS[i], trim(bottom, 80));
            rv.setOnClickPendingIntent(ROWS[i], browserIntent(context, Prefs.baseUrl(context) + item.optString("path", "dashboard.php"), 100 + i));
        }

        if (todayCount == 0 && upcomingCount > 0) {
            rv.setTextViewText(R.id.summaryText, "Ingen i dag · neste planlagte");
        }

        String footer = todayCount > rowsToShow ? "+ " + (todayCount - rowsToShow) + " flere i dag" : "Neste 7 dager: " + upcomingCount;
        rv.setTextViewText(R.id.footerText, footer);

        JSONObject links = data.optJSONObject("links");
        String calendar = links == null ? "calendar.php" : links.optString("calendar", "calendar.php");
        rv.setOnClickPendingIntent(R.id.header, browserIntent(context, Prefs.baseUrl(context) + calendar, 2));
        rv.setOnClickPendingIntent(R.id.summaryText, browserIntent(context, Prefs.baseUrl(context) + calendar, 3));
        return rv;
    }

    private static void setRowsVisible(RemoteViews rv, int visibleCount) {
        for (int i = 0; i < ROWS.length; i++) {
            rv.setViewVisibility(ROWS[i], i < visibleCount ? View.VISIBLE : View.GONE);
        }
    }

    private static PendingIntent refreshIntent(Context context) {
        Intent i = new Intent(context, ProBenkWidgetProvider.class).setAction(ACTION_REFRESH);
        return PendingIntent.getBroadcast(context, 1, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static PendingIntent appIntent(Context context) {
        Intent i = new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(context, 4, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static PendingIntent browserIntent(Context context, String url, int requestCode) {
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(context, requestCode, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static String prettyDate(String iso) {
        try {
            LocalDate d = LocalDate.parse(iso);
            DateTimeFormatter f = DateTimeFormatter.ofPattern("EEE d. MMM", new Locale("nb", "NO"));
            String s = d.format(f);
            return s.substring(0, 1).toUpperCase(new Locale("nb", "NO")) + s.substring(1);
        } catch (Exception e) {
            return iso;
        }
    }

    private static String trim(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, Math.max(0, max - 1)) + "…";
    }
}
