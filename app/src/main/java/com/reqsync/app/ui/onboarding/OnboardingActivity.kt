package com.reqsync.app.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.reqsync.app.R
import com.reqsync.app.ui.MainActivity
import com.reqsync.app.utils.PreferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class OnboardingPage(val title: String, val description: String, val emoji: String)

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var btnNext: TextView
    private lateinit var btnSkip: TextView
    private lateinit var dotIndicator: LinearDotIndicator
    private lateinit var prefs: PreferencesDataStore

    private val pages = listOf(
        OnboardingPage("MISSION CONTROL",
            "Track all your employment requirements in one cyberpunk command center.", "⬡"),
        OnboardingPage("SMART PARSING",
            "Paste raw requirement lists and let ReqSync auto-organize them into missions.", "⌗"),
        OnboardingPage("LEVEL UP",
            "Earn XP, unlock achievements, and rise through ranks as you complete requirements.", "★")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = PreferencesDataStore(this)

        // Check if onboarding already completed
        lifecycleScope.launch {
            if (prefs.onboardingDone.first()) {
                goToMain()
                return@launch
            }
            setupUI()
        }
    }

    private fun setupUI() {
        setContentView(buildLayout())
    }

    private fun buildLayout(): View {
        // Programmatic layout for simplicity
        val root = android.widget.FrameLayout(this).apply {
            setBackgroundColor(android.graphics.Color.parseColor("#080B12"))
        }

        viewPager = ViewPager2(this).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
            adapter = OnboardingPagerAdapter(pages)
        }

        btnSkip = TextView(this).apply {
            text = "Skip"
            setTextColor(android.graphics.Color.parseColor("#8B9CB8"))
            textSize = 15f
            setPadding(60, 40, 60, 40)
        }

        btnNext = TextView(this).apply {
            text = "NEXT →"
            setTextColor(android.graphics.Color.parseColor("#00F5FF"))
            textSize = 15f
            setPadding(60, 40, 60, 40)
            letterSpacing = 0.1f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        // Bottom navigation bar
        val bottomBar = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            val lp = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
            ).also { it.gravity = android.view.Gravity.BOTTOM; it.bottomMargin = 60 }
            layoutParams = lp
            setPadding(16, 0, 16, 0)
        }

        val spacer = android.widget.Space(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(0, 1, 1f)
        }

        bottomBar.addView(btnSkip)
        bottomBar.addView(spacer)
        bottomBar.addView(btnNext)

        root.addView(viewPager)
        root.addView(bottomBar)

        // ViewPager page change listener
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (position == pages.lastIndex) {
                    btnNext.text = "INITIATE MISSION  ▶"
                    btnSkip.visibility = View.GONE
                } else {
                    btnNext.text = "NEXT →"
                    btnSkip.visibility = View.VISIBLE
                }
            }
        })

        btnSkip.setOnClickListener { finishOnboarding() }
        btnNext.setOnClickListener {
            val current = viewPager.currentItem
            if (current < pages.lastIndex) {
                viewPager.setCurrentItem(current + 1, true)
            } else {
                finishOnboarding()
            }
        }

        return root
    }

    private fun finishOnboarding() {
        lifecycleScope.launch {
            prefs.setOnboardingDone(true)
            goToMain()
        }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// OnboardingPagerAdapter
// ─────────────────────────────────────────────────────────────────────────────
class OnboardingPagerAdapter(private val pages: List<OnboardingPage>) :
    RecyclerView.Adapter<OnboardingPagerAdapter.PageVH>() {

    inner class PageVH(view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageVH {
        val ctx = parent.context

        val root = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setPadding(80, 120, 80, 200)
        }

        val tvEmoji = TextView(ctx).apply {
            textSize = 72f
            gravity = android.view.Gravity.CENTER
        }

        val tvTitle = TextView(ctx).apply {
            textSize = 26f
            setTextColor(android.graphics.Color.parseColor("#F0F4FF"))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = android.view.Gravity.CENTER
            letterSpacing = 0.05f
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = 40 }
        }

        val tvDesc = TextView(ctx).apply {
            textSize = 15f
            setTextColor(android.graphics.Color.parseColor("#8B9CB8"))
            gravity = android.view.Gravity.CENTER
            setLineSpacing(6f, 1.0f)
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = 20 }
        }

        // Neon divider
        val divider = View(ctx).apply {
            setBackgroundColor(android.graphics.Color.parseColor("#00F5FF"))
            layoutParams = android.widget.LinearLayout.LayoutParams(80, 3).also {
                it.topMargin = 30
                it.gravity = android.view.Gravity.CENTER_HORIZONTAL
            }
        }

        root.addView(tvEmoji)
        root.addView(tvTitle)
        root.addView(divider)
        root.addView(tvDesc)

        root.tag = arrayOf(tvEmoji, tvTitle, tvDesc)
        return PageVH(root)
    }

    override fun onBindViewHolder(holder: PageVH, position: Int) {
        val page = pages[position]
        @Suppress("UNCHECKED_CAST")
        val tags = holder.itemView.tag as Array<TextView>
        tags[0].text = page.emoji
        tags[1].text = page.title
        tags[2].text = page.description
    }

    override fun getItemCount() = pages.size
}

// Stub — replace with a real dot indicator view if desired
class LinearDotIndicator(ctx: android.content.Context) : View(ctx)
