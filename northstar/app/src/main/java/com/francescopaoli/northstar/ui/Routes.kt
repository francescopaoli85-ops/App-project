package com.francescopaoli.northstar.ui

/** Percorsi di navigazione tra le schermate. */
object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val NEW = "new"
    const val DETAIL = "detail/{id}"
    const val ACHIEVEMENTS = "achievements"
    const val CALENDAR = "calendar"
    const val CHECKIN = "checkin/{id}"
    const val CELEBRATE = "celebrate/{id}"
    const val SETTINGS = "settings"
    const val WEEK = "week"
    const val POLARIS = "polaris"

    fun detail(id: String) = "detail/$id"
    fun checkin(id: String) = "checkin/$id"
    fun celebrate(id: String) = "celebrate/$id"
}
