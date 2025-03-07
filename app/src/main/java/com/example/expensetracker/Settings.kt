package com.example.expensetracker

import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class Settings : AppCompatActivity() {
    private lateinit var imageView: ImageView
    private lateinit var changePassword: Button
    private lateinit var deleteAccount: Button
    private lateinit var textView: TextView
    private lateinit var spinnerCurrency: Spinner
    private lateinit var btnSave: Button

    private lateinit var auth: FirebaseAuth
    private var user: FirebaseUser? = null
    private lateinit var database: DatabaseReference
    private var email: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        auth = FirebaseAuth.getInstance()
        user = auth.currentUser
        email = user?.email?.replace(".", ",")
        database = FirebaseDatabase.getInstance().reference

        if (user == null) {
            val intent = Intent(applicationContext, Login::class.java)
            startActivity(intent)
            finish()
            return
        }

        textView = findViewById(R.id.textView)
        imageView = findViewById(R.id.btn_back)
        changePassword = findViewById(R.id.btn_change_password)
        deleteAccount = findViewById(R.id.btn_delete_account)
        spinnerCurrency = findViewById(R.id.spinner_currency)
        btnSave = findViewById(R.id.btn_save)

        textView.text = user!!.email

        setupSpinner()
        clickListeners()
    }

    private fun setupSpinner() {
        val currencies = listOf("USD:$", "EUR:€", "GBP:£", "JPY:¥", "INR:₹")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, currencies)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCurrency.adapter = adapter

        // Set the current active currency
        database.child(email!!).child("Currencies").child("active").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val activeCurrency = snapshot.getValue(String::class.java)
                val position = currencies.indexOf(activeCurrency)
                if (position != -1) {
                    spinnerCurrency.setSelection(position)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Failed to read active currency", error.toException())
            }
        })
    }

    private fun clickListeners() {
        imageView.setOnClickListener {
            val intent = Intent(applicationContext, Profile::class.java)
            startActivity(intent)
            finish()
        }

        changePassword.setOnClickListener {
            FirebaseAuth.getInstance().sendPasswordResetEmail(user!!.email.toString())
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(
                            baseContext,
                            "An email was sent to you to change your password",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.d(TAG, "Email sent.")
                    }
                }
        }

        deleteAccount.setOnClickListener {
            showDeleteConfirmationDialog()
        }

        btnSave.setOnClickListener {
            val selectedCurrency = spinnerCurrency.selectedItem.toString()
            updateActiveCurrency(selectedCurrency)
        }
    }

    private fun updateActiveCurrency(newCurrency: String) {
        database.child(email!!).child("Currencies").child("active").setValue(newCurrency)
            .addOnSuccessListener {
                Toast.makeText(this, "Currency updated successfully", Toast.LENGTH_SHORT).show()
                val intent = Intent(applicationContext, Profile::class.java)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error updating currency", e)
                Toast.makeText(this, "Failed to update currency", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Confirm Deletion")
            .setMessage("Are you sure you want to delete your account?")
            .setPositiveButton("Yes") { dialog, _ ->
                showReauthenticationDialog()
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    private fun showReauthenticationDialog() {
        val user = FirebaseAuth.getInstance().currentUser
        val email = user?.email ?: return
        val reauthenticateView = layoutInflater.inflate(R.layout.dialog_reauthenticate, null)
        val passwordInput = reauthenticateView.findViewById<EditText>(R.id.password)

        AlertDialog.Builder(this)
            .setTitle("Re-authentication Required")
            .setMessage("Please enter your password to re-authenticate.")
            .setView(reauthenticateView)
            .setPositiveButton("Confirm") { dialog, _ ->
                val password = passwordInput.text.toString()
                val credential = EmailAuthProvider.getCredential(email, password)
                user.reauthenticate(credential)
                    .addOnCompleteListener { reauthTask ->
                        if (reauthTask.isSuccessful) {
                            deleteUserData()
                        } else {
                            Log.e(TAG, "Re-authentication failed.", reauthTask.exception)
                            Toast.makeText(
                                this,
                                "Re-authentication failed. Please try again.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        dialog.dismiss()
                    }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    private fun deleteUserData() {
        // Delete user data from Realtime Database
        database.child(email!!).removeValue()
            .addOnSuccessListener {
                // After deleting data, delete the user account
                deleteUserAccount()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error deleting user data", e)
                Toast.makeText(this, "Error deleting user data", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteUserAccount() {
        user?.delete()
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val intent = Intent(applicationContext, Login::class.java)
                    startActivity(intent)
                    finish()
                    Log.d(TAG, "User account deleted.")
                } else {
                    Log.e(TAG, "Account deletion failed.", task.exception)
                    Toast.makeText(
                        this,
                        "Account deletion failed. Please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }
}