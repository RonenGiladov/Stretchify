package com.stretchify.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.stretchify.MainActivity
import com.stretchify.R
import com.stretchify.data.RoutineCatalog
import com.stretchify.data.StretchifyRepository

class StretchWidgetProvider : AppWidgetProvider()
{
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray)
    {
        val repository = StretchifyRepository(context)
        val catalog = RoutineCatalog.build(repository.loadCustomRoutines(), repository.loadRoutineOverrides())
        val lastRoutine = repository.loadCompletionRecords()
            .sortedByDescending { it.completedAtMillis }
            .firstNotNullOfOrNull { record -> catalog.firstOrNull { it.id == record.routineId } }
        appWidgetIds.forEach { appWidgetId ->
            val views = RemoteViews(context.packageName, R.layout.stretch_widget)
            val intent = Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            if (lastRoutine != null)
            {
                views.setTextViewText(R.id.widget_routine_title, lastRoutine.title)
                views.setTextViewText(R.id.widget_action, "Repeat routine")
                intent.action = ACTION_START_ROUTINE
                intent.putExtra(EXTRA_ROUTINE_ID, lastRoutine.id)
            }
            else
            {
                views.setTextViewText(R.id.widget_routine_title, "Ready to move?")
                views.setTextViewText(R.id.widget_action, "Open Stretchify")
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                10,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_action, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    companion object
    {
        const val ACTION_START_ROUTINE = "com.stretchify.action.START_ROUTINE"
        const val EXTRA_ROUTINE_ID = "routine_id"

        fun updateAll(context: Context)
        {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, StretchWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isNotEmpty())
            {
                StretchWidgetProvider().onUpdate(context, appWidgetManager, appWidgetIds)
            }
        }
    }
}
