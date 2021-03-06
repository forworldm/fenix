/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabtray

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.tabs.TabLayout
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.component_tabstray.*
import kotlinx.android.synthetic.main.component_tabstray.view.*
import kotlinx.android.synthetic.main.component_tabstray_fab.view.*
import mozilla.components.browser.menu.BrowserMenuBuilder
import mozilla.components.browser.menu.item.SimpleBrowserMenuItem
import mozilla.components.browser.state.selector.normalTabs
import mozilla.components.browser.state.selector.privateTabs
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.tabstray.BrowserTabsTray
import mozilla.components.concept.tabstray.Tab
import mozilla.components.concept.tabstray.TabsTray
import mozilla.components.feature.tabs.tabstray.TabsFeature
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.components

interface TabTrayInteractor {
    fun onTabClosed(tab: Tab)
    fun onTabSelected(tab: Tab)
    fun onNewTabTapped(private: Boolean)
    fun onTabTrayDismissed()
    fun onShareTabsClicked(private: Boolean)
    fun onSaveToCollectionClicked()
    fun onCloseAllTabsClicked(private: Boolean)
}
/**
 * View that contains and configures the BrowserAwesomeBar
 */
class TabTrayView(
    private val container: ViewGroup,
    private val interactor: TabTrayInteractor,
    private val isPrivate: Boolean
) : LayoutContainer, TabsTray.Observer, TabLayout.OnTabSelectedListener {
    val fabView = LayoutInflater.from(container.context)
        .inflate(R.layout.component_tabstray_fab, container, true)

    val view = LayoutInflater.from(container.context)
        .inflate(R.layout.component_tabstray, container, true)

    val isPrivateModeSelected: Boolean get() = view.tab_layout.selectedTabPosition == PRIVATE_TAB_ID

    private val behavior = BottomSheetBehavior.from(view.tab_wrapper)
    private var tabsFeature: TabsFeature
    private var tabTrayItemMenu: TabTrayItemMenu

    override val containerView: View?
        get() = container

    init {
        fabView.new_tab_button.compatElevation = ELEVATION

        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                if (slideOffset >= SLIDE_OFFSET) {
                    fabView.new_tab_button.show()
                } else {
                    fabView.new_tab_button.hide()
                }
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    interactor.onTabTrayDismissed()
                }
            }
        })

        val selectedTabIndex = if (!isPrivate) {
            DEFAULT_TAB_ID
        } else {
            PRIVATE_TAB_ID
        }

        view.tab_layout.getTabAt(selectedTabIndex)?.also {
            view.tab_layout.selectTab(it, true)
        }

        view.tab_layout.addOnTabSelectedListener(this)

        tabsFeature = TabsFeature(
            view.tabsTray,
            view.context.components.core.store,
            view.context.components.useCases.tabsUseCases,
            { it.content.private == isPrivate },
            { })

        (view.tabsTray as? BrowserTabsTray)?.also { tray ->
            TabsTouchHelper(tray.tabsAdapter).attachToRecyclerView(tray)
        }

        tabTrayItemMenu = TabTrayItemMenu(view.context, { view.tab_layout.selectedTabPosition == 0 }) {
            when (it) {
                is TabTrayItemMenu.Item.ShareAllTabs -> interactor.onShareTabsClicked(
                    isPrivateModeSelected
                )
                is TabTrayItemMenu.Item.SaveToCollection -> interactor.onSaveToCollectionClicked()
                is TabTrayItemMenu.Item.CloseAllTabs -> interactor.onCloseAllTabsClicked(
                    isPrivateModeSelected
                )
            }
        }

        view.tab_tray_overflow.setOnClickListener {
            tabTrayItemMenu.menuBuilder
                .build(view.context)
                .show(anchor = it)
        }

        fabView.new_tab_button.setOnClickListener {
            interactor.onNewTabTapped(isPrivateModeSelected)
        }

        tabsTray.register(this)
        tabsFeature.start()
    }

    override fun onTabSelected(tab: Tab) {
        interactor.onTabSelected(tab)
    }

    override fun onTabSelected(tab: TabLayout.Tab?) {
        // We need a better way to determine which tab was selected.
        val filter: (TabSessionState) -> Boolean = when (tab?.position) {
            1 -> { state -> state.content.private }
            else -> { state -> !state.content.private }
        }

        tabsFeature.filterTabs(filter)

        updateState(view.context.components.core.store.state)
    }

    fun updateState(state: BrowserState) {
        val shouldHide = if (isPrivateModeSelected) {
            state.privateTabs.isEmpty()
        } else {
            state.normalTabs.isEmpty()
        }
        view?.tab_tray_overflow?.isVisible = !shouldHide
    }

    override fun onTabClosed(tab: Tab) {
        interactor.onTabClosed(tab)
    }
    override fun onTabReselected(tab: TabLayout.Tab?) { /*noop*/ }
    override fun onTabUnselected(tab: TabLayout.Tab?) { /*noop*/ }

    companion object {
        private const val DEFAULT_TAB_ID = 0
        private const val PRIVATE_TAB_ID = 1
        private const val SLIDE_OFFSET = 0
        private const val ELEVATION = 90f
    }
}

class TabTrayItemMenu(
    private val context: Context,
    private val shouldShowSaveToCollection: () -> Boolean,
    private val onItemTapped: (Item) -> Unit = {}
) {

    sealed class Item {
        object ShareAllTabs : Item()
        object SaveToCollection : Item()
        object CloseAllTabs : Item()
    }

    val menuBuilder by lazy { BrowserMenuBuilder(menuItems) }

    private val menuItems by lazy {
        listOf(
            SimpleBrowserMenuItem(
                context.getString(R.string.tab_tray_menu_item_save),
                textColorResource = R.color.primary_text_normal_theme
            ) {
                onItemTapped.invoke(Item.SaveToCollection)
            }.apply { visible = shouldShowSaveToCollection },

            SimpleBrowserMenuItem(
                context.getString(R.string.tab_tray_menu_item_share),
                textColorResource = R.color.primary_text_normal_theme
            ) {
                onItemTapped.invoke(Item.ShareAllTabs)
            },

            SimpleBrowserMenuItem(
                context.getString(R.string.tab_tray_menu_item_close),
                textColorResource = R.color.primary_text_normal_theme
            ) {
                onItemTapped.invoke(Item.CloseAllTabs)
            }
        )
    }
}
