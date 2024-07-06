package com.finwise.finwise_app

import android.os.Bundle
import android.content.Intent
import android.app.Activity
import android.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
class MainActivity3 : AppCompatActivity(),CategoryAdapter.OnItemClickListener  {

    private lateinit var recyclerView : RecyclerView
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main3)

        auth = FirebaseAuth.getInstance()

        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = calculateGridLayoutManager()
        categoryAdapter = CategoryAdapter(this, getCategories().toMutableList(), this, auth)
        recyclerView.adapter = categoryAdapter
    }

    private fun getCategories(): List<Category> {
        val categoryData = listOf(
            "Food" to R.drawable.food_icon,
            "Fuel" to R.drawable.fuel_icon,
            "Electric" to R.drawable.electric,
            "Stationary" to R.drawable.sationary,
            "Gadget" to R.drawable.gadget,
            "Shopping" to R.drawable.shop,
            "Grocery" to R.drawable.gros_icon,
            "Fees" to R.drawable.fees,
            "Rent" to R.drawable.rent_icon,
            "Investment" to R.drawable.invest_icon,
            "Entertainment" to R.drawable.entertainment,
            "Pet" to R.drawable.pet_icon,
            "Fitness" to R.drawable.fitness,
            "Medical" to R.drawable.medical,
            "Loan" to R.drawable.loan_icon,
            "Appliance" to R.drawable.home_appliance,
            "+ Add" to R.drawable.coin
        )

        return categoryData.map { (name, iconResource) ->
            if (name == "+ Add") {
                Category(name, iconResource, "") // For the "+ Add" category, provide an empty userId
            } else {
                Category(name, iconResource, auth.currentUser?.uid ?: "")
            }
        }
    }

    private fun calculateGridLayoutManager(): GridLayoutManager {
        val numberOfColumns = calculateNumberOfColumns()
        return GridLayoutManager(this, numberOfColumns)
    }

    private fun calculateNumberOfColumns(): Int {
        val totalCategories = getCategories().size
        val desiredRows = 4 // Desired number of rows
        val itemsPerRow = totalCategories / desiredRows
        return if (itemsPerRow == 0) {
            1 // Ensure at least one column if there are fewer categories than desiredRows
        } else {
            itemsPerRow
        }
    }
    override fun onItemClick(category: Category) {
        if (category.name == "+ Add") {
            // Do nothing when the "+ Add" category is clicked since the dialog will be shown from the adapter
        } else if (categoryHasSubcategories(category.name)) {
            showSubcategoryDialog(category)
        } else {
            startExpenseLogActivity(category)
        }
    }

    private fun showSubcategoryDialog(category: Category) {
        val subcategories = getSubcategories(category.name)

        val dialogView = layoutInflater.inflate(R.layout.activity_subcat, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerViewSubcategories)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val subcategoryAdapter = SubcatAdapter(subcategories) { selectedSubcategory ->
            startExpenseLogActivity(Category(selectedSubcategory, category.imageResource, category.userId))
        }
        recyclerView.adapter = subcategoryAdapter

        val alertDialog = AlertDialog.Builder(this)
            .setTitle("Select Subcategory")
            .setView(dialogView)
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        alertDialog.show()
    }

    private fun getSubcategories(mainCategoryName: String?): List<String> {
        return when (mainCategoryName) {
            "Grocery" -> listOf(
                "Vegetables","Fruits","Dairy Products", "Canned Goods", "Snacks", "Beverages",
                "Condiments", "Frozen Foods", "Bakery Items", "Cereals",
                "Spices", "Pasta", "Fresh Produce", "Sauces", "Masala","DosaBatter","Meat"
            )
            "Food" -> listOf(
                "Lunch","Dinner","Breakfast","Brunch","Dessert","Junk"
            )
            "Shopping" -> listOf("Clothes","Toys","KitchenUtensils","Decoration")
            "Gadget" -> listOf("Mobile","Tablet","SmartWatch","Laptop")
            "Appliances" -> listOf("Refrigerator", "Microwave", "Washing Machine", "Dishwasher", "Blender")
            "Stationary" -> listOf("Notebooks", "Pens", "Pencils", "Erasers", "Markers","Colors")
            "Entertainment" -> listOf("Movies", "Books", "Music", "Video Games","Sports")
            else -> emptyList()
        }
    }

    private fun categoryHasSubcategories(categoryName: String): Boolean {
        // Define categories that have subcategories
        val categoriesWithSubcategories = setOf(
            "Food","Grocery","Appliance","Stationary","Entertainment","Shopping","Gadget"
        )
        return categoryName in categoriesWithSubcategories
    }

    private fun startExpenseLogActivity(category: Category) {
        val intent = Intent(this, expense_log::class.java)
        intent.putExtra("selectedCategoryImage", category.imageResource)
        intent.putExtra("selectedCategoryName", category.name)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    companion object {
        const val REQUEST_SUBCATEGORY = 1
        const val REQUEST_EXPENSE_LOG = 2
    }




}


