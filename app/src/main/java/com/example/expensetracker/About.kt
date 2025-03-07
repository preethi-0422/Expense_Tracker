package com.example.expensetracker

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class About : AppCompatActivity(){
    private lateinit var imageView: ImageView
    private lateinit var textView: TextView
    private lateinit var auth: FirebaseAuth
    private var user: FirebaseUser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_about)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        user = auth.currentUser
        textView = findViewById(R.id.user_email)

        if (user == null) {
            val intent = Intent(applicationContext, Login::class.java)
            startActivity(intent)
            finish()
        } else {
            textView.text = user!!.email
        }

        clickListeners()
    }

    private fun clickListeners(){
        imageView = findViewById(R.id.btn_back)

        // Go back to the Profile activity
        imageView.setOnClickListener{
            val intent = Intent(applicationContext, Profile::class.java)
            startActivity(intent)
            finish()
        }
    }
}