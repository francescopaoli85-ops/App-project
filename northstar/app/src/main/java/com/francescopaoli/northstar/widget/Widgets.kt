package com.francescopaoli.northstar.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.francescopaoli.northstar.MainActivity
import com.francescopaoli.northstar.NorthstarApp
import com.francescopaoli.northstar.data.Goal
import com.francescopaoli.northstar.domain.Focus
import com.francescopaoli.northstar.notify.Notifications
import com.francescopaoli.northstar.ui.Routes
import com.francescopaoli.northstar.ui.theme.Palette
import com.francescopaoli.northstar.ui.theme.Palettes

/** Dati e aggiornamento dei widget della schermata Home di Android. */
object Widgets {
    /** Obiettivi e tema attuali, letti fuori dall'app (il widget vive da solo). */
    suspend fun load(context: Context): Pair<List<Goal>, Palette> {
        val c = (context.applicationContext as NorthstarApp).container
        val goals = runCatching { c.backgroundRepository()?.getGoals().orEmpty() }.getOrDefault(emptyList())
        return goals to Palettes.byId(c.settings.current().theme)
    }

    /** Ridisegna tutti i widget (chiamato quando cambiano gli obiettivi). */
    suspend fun refresh(context: Context) {
        runCatching {
            TodayStepWidget().updateAll(context)
            PolarisWidget().updateAll(context)
        }
    }

    /** Apre l'app su una schermata precisa (Home, dettaglio, nuovo obiettivo). */
    fun open(context: Context, route: String) = actionStartActivity(
        Intent(context, MainActivity::class.java)
            .setData(Uri.parse("northstar://open/$route")) // intent distinti per ogni destinazione
            .putExtra(Notifications.EXTRA_ROUTE, route)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
    )
}

private fun color(c: Color) = ColorProvider(c)

// ---------------- Passo di oggi ----------------

/**
 * "Passo di oggi": la prossima azione dell'obiettivo più urgente (lo stesso Focus della Home).
 * La casella si spunta direttamente dal widget, senza aprire l'app.
 */
class TodayStepWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(DpSize(150.dp, 110.dp), DpSize(260.dp, 110.dp)))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val (goals, p) = Widgets.load(context)
        val focus = Focus.pick(goals)
        provideContent { TodayContent(context, focus, p) }
    }
}

@Composable
private fun TodayContent(context: Context, focus: Focus.Pick?, p: Palette) {
    val wide = LocalSize.current.width >= 250.dp
    Column(
        GlanceModifier.fillMaxSize().cornerRadius(22.dp).background(color(p.surface)).padding(14.dp)
            .clickable(Widgets.open(context, focus?.let { Routes.detail(it.goal.id) } ?: Routes.HOME)),
    ) {
        Text("✦ PASSO DI OGGI", style = TextStyle(color = color(p.accent2), fontSize = 10.sp, fontWeight = FontWeight.Bold))
        if (focus == null) {
            Spacer(GlanceModifier.height(8.dp))
            Text("Nessun obiettivo aperto", style = TextStyle(color = color(p.text), fontSize = 14.sp, fontWeight = FontWeight.Bold))
            Spacer(GlanceModifier.height(6.dp))
            Text(
                "+ Nuovo obiettivo", style = TextStyle(color = color(p.accent2), fontSize = 13.sp, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.clickable(Widgets.open(context, Routes.NEW)),
            )
            return@Column
        }
        Text(
            focus.goal.title, maxLines = 1,
            style = TextStyle(color = color(p.text2), fontSize = 11.sp),
            modifier = GlanceModifier.padding(top = 4.dp),
        )
        Spacer(GlanceModifier.height(8.dp))
        val a = focus.action
        Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth()) {
            // casella: un tocco e il passo è fatto (arriva da solo il successivo)
            Box(
                GlanceModifier.size(30.dp).cornerRadius(9.dp).background(color(p.accent1.copy(alpha = 0.35f)))
                    .clickable(
                        if (a != null) actionRunCallback<ToggleStepAction>(
                            actionParametersOf(ToggleStepAction.GOAL to focus.goal.id, ToggleStepAction.ACTION to a.id),
                        ) else Widgets.open(context, Routes.detail(focus.goal.id)),
                    ),
                contentAlignment = Alignment.Center,
            ) { Text(if (a != null) "✓" else "+", style = TextStyle(color = color(p.text), fontSize = 15.sp, fontWeight = FontWeight.Bold)) }
            Spacer(GlanceModifier.width(10.dp))
            Text(
                a?.text ?: "Aggiungi il prossimo passo", maxLines = if (wide) 2 else 3,
                style = TextStyle(color = color(if (a != null) p.text else p.soft), fontSize = 15.sp, fontWeight = FontWeight.Bold),
            )
        }
        if (focus.behind) {
            Spacer(GlanceModifier.height(6.dp))
            Text("un po' indietro: oggi conta", style = TextStyle(color = color(p.soft), fontSize = 10.sp))
        }
    }
}

