package com.example.dormta

import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class StudentDashboardActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var rvPopular: RecyclerView
    private lateinit var rvAllListings: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var tvStudentName: TextView
    private lateinit var tvListingCount: TextView
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var btnLogout: ImageView

    // All dorms fetched from Firestore
    private val masterList = mutableListOf<Dorm>()
    // Filtered list shown in rvAllListings
    private val filteredList = mutableListOf<Dorm>()
    private val popularList = mutableListOf<Dorm>()

    private lateinit var popularAdapter: DormAdapter
    private lateinit var allListingsAdapter: DormAdapter

    // Active filters
    private var activeSearchQuery = ""
    private var activeChipFilter = "All"   // All | Bedspace | Studio | Aircon | Budget

    private val TARGET_SCHOOL = "Southwestern University PHINMA"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.student_dashboard)

        db   = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        bindViews()
        loadUserName()
        setupRecyclerViews()
        setupSearch()
        setupChips()
        setupClickListeners()
        fetchDorms()
    }

    private fun bindViews() {
        rvPopular        = findViewById(R.id.rvPopular)
        rvAllListings    = findViewById(R.id.rvAllListings)
        etSearch         = findViewById(R.id.etSearch)
        tvStudentName    = findViewById(R.id.tvStudentName)
        tvListingCount   = findViewById(R.id.tvListingCount)
        layoutEmptyState = findViewById(R.id.layoutEmptyState)
        btnLogout        = findViewById(R.id.btnLogout)
    }

    // ── Load user's first name from Firestore ──────────────────────────────
    private fun loadUserName() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("name") ?: ""
                val firstName = name.split(" ").firstOrNull() ?: "Student"
                tvStudentName.text = "Hi, $firstName 👋"
            }
    }

    private fun setupRecyclerViews() {
        popularAdapter = DormAdapter(popularList, isHorizontal = true)
        rvPopular.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvPopular.adapter = popularAdapter

        allListingsAdapter = DormAdapter(filteredList, isHorizontal = false)
        rvAllListings.layoutManager = LinearLayoutManager(this)
        rvAllListings.adapter = allListingsAdapter
    }

    // ── Real-time search  ──────────────────────────────────────────────────
    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                activeSearchQuery = s?.toString()?.trim() ?: ""
                applyFilters()
            }
        })
    }

    // ── Filter chips ───────────────────────────────────────────────────────
    private fun setupChips() {
        val chips = mapOf(
            R.id.chipAll      to "All",
            R.id.chipBedspace to "Bedspace",
            R.id.chipStudio   to "Studio",
            R.id.chipAircon   to "Aircon",
            R.id.chipBudget   to "Budget"
        )
        chips.forEach { (id, label) ->
            val chip = findViewById<TextView>(id)
            chip.setOnClickListener {
                activeChipFilter = label
                updateChipUI(chips, id)
                applyFilters()
            }
        }
    }

    private fun updateChipUI(chips: Map<Int, String>, selectedId: Int) {
        chips.keys.forEach { id ->
            val chip = findViewById<TextView>(id)
            if (id == selectedId) {
                chip.setBackgroundResource(R.drawable.chip_bg_selected)
                chip.setTextColor(getColor(android.R.color.white))
            } else {
                chip.setBackgroundResource(R.drawable.chip_bg_unselected)
                chip.setTextColor(getColor(R.color.textGray))
            }
        }
    }

    // ── Apply search + chip filters ────────────────────────────────────────
    private fun applyFilters() {
        val query = activeSearchQuery.lowercase()
        filteredList.clear()

        masterList.forEach { dorm ->
            // Chip filter
            val passChip = when (activeChipFilter) {
                "All"      -> true
                "Bedspace" -> dorm.room_type.contains("bedspace", ignoreCase = true)
                "Studio"   -> dorm.room_type.contains("studio", ignoreCase = true)
                "Aircon"   -> dorm.amenities?.any { it.contains("aircon", ignoreCase = true) } == true ||
                              dorm.options.contains("aircon", ignoreCase = true)
                "Budget"   -> dorm.price > 0 && dorm.price <= 3500
                else       -> true
            }

            // Search filter
            val passSearch = query.isEmpty() ||
                dorm.displayName().lowercase().contains(query) ||
                dorm.address.lowercase().contains(query) ||
                dorm.school.lowercase().contains(query) ||
                dorm.room_type.lowercase().contains(query)

            if (passChip && passSearch) filteredList.add(dorm)
        }

        allListingsAdapter.notifyDataSetChanged()
        tvListingCount.text = "${filteredList.size} found"
        layoutEmptyState.visibility = if (filteredList.isEmpty()) View.VISIBLE else View.GONE
    }

    // ── Header buttons ─────────────────────────────────────────────────────
    private fun setupClickListeners() {
        // Logout directly from header
        btnLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Log Out") { _, _ ->
                    auth.signOut()
                    Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, RoleActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // See all popular
        findViewById<TextView>(R.id.tvSeeAllPopular)?.setOnClickListener {
            etSearch.setText("")
            activeChipFilter = "All"
            updateChipUI(mapOf(
                R.id.chipAll to "All", R.id.chipBedspace to "Bedspace",
                R.id.chipStudio to "Studio", R.id.chipAircon to "Aircon",
                R.id.chipBudget to "Budget"
            ), R.id.chipAll)
            applyFilters()
        }
    }

    // ── Firestore fetch ────────────────────────────────────────────────────
    private fun fetchDorms() {
        db.collection("dorms")
            .whereEqualTo("school", TARGET_SCHOOL)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    fetchAllAndFilter()
                    return@addSnapshotListener
                }
                masterList.clear()
                value?.documents?.forEach { doc ->
                    val dorm = doc.toObject(Dorm::class.java)?.copy(id = doc.id)
                    dorm?.let { masterList.add(it) }
                }
                if (masterList.isEmpty()) { fetchAllAndFilter(); return@addSnapshotListener }
                updatePopular()
                applyFilters()
            }
    }

    private fun fetchAllAndFilter() {
        db.collection("dorms")
            .addSnapshotListener { value, error ->
                if (error != null) return@addSnapshotListener
                masterList.clear()
                value?.documents?.forEach { doc ->
                    val dorm = doc.toObject(Dorm::class.java)?.copy(id = doc.id)
                    dorm?.let {
                        val swu = it.school.contains("Southwestern", ignoreCase = true) ||
                                  it.school.contains("PHINMA", ignoreCase = true) ||
                                  it.school.contains("SWU", ignoreCase = true)
                        val noSchool = it.school.isEmpty()
                        if (swu || noSchool) masterList.add(it)
                    }
                }
                updatePopular()
                applyFilters()
            }
    }

    private fun updatePopular() {
        popularList.clear()
        popularList.addAll(
            masterList.filter { it.bed_space_available > 0 }.take(5)
                .ifEmpty { masterList.take(5) }
        )
        popularAdapter.notifyDataSetChanged()
    }
}
