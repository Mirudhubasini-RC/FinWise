package com.finwise.finwise_app


import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.fragment.app.FragmentManager


class Navigation : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_view)

        // Set the default fragment to be loaded
        loadFragment(ExpenseActivity(), supportFragmentManager)

        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(ExpenseActivity(), supportFragmentManager)
                    true
                }

                R.id.nav_graph -> {
                    loadFragment(Pie(), supportFragmentManager)
                    true
                }

               R.id.nav_remainder -> {
                   loadFragment(reminder_log(), supportFragmentManager)
                    true
                }

                R.id.nav_history -> {
                    loadFragment(History(),supportFragmentManager)
                    true
                }

                R.id.nav_profile -> {
                    loadFragment(profile(), supportFragmentManager)
                    true
                }

                else -> false
            }
        }



    }
    private fun loadFragment(fragment: Fragment, fragmentManager: FragmentManager) {
        fragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

}