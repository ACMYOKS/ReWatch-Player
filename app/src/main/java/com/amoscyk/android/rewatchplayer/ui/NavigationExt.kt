package com.amoscyk.android.rewatchplayer.ui

import android.util.SparseArray
import android.view.MenuItem
import androidx.core.util.forEach
import androidx.core.util.set
import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * Simple setup extension from Google's navigationadvancedsample
 * */

fun BottomNavigationView.setupWithNavController(
    navGraphIds: List<Int>,
    fragmentManager: FragmentManager,
    containerId: Int,
    requireAttach: Boolean,
    onSelectedChange: ((item: MenuItem) -> Unit)? = null
) {
    val graphIdToTagMap = SparseArray<String>()
    var firstFragmentTagId = 0

    navGraphIds.forEachIndexed { index, navGraphId ->
        val fragmentTag = getFragmentTag(index)
        val navHostFrag = obtainNavHostFragment(
            fragmentManager,
            fragmentTag,
            navGraphId,
            containerId)
        val graphId = navHostFrag.navController.graph.id
        if (index == 0) {
            firstFragmentTagId = graphId
        }
        graphIdToTagMap[graphId] = fragmentTag
        if (requireAttach) {
            if (this.selectedItemId == graphId) {
                attachNavHostFragment(fragmentManager, navHostFrag, index == 0)
            } else {
                detachNavHostFragment(fragmentManager, navHostFrag)
            }
        }
    }

    var selectedItemTag = graphIdToTagMap[this.selectedItemId]

    setOnNavigationItemSelectedListener { item ->
        if (fragmentManager.isStateSaved) {
            false
        } else {
            val newSelectedItemTag = graphIdToTagMap[item.itemId]
            if (newSelectedItemTag != selectedItemTag) {
                onSelectedChange?.invoke(item)
                // attach nav host fragment for newly selected item
                val selectedFrag = fragmentManager.findFragmentByTag(newSelectedItemTag)
                        as NavHostFragment
                fragmentManager.beginTransaction()
                    .attach(selectedFrag)
                    .setPrimaryNavigationFragment(selectedFrag)
                    .apply {
                        graphIdToTagMap.forEach { _, fragTag ->
                            if (fragTag != newSelectedItemTag) {
                                detach(fragmentManager.findFragmentByTag(fragTag)!!)
                            }
                        }
                    }
                    .commit()
                selectedItemTag = newSelectedItemTag
                true
            } else {
                true
            }
        }
    }
}

private fun obtainNavHostFragment(
    fragmentManager: FragmentManager,
    fragmentTag: String,
    navGraphId: Int,
    containerId: Int
): NavHostFragment {
    val existingFrag = fragmentManager.findFragmentByTag(fragmentTag) as NavHostFragment?
    existingFrag?.let { return it }
    val navHostFrag = NavHostFragment.create(navGraphId)
    fragmentManager.beginTransaction()
        .add(containerId, navHostFrag, fragmentTag)
        .commitNow()
    return navHostFrag
}

private fun attachNavHostFragment(
    fragmentManager: FragmentManager,
    navHostFragment: NavHostFragment,
    isPrimaryNavFragment: Boolean
) {
    fragmentManager.beginTransaction()
        .attach(navHostFragment)
        .apply {
            if (isPrimaryNavFragment) {
                setPrimaryNavigationFragment(navHostFragment)
            }
        }
        .commitNow()
}

private fun detachNavHostFragment(
    fragmentManager: FragmentManager,
    navHostFragment: NavHostFragment
) {
    fragmentManager.beginTransaction()
        .detach(navHostFragment)
        .commitNow()
}

private fun getFragmentTag(index: Int) = "navHostFragment:$index"