/** Spunta l'azione dal widget: salva e ridisegna. */
class ToggleStepAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val goalId = parameters[GOAL] ?: return
        val actionId = parameters[ACTION] ?: return
        val c = (context.applicationContext as NorthstarApp).container
        val repo = c.backgroundRepository() ?: return
        val g = repo.getGoals().firstOrNull { it.id == goalId } ?: return
        val now = System.currentTimeMillis()
        repo.upsert(g.copy(actions = g.actions.map { if (it.id == actionId) it.copy(done = true, doneAt = now) else it }))
        Widgets.refresh(context)
    }

    companion object {
        val GOAL = ActionParameters.Key<String>("goal")
        val ACTION = ActionParameters.Key<String>("action")
    }
}

class TodayStepWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayStepWidget()
}

// ---------------- Stella Polare ----------------

/** "Stella Polare": i tuoi obiettivi con avanzamento e giorni rimasti; tocchi e apri quello. */
class PolarisWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(DpSize(250.dp, 110.dp), DpSize(250.dp, 220.dp)))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val (goals, p) = Widgets.load(context)
        val open = goals.filter { it.isOpen }.sortedWith(compareBy({ !it.isDue() }, { it.deadlineEpochDay }))
        val achieved = goals.count { !it.isOpen }
        provideContent { PolarisContent(context, open, achieved, p) }
    }
}

@Composable
private fun PolarisContent(context: Context, open: List<Goal>, achieved: Int, p: Palette) {
    val rows = if (LocalSize.current.height >= 200.dp) 4 else 2
    Column(
        GlanceModifier.fillMaxSize().cornerRadius(22.dp).background(color(p.night)).padding(14.dp)
            .clickable(Widgets.open(context, Routes.POLARIS)),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth()) {
            Text("✦ La tua Stella Polare", style = TextStyle(color = color(p.text), fontSize = 13.sp, fontWeight = FontWeight.Bold))
            Spacer(GlanceModifier.defaultWeight())
            Text("${open.size} attivi · $achieved raggiunti", style = TextStyle(color = color(p.text3), fontSize = 10.sp))
        }
        if (open.isEmpty()) {
            Spacer(GlanceModifier.height(10.dp))
            Text(
                "+ Crea il tuo primo obiettivo", style = TextStyle(color = color(p.accent2), fontSize = 13.sp, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.clickable(Widgets.open(context, Routes.NEW)),
            )
        }
        open.take(rows).forEach { g ->
            Spacer(GlanceModifier.height(9.dp))
            Column(GlanceModifier.fillMaxWidth().clickable(Widgets.open(context, Routes.detail(g.id)))) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth()) {
                    Text(g.title, maxLines = 1, style = TextStyle(color = color(p.text), fontSize = 12.5.sp, fontWeight = FontWeight.Bold),
                        modifier = GlanceModifier.defaultWeight())
                    val days = g.daysLeft()
                    Text(
                        when { g.isDue() -> "oggi ✦"; days == 1L -> "1 giorno"; else -> "$days giorni" },
                        style = TextStyle(color = color(if (g.isDue()) p.accent2 else p.text3), fontSize = 10.sp),
                    )
                }
                Spacer(GlanceModifier.height(4.dp))
                LinearProgressIndicator(
                    progress = g.progress,
                    modifier = GlanceModifier.fillMaxWidth().height(5.dp),
                    color = color(p.accent2), backgroundColor = color(p.track),
                )
            }
        }
    }
}

class PolarisWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PolarisWidget()
}
