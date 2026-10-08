package com.weibolite.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.weibolite.android.ui.MainViewModel
import com.weibolite.android.ui.WeiboApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val vm = ViewModelProvider(this)[MainViewModel::class.java]
        setContent { WeiboApp(vm) }
        vm.visitor.start(this, lifecycleScope)
    }
}
