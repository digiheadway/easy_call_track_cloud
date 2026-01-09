/**
 * Consolidated Utilities Package
 * 
 * This package consolidates utilities from:
 * - com.miniclick.calltrackmanage.util (original location)
 * - com.miniclick.calltrackmanage.ui.utils (UI-related utilities)
 * 
 * For NEW CODE, prefer importing from this package directly.
 * OLD CODE using ui.utils will continue to work due to backward compatibility.
 * 
 * Usage:
 * ```kotlin
 * import com.miniclick.calltrackmanage.util.NetworkConnectivityObserver
 * import com.miniclick.calltrackmanage.util.LogExporter
 * ```
 */
package com.miniclick.calltrackmanage.util

// This file serves as documentation for the util package organization.
// All classes in this package are self-contained in their own files:
//
// NetworkConnectivityObserver.kt - Network state observation via Flows
// LogExporter.kt - Debug log export and sharing functionality
//
// UI-related utilities remain in ui.utils for Compose layer separation:
// - FormatUtils.kt - Duration/time/date formatting
// - CallUtils.kt - Phone call initiation utilities
// - WhatsAppUtils.kt - WhatsApp integration utilities
