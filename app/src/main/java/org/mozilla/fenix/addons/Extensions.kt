/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.addons

import android.view.View
import mozilla.components.concept.engine.webextension.isUnsupported
import mozilla.components.feature.addons.Addon
import mozilla.components.feature.addons.AddonManager
import mozilla.components.support.webextensions.WebExtensionSupport
import org.mozilla.fenix.components.FenixSnackbar

/**
 * Shows the Fenix Snackbar in the given view along with the provided text.
 *
 * @param view A [View] used to determine a parent for the [FenixSnackbar].
 * @param text The text to display in the [FenixSnackbar].
 */
internal fun showSnackBar(view: View, text: String, duration: Int = FenixSnackbar.LENGTH_SHORT) {
    FenixSnackbar.make(
        view = view,
        duration = duration,
        isDisplayedWithBrowserToolbar = true
    )
        .setText(text)
        .show()
}

suspend fun AddonManager.getAllAddons(waitForPendingActions: Boolean = true, allowCache: Boolean = true): List<Addon> {
    return getAddons(waitForPendingActions, allowCache).map {
        val ext = WebExtensionSupport.installedExtensions[it.id]
        if (ext?.isUnsupported() == false &&
            (!it.isSupported() || it.isDisabledAsUnsupported())
        ) {
            val meta = ext.getMetadata()
            it.copy(
                installedState = Addon.InstalledState(
                    id = ext.id,
                    version = meta?.version ?: "",
                    optionsPageUrl = meta?.optionsPageUrl,
                    openOptionsPageInTab = meta?.openOptionsPageInTab ?: false,
                    enabled = ext.isEnabled(),
                    supported = true,
                    disabledAsUnsupported = false,
                    allowedInPrivateBrowsing = ext.isAllowedInPrivateBrowsing()
                )
            )
        } else {
            it
        }
    }
}
