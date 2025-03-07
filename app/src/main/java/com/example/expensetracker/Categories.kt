package com.example.expensetracker

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class Categories : AppCompatActivity() {
    private lateinit var imageView: ImageView
    private lateinit var btnAddCategory: FloatingActionButton
    private lateinit var inputCategory: EditText
    private lateinit var auth: FirebaseAuth
    private var user: FirebaseUser? = null
    private lateinit var database: DatabaseReference
    private var email: String? = null

    private val categories = mutableListOf<String>()
    private lateinit var adapter: CategoriesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_categories)
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
        }

        setupRecyclerView()
        clickListeners()
        updateCategories()
    }

    private fun setupRecyclerView() {
        adapter = CategoriesAdapter(categories)
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view_categories)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun clickListeners() {
        imageView = findViewById(R.id.btn_back)
        btnAddCategory = findViewById(R.id.fab_add_category)
        inputCategory = findViewById(R.id.input_new_category)

        imageView.setOnClickListener {
            val intent = Intent(applicationContext, Profile::class.java)
            startActivity(intent)
            finish()
        }

        checkIfUserIsPremium(email!!) { isPremium ->
            if (isPremium) {
                btnAddCategory.setOnClickListener {
                    createNewCategory()
                }
            } else {
                btnAddCategory.setOnClickListener {
                    showPremiumOnlyMessage()
                }
            }
        }
    }

    private fun createNewCategory() {
        val newCategory = inputCategory.text.toString().trim()
        if (newCategory.isNotBlank()) {
            database.child(email!!).child("Categories").child(newCategory).setValue(newCategory)
                .addOnSuccessListener {
                    categories.add(newCategory)
                    adapter.notifyItemInserted(categories.size - 1)
                    inputCategory.text.clear()
                }
                .addOnFailureListener { exception ->
                    Log.d(TAG, "Error adding category", exception)
                }
        } else {
            Toast.makeText(baseContext, "Write the name of the new category", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateCategories() {
        database.child(email.toString()).child("Categories").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                categories.clear()
                for (categorySnapshot in snapshot.children) {
                    categorySnapshot.getValue(String::class.java)?.let {
                        categories.add(it)
                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d(TAG, "Error getting categories", error.toException())
            }
        })
    }

    private fun showPremiumOnlyMessage() {
        Snackbar.make(findViewById(R.id.main), "This functionality is only for premium users.", Snackbar.LENGTH_LONG).show()
    }

    private fun checkIfUserIsPremium(email: String, callback: (Boolean) -> Unit) {
        database.child(email).child("Plan").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isPremium = snapshot.child("premium").getValue(Boolean::class.java) ?: false
                callback(isPremium)
            }

            override fun onCancelled(error: DatabaseError) {
                callback(false)
            }
        })
    }

    class CategoriesAdapter(private val categories: List<String>) : RecyclerView.Adapter<CategoriesAdapter.CategoryViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
            return CategoryViewHolder(view as TextView)
        }

        override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
            holder.textView.text = categories[position]
        }

        override fun getItemCount() = categories.size

        class CategoryViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)
    }
}