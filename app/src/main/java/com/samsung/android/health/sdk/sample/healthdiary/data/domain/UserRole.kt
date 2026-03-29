package com.samsung.android.health.sdk.sample.healthdiary.data.domain

import android.content.Context

/**
 * User role in the Digital Sanctuary health monitoring system.
 * Determines the onboarding flow and initial navigation path.
 */
enum class UserRole(val value: String, val displayName: String) {
    /**
     * Caregiver - monitors health of family members.
     * After role selection, navigates to FamilyLinkActivity.
     */
    CAREGIVER("caregiver", "Cuidador"),
    
    /**
     * Patient - person being monitored.
     * After role selection, navigates to WatchOnboardingActivity.
     */
    PATIENT("patient", "Persona a cuidar");
    
    companion object {
        private const val PREFS_NAME = "role_selection"
        private const val KEY_ROLE_COMPLETED = "role_selection_completed"
        private const val KEY_USER_ROLE = "user_role"
        
        /**
         * Save the selected user role to SharedPreferences.
         */
        fun saveToPreferences(context: Context, role: UserRole) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_USER_ROLE, role.value)
                .putBoolean(KEY_ROLE_COMPLETED, true)
                .apply()
        }
        
        /**
         * Get the saved user role from SharedPreferences.
         * @return The saved UserRole, or null if not set.
         */
        fun getFromPreferences(context: Context): UserRole? {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val roleValue = prefs.getString(KEY_USER_ROLE, null) ?: return null
            return entries.find { it.value == roleValue }
        }
        
        /**
         * Check if the user has completed role selection.
         * @return true if role has been selected and saved, false otherwise.
         */
        fun isRoleSelectionComplete(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_ROLE_COMPLETED, false)
        }
        
        /**
         * Clear the saved role selection (useful for testing or allowing role change).
         */
        fun clearRoleSelection(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .remove(KEY_USER_ROLE)
                .remove(KEY_ROLE_COMPLETED)
                .apply()
        }
        
        /**
         * Parse a string value to UserRole.
         * @return The matching UserRole, or null if no match found.
         */
        fun fromValue(value: String): UserRole? {
            return entries.find { it.value.equals(value, ignoreCase = true) }
        }
    }
}
