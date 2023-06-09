package com.hygeia

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.FragmentContainerView
import androidx.navigation.ui.setupWithNavController
import com.hygeia.databinding.ActMainBinding
import com.hygeia.objects.UserManager
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController

class ActMain : AppCompatActivity() {
    private lateinit var bind : ActMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActMainBinding.inflate(layoutInflater)
        setContentView(bind.root)

        with(bind) {

            val navHostFragment = supportFragmentManager.findFragmentById(R.id.containerMain) as NavHostFragment
            val navController = navHostFragment.navController

            when (UserManager.role) {
                "super admin" -> {
                    actMainBotNavigation.inflateMenu(R.menu.menu_for_super)
                    navHostFragment.navController.graph = navController.navInflater.inflate(R.navigation.nav_for_super)
                }
                "admin" -> {
                    actMainBotNavigation.inflateMenu(R.menu.menu_for_admin)
                    navHostFragment.navController.graph = navController.navInflater.inflate(R.navigation.nav_for_admin)
                }
                "standard" -> {
                    actMainBotNavigation.inflateMenu(R.menu.menu_for_standard)
                    navHostFragment.navController.graph = navController.navInflater.inflate(R.navigation.nav_for_standard)
                }
            }
            actMainBotNavigation.setupWithNavController(
                containerMain.getFragment<NavHostFragment>().navController
            )
        }
    }
}