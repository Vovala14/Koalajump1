package com.lavrik.koalajump.ui

/**
 * Defines the possible states of the main menu
 */
sealed class MenuState {
    object Idle : MenuState()
    object Starting : MenuState()
    object ShowingSettings : MenuState()
}