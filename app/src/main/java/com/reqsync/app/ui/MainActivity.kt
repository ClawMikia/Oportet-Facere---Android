package com.reqsync.app.ui

import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.reqsync.app.R
import com.reqsync.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        binding.bottomNav.setupWithNavController(navController)

        // Ensure navigation works correctly by explicitly handling it
        binding.bottomNav.setOnItemSelectedListener { item ->
            val options = androidx.navigation.NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setRestoreState(true)
                .setPopUpTo(navController.graph.startDestinationId, inclusive = false, saveState = true)
                .build()
            
            try {
                navController.navigate(item.itemId, null, options)
                true
            } catch (e: Exception) {
                // Fallback to standard behavior if specialized navigation fails
                NavigationUI.onNavDestinationSelected(item, navController)
            }
        }

        // Hide bottom nav on detail screens
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.detailsFragment, R.id.notesFragment -> {
                    binding.bottomNav.visibility = View.GONE
                }
                else -> {
                    binding.bottomNav.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    /** Called by fragments to show a floating XP gain animation. */
    fun showXpGain(xp: Int) {
        binding.tvXpToast.apply {
            text = "+$xp XP"
            visibility = View.VISIBLE
            val anim = AnimationUtils.loadAnimation(context, R.anim.xp_float_up)
            anim.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                override fun onAnimationStart(a: android.view.animation.Animation?) {}
                override fun onAnimationRepeat(a: android.view.animation.Animation?) {}
                override fun onAnimationEnd(a: android.view.animation.Animation?) {
                    visibility = View.GONE
                }
            })
            startAnimation(anim)
        }
    }
}
