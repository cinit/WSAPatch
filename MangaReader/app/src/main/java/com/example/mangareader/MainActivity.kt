package com.example.mangareader

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.mangareader.ui.browse.BrowseFragment
import com.example.mangareader.ui.downloads.DownloadsFragment // Add import
import com.example.mangareader.ui.local.LocalBrowseFragment // Add import for LocalBrowseFragment
// Import other fragments if you create them e.g. LibraryFragment
// import com.example.mangareader.ui.library.LibraryFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigation.setOnItemSelectedListener { item ->
            var selectedFragment: Fragment? = null
            when (item.itemId) {
                R.id.navigation_library -> {
                    // selectedFragment = LibraryFragment.newInstance() // Placeholder
                    // For now, just show a placeholder fragment or clear
                    selectedFragment = PlaceholderFragment.newInstance("Library")
                }
                R.id.navigation_browse -> {
                    selectedFragment = BrowseFragment.newInstance()
                }
                        R.id.navigation_downloads -> {
                            selectedFragment = DownloadsFragment.newInstance()
                        }
                        R.id.navigation_local_browse -> {
                            selectedFragment = LocalBrowseFragment.newInstance()
                        }
                R.id.navigation_settings -> {
                    // Option 1: Launch SettingsActivity
                     startActivity(Intent(this, SettingsActivity::class.java))
                    // Option 2: Use a SettingsFragment (if you create one)
                    // selectedFragment = SettingsFragment.newInstance()
                    // For now, if we launch activity, don't set a fragment
                    return@setOnItemSelectedListener true // Consume event
                }
            }

            if (selectedFragment != null) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment_container, selectedFragment)
                    .commit()
            }
            true
        }

        // Set default selected item and load initial fragment
        if (savedInstanceState == null) {
            bottomNavigation.selectedItemId = R.id.navigation_browse
            // Manually trigger listener or load fragment directly for the first time
             supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment_container, BrowseFragment.newInstance())
                .commit()
        }
    }

    override fun onBackPressed() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_container)
        // Get the current fragment shown in the container
        val currentFragment = navHostFragment?.childFragmentManager?.fragments?.getOrNull(0)

        if (currentFragment is LocalBrowseFragment) {
            if (currentFragment.canGoUp()) {
                currentFragment.goUp()
            } else {
                // If LocalBrowseFragment is at its root, check if it's the initial fragment of the app.
                // If not (e.g., user navigated from Browse to Local), then default back press (handled by nav component or exiting) is fine.
                // If it IS the initial view or you want specific behavior:
                // super.onBackPressed() // Default behavior
                // Or, if you want to switch to a default tab like "Browse" instead of exiting:
                // val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
                // bottomNavigation.selectedItemId = R.id.navigation_browse
                // But this might conflict with NavController if one is used.
                // For now, simplest is to let super.onBackPressed() handle it if canGoUp is false.
                super.onBackPressed()
            }
        } else {
            super.onBackPressed()
        }
    }

    fun navigateToMangaDetail(mangaId: String) {
        val fragment = com.example.mangareader.ui.detail.MangaDetailFragment.newInstance(mangaId)
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment_container, fragment)
            .addToBackStack(null) // Add to back stack so user can navigate back
            .commit()
    }
}

// Create a simple PlaceholderFragment for Library/Settings until they are implemented
class PlaceholderFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val textView = android.widget.TextView(activity)
        textView.text = arguments?.getString("text_key") ?: "Placeholder"
        textView.textSize = 24f
        textView.gravity = android.view.Gravity.CENTER
        return textView
    }
    companion object {
        fun newInstance(text: String): PlaceholderFragment {
            val args = Bundle()
            args.putString("text_key", text)
            val fragment = PlaceholderFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
