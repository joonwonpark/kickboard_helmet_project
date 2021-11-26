package com.aiden.tflite.example

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aiden.tflite.example.databinding.ActivityDrawBinding
import java.io.IOException
import java.util.*

class DrawActivity : AppCompatActivity() {
    private val binding by lazy { ActivityDrawBinding.inflate(layoutInflater, null, false) }
    private lateinit var classifier: Classifier

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initCanvas()
        initClassifier()
    }

    override fun onDestroy() {
        if (::classifier.isInitialized) classifier.finish()
        super.onDestroy()
    }

    private fun initCanvas() {
        binding.run {
            drawView.run {
                setStrokeWidth(100.0f)
                setBackgroundColor(Color.BLACK)
                setColor(Color.WHITE)
            }
            btnClassify.setOnClickListener {
                val image = drawView.getBitmap()
                val result = classifier.classify(image)
                val outString = String.format(Locale.ENGLISH, "%d, %.0f%%", result.first, result.second * 100.0f)
                textResult.text = outString
            }
            btnClear.setOnClickListener {
                drawView.clearCanvas()
                textResult.text = ""
            }
        }
    }

    private fun initClassifier() {
        classifier = Classifier(assets, Classifier.DIGIT_CLASSIFIER_V2)
        try {
            classifier.init()
        } catch (exception: IOException) {
            Toast.makeText(this@DrawActivity, "classifier IOException", Toast.LENGTH_SHORT).show()
        }
    }
}