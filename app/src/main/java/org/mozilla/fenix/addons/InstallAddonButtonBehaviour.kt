package org.mozilla.fenix.addons

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.annotation.Keep
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton.OnVisibilityChangedListener

@Keep
class InstallAddonButtonBehaviour(ctx: Context, attrs: AttributeSet?) :
    FloatingActionButton.Behavior(ctx, attrs) {

    override fun onStartNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: FloatingActionButton,
        directTargetChild: View,
        target: View,
        axes: Int,
        type: Int
    ): Boolean {
        return super.onStartNestedScroll(
            coordinatorLayout,
            child,
            directTargetChild,
            target,
            axes,
            type
        ) || axes == ViewCompat.SCROLL_AXIS_VERTICAL
    }

    override fun onNestedPreScroll(
        coordinatorLayout: CoordinatorLayout,
        child: FloatingActionButton,
        target: View,
        dx: Int,
        dy: Int,
        consumed: IntArray,
        type: Int
    ) {
        super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed, type)
        if (dy < 0 && child.isEnabled && child.isOrWillBeHidden)
            child.show()
        if (dy > 0 && child.isEnabled && child.isOrWillBeShown) {
            child.hide(object : OnVisibilityChangedListener() {
                override fun onHidden(fab: FloatingActionButton?) {
                    fab?.visibility = View.INVISIBLE
                }
            })
        }
    }
}
