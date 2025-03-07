package com.example.expensetracker

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.widget.ImageView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Locale

class ViewExpenses : AppCompatActivity() {
    private lateinit var imageView: ImageView
    private lateinit var auth: FirebaseAuth
    private var user: FirebaseUser? = null
    private lateinit var database: DatabaseReference
    private var email: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_view_expenses)
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
        } else {
            getExpenses { expenses ->
                updateExpenses(expenses)
            }
        }

        clickListeners()
    }

    private fun clickListeners() {
        imageView = findViewById(R.id.btn_back)

        imageView.setOnClickListener {
            val intent = Intent(applicationContext, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun getExpenses(callback: (List<Expense>) -> Unit) {
        val expenses = mutableListOf<Expense>()
        database.child(email.toString()).child("expenses").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (expenseSnapshot in snapshot.children) {
                    val name = expenseSnapshot.child("name").getValue(String::class.java) ?: continue
                    val amount = expenseSnapshot.child("amount").getValue(Double::class.java) ?: continue
                    val date = expenseSnapshot.child("date").getValue(String::class.java) ?: continue
                    val category = expenseSnapshot.child("category").getValue(String::class.java) ?: continue

                    expenses.add(Expense(name, amount, date, category))
                }
                callback(expenses)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Error getting expenses", error.toException())
                callback(expenses)
            }
        })
    }

    private fun updateExpenses(expenses: List<Expense>) {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        val sortedExpenses = expenses.sortedByDescending { dateFormat.parse(it.date) }

        val tableLayout = findViewById<TableLayout>(R.id.expenses_table)

        // Create header row
        val headerRow = TableRow(this)
        headerRow.layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT)

        val headerTextViewParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1.0f)

        // Add header cells
        val headers = arrayOf("Name", "Amount", "Date", "Category")
        headers.forEach { headerText ->
            val headerTextView = TextView(this).apply {
                layoutParams = headerTextViewParams
                text = headerText
                setPadding(8, 8, 8, 8)
                setBackgroundResource(R.drawable.header_background)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
            }
            headerRow.addView(headerTextView)
        }

        tableLayout.addView(headerRow)

        // Fetch active currency from Realtime Database
        database.child(email.toString()).child("Currencies").child("active").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currency = snapshot.getValue(String::class.java)?.split(":")?.get(1) ?: ""
                addExpenseRows(tableLayout, sortedExpenses, currency)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error getting currency", error.toException())
                addExpenseRows(tableLayout, sortedExpenses, "")
            }
        })
    }

    @SuppressLint("SetTextI18n")
    private fun addExpenseRows(tableLayout: TableLayout, expenses: List<Expense>, currency: String) {
        expenses.forEach { expense ->
            val row = TableRow(this)
            val rowParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT)
            row.layoutParams = rowParams

            val textViewParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1.0f)

            val nameTextView = TextView(this).apply {
                layoutParams = textViewParams
                text = expense.name
                setPadding(8, 8, 8, 8)
                setBackgroundResource(R.drawable.cell_border)
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                gravity = Gravity.CENTER
            }
            row.addView(nameTextView)

            val amountTextView = TextView(this).apply {
                layoutParams = textViewParams
                text = "${expense.amount} $currency"
                setPadding(8, 8, 8, 8)
                setBackgroundResource(R.drawable.cell_border)
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                gravity = Gravity.CENTER
            }
            row.addView(amountTextView)

            val dateTextView = TextView(this).apply {
                layoutParams = textViewParams
                text = expense.date
                setPadding(8, 8, 8, 8)
                setBackgroundResource(R.drawable.cell_border)
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                gravity = Gravity.CENTER
            }
            row.addView(dateTextView)

            val categoryTextView = TextView(this).apply {
                layoutParams = textViewParams
                text = expense.category
                setPadding(8, 8, 8, 8)
                setBackgroundResource(R.drawable.cell_border)
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                gravity = Gravity.CENTER
            }
            row.addView(categoryTextView)

            tableLayout.addView(row)
        }
    }

    data class Expense(
        val name: String,
        val amount: Double,
        val date: String,
        val category: String,
    )
}