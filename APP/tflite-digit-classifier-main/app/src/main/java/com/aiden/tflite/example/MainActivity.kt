package com.aiden.tflite.example

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.aiden.tflite.example.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater, null, false) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.btnDrawView.setOnClickListener {
            val intent = Intent(this@MainActivity, DrawActivity::class.java)
            startActivity(intent)
        }
    }
}