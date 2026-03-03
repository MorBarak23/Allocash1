package com.mor.allocash1.data.cloud

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mor.allocash1.data.local.UserData

// Central manager for Firebase operations. Uses Email as Document ID for O(1) complexity.
object FireStoreManager {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    // Variables to track listeners and prevent memory leaks
    private var userProfileListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var actionsListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var transactionsListener: com.google.firebase.firestore.ListenerRegistration? = null

    // Volatile flag to skip biometric check right after manual login
    var isManualLoginSession: Boolean = false

    // Authenticates user and sets session flag on success.
    fun loginUser(email: String, pass: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener {
                isManualLoginSession = true
                onSuccess()
            }
            .addOnFailureListener { onFailure(it.message ?: "Login failed") }
    }

    // Creates Auth account and then a Firestore profile document.
    fun registerUser(email: String, pass: String, name: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnSuccessListener {
                saveUserProfile(email, name, onSuccess, onFailure)
            }
            .addOnFailureListener { onFailure(it.message ?: "Registration failed") }
    }

    // Saves the user profile by using email address as the unique Document ID.
    private fun saveUserProfile(email: String, name: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val userMap = hashMapOf(
            "name" to name,
            "email" to email,
            "phone" to "-",
            "country" to "Israel",
            "currencySymbol" to "₪",
            "currencyCode" to "ILS",
            "monthlySavingsGoal" to 500.0, // Default goal set during signup
            "isDarkMode" to false,
            "isBiometricEnabled" to false,
            "familyId" to null
        )
        db.collection("users").document(email).set(userMap)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it.message ?: "Profile data error") }
    }

    // Sends a verification link to the currently signed-in user's email.
    fun sendEmailVerification(onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        auth.currentUser?.sendEmailVerification()
            ?.addOnSuccessListener { onSuccess() }
            ?.addOnFailureListener { onFailure(it.message ?: "Verification email failed") }
    }

    // Sends a password reset link to the specified email address.
    fun sendPasswordReset(email: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it.message ?: "Reset email failed") }
    }

    // Returns true only if the user is logged in AND has clicked the verification link.
    fun isUserFullyVerified(): Boolean {
        val user = auth.currentUser
        user?.reload() // Refresh user state to get latest verification status
        return user != null && user.isEmailVerified
    }

    // Fetches full user profile from Firestore and updates local UserData state.
    fun fetchAndSyncUserProfile(onComplete: () -> Unit) {
        val email = auth.currentUser?.email ?: return

        db.collection("users").document(email).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Syncing basic profile data
                    UserData.name = document.getString("name") ?: "User Name"
                    UserData.email = document.getString("email") ?: email

                    // Syncing extended profile data
                    UserData.phone = document.getString("phone") ?: "-"
                    UserData.country = document.getString("country") ?: "Israel"

                    // Syncing financial preferences
                    UserData.currencySymbol = document.getString("currencySymbol") ?: "₪"
                    UserData.currencyCode = document.getString("currencyCode") ?: "ILS"

                    UserData.monthlySavingsGoal = document.getDouble("monthlySavingsGoal") ?: 500.0

                    onComplete()
                } else {
                    onComplete()
                }
            }
            .addOnFailureListener { onComplete() }
    }

    // Initializes a new financial category in the cloud while automatically
    // associating it with the user's specific family group ID for shared access.
    fun saveFinancialAction(title: String, amount: Double, category: String, isExpense: Boolean, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val email = auth.currentUser?.email ?: return

        db.collection("users").document(email).get().addOnSuccessListener { userDoc ->
            val familyId = userDoc.getString("familyId") ?: email
            val timestamp = System.currentTimeMillis()

            // Creates a primary budget or income document in the "actions" collection
            val actionData = hashMapOf(
                "title" to title,
                "amount" to amount,
                "category" to category,
                "isExpense" to isExpense,
                "currentAmount" to if (isExpense) 0.0 else amount, // Income starts with full amount
                "timestamp" to timestamp,
                "userEmail" to email,
                "familyId" to familyId
            )

            db.collection("actions").add(actionData).addOnSuccessListener {
                // If it's an income, we also record it as a transaction for history / Recent Actions
                if (!isExpense) {
                    val transactionMap = hashMapOf(
                        "title" to "Initial $title",
                        "amount" to amount,
                        "category" to category,
                        "isExpense" to false,
                        "parentAction" to title,
                        "userEmail" to email,
                        "familyId" to familyId,
                        "timestamp" to timestamp
                    )
                    db.collection("transactions").add(transactionMap)
                }
                onSuccess()
            }.addOnFailureListener { onFailure(it.message ?: "Sync failed") }
        }
    }

    //Links another user to the current user's family group.
    fun inviteUserToFamily(targetEmail: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val currentUserEmail = auth.currentUser?.email ?: return
        db.collection("users").document(currentUserEmail).get().addOnSuccessListener { document ->
            val familyId = document.getString("familyId") ?: currentUserEmail
            val inviterName = document.getString("name") ?: "Family Member"

            val inviteData = hashMapOf(
                "fromEmail" to currentUserEmail,
                "fromName" to inviterName,
                "toEmail" to targetEmail,
                "familyId" to familyId,
                "currencySymbol" to (document.getString("currencySymbol") ?: "₪"),
                "currencyCode" to (document.getString("currencyCode") ?: "ILS"),
                "monthlySavingsGoal" to (document.getDouble("monthlySavingsGoal") ?: 500.0), // Goal included in invite
                "status" to "pending"
            )
            db.collection("invites").document(targetEmail).set(inviteData).addOnSuccessListener { onSuccess() }
        }
    }

    // Listens for incoming invites for the current user
    fun listenForInvites(onInviteReceived: (Map<String, Any>) -> Unit) {
        val email = auth.currentUser?.email ?: return
        db.collection("invites").document(email).addSnapshotListener { snapshot, _ ->
            if (snapshot != null && snapshot.exists() && snapshot.getString("status") == "pending") {
                onInviteReceived(snapshot.data!!)
            }
        }
    }

    // Updates user's familyId, monthly saving goal and currency to match the family group
    fun acceptFamilyInvite(inviteData: Map<String, Any>, onComplete: () -> Unit) {
        val email = auth.currentUser?.email ?: return
        val updates = mapOf(
            "familyId" to inviteData["familyId"],
            "currencySymbol" to inviteData["currencySymbol"],
            "currencyCode" to inviteData["currencyCode"],
            "monthlySavingsGoal" to (inviteData["monthlySavingsGoal"] as? Number)?.toDouble() // Syncing the shared goal
        )
        db.collection("users").document(email).update(updates).addOnSuccessListener {
            db.collection("invites").document(email).delete()
            onComplete()
        }
    }

    /**
     * Listens for real-time changes for the current month.
     * Automatically filters by familyId or individual email.
     */
    fun listenToAllActions(onUpdate: (List<com.mor.allocash1.data.classes.Action>) -> Unit) {
        val email = auth.currentUser?.email ?: return
        //Use cached familyId if available, otherwise fetch once
        val familyId = UserData.familyId ?: email

        actionsListener?.remove() // Close previous before opening new
        actionsListener = db.collection("actions")
            .whereEqualTo("familyId", familyId)
            .addSnapshotListener { snapshots, _ ->
                val actions = snapshots?.map { doc ->
                    val actionTimestamp = doc.getLong("timestamp") ?: 0L
                    val action = com.mor.allocash1.data.classes.Action(
                        title = doc.getString("title") ?: "",
                        limit = doc.getDouble("amount") ?: 0.0,
                        currentAmount = doc.getDouble("currentAmount") ?: 0.0,
                        category = mapCategory(doc.getString("category"))
                    )
                    if (isFromPreviousMonth(actionTimestamp)) resetActionForNewMonth(doc.id)
                    action
                } ?: emptyList()
                onUpdate(actions)
            }
    }

    // Helper to check if a timestamp belongs to a month before the current one
    private fun isFromPreviousMonth(timestamp: Long): Boolean {
        if (timestamp == 0L) return false
        val actionDate = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
        val now = java.util.Calendar.getInstance()

        return actionDate.get(java.util.Calendar.MONTH) != now.get(java.util.Calendar.MONTH) ||
                actionDate.get(java.util.Calendar.YEAR) != now.get(java.util.Calendar.YEAR)
    }

    // Resets the currentAmount to 0 and updates the timestamp to the current month
    private fun resetActionForNewMonth(documentId: String) {
        db.collection("actions").document(documentId).update(
            mapOf(
                "currentAmount" to 0.0,
                "timestamp" to System.currentTimeMillis()
            )
        )
    }

    // Establishes a real-time listener for the family's transaction history and triggers a callback to synchronize data changes.
    fun listenToTransactions(onUpdate: (List<com.mor.allocash1.data.classes.Transaction>) -> Unit) {
        val email = auth.currentUser?.email ?: return
        val familyId = UserData.familyId ?: email

        transactionsListener?.remove()
        transactionsListener = db.collection("transactions")
            .whereEqualTo("familyId", familyId)
            .addSnapshotListener { snapshots, _ ->
                val transactions = snapshots?.map { doc ->
                    com.mor.allocash1.data.classes.Transaction(
                        title = doc.getString("title") ?: "",
                        amount = doc.getDouble("amount") ?: 0.0,
                        category = mapCategory(doc.getString("category")),
                        isExpense = doc.getBoolean("isExpense") ?: true,
                        timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                    )
                } ?: emptyList()
                onUpdate(transactions)
            }
    }

    // Monitors real-time user profile updates to synchronize local state and trigger a UI refresh callback.
    fun listenToUserProfile(onUpdate: () -> Unit) {
        val email = auth.currentUser?.email ?: return
        userProfileListener?.remove()
        userProfileListener = db.collection("users").document(email).addSnapshotListener { snapshot, _ ->
            if (snapshot != null && snapshot.exists()) {
                UserData.name = snapshot.getString("name") ?: UserData.name
                UserData.monthlySavingsGoal = snapshot.getDouble("monthlySavingsGoal") ?: 500.0
                UserData.currencySymbol = snapshot.getString("currencySymbol") ?: "₪"
                UserData.currencyCode = snapshot.getString("currencyCode") ?: "ILS"
                UserData.familyId = snapshot.getString("familyId")
                onUpdate()
            }
        }
    }

    // Function to stop all the listeners while not in Home Fragment
    fun stopAllListeners() {
        userProfileListener?.remove()
        actionsListener?.remove()
        transactionsListener?.remove()
        userProfileListener = null
        actionsListener = null
        transactionsListener = null
    }

    //Fetches historical data for a specific month and year (History Fragment).
    fun getActionsByPeriod(month: Int, year: Int, onSuccess: (List<com.mor.allocash1.data.classes.Transaction>) -> Unit, onFailure: (String) -> Unit) {
        val email = auth.currentUser?.email ?: return

        db.collection("users").document(email).get().addOnSuccessListener { userDoc ->
            val familyId = userDoc.getString("familyId") ?: email
            val calendar = java.util.Calendar.getInstance()
            calendar.set(year, month, 1, 0, 0, 0)
            val start = calendar.timeInMillis
            calendar.set(java.util.Calendar.DAY_OF_MONTH, calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
            val end = calendar.timeInMillis

            db.collection("transactions")
                .whereEqualTo("familyId", familyId)
                .whereGreaterThanOrEqualTo("timestamp", start)
                .whereLessThanOrEqualTo("timestamp", end)
                .get()
                .addOnSuccessListener { result ->
                    val transactions = result.map { doc ->
                        com.mor.allocash1.data.classes.Transaction(
                            title = doc.getString("title") ?: "",
                            amount = doc.getDouble("amount") ?: 0.0,
                            category = mapCategory(doc.getString("category")), // Using safe mapping
                            isExpense = doc.getBoolean("isExpense") ?: true,
                            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                        )
                    }
                    onSuccess(transactions)
                }
                .addOnFailureListener { onFailure(it.message ?: "Fetch failed") }
        }
    }

    //Fetches transactions instead of budget actions for Recent Actions screen.
    fun getTransactions(onSuccess: (List<Map<String, Any>>) -> Unit, onFailure: (String) -> Unit) {
        val email = auth.currentUser?.email ?: return

        db.collection("users").document(email).get().addOnSuccessListener { userDoc ->
            val familyId = userDoc.getString("familyId") ?: email

            db.collection("transactions")
                .whereEqualTo("familyId", familyId) // Filter by group ID instead of email
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener { result ->
                    val transactions = result.map { it.data }
                    onSuccess(transactions)
                }
                .addOnFailureListener { onFailure(it.message ?: "Fetch failed") }
        }
    }

    //Updates specific fields in the user's cloud profile document.
    fun updateUserProfile(updates: Map<String, Any>, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val email = auth.currentUser?.email ?: return

        db.collection("users").document(email)
            .update(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it.message ?: "Update failed") }
    }

    fun updateMonthlySavingsGoal(newGoal: Double, onSuccess: () -> Unit) {
        val email = auth.currentUser?.email ?: return
        db.collection("users").document(email).update("monthlySavingsGoal", newGoal)
            .addOnSuccessListener { onSuccess() }
    }

    // Renames an action by finding it by title and family group
    fun updateActionName(oldTitle: String, newTitle: String, onComplete: () -> Unit) {
        val email = auth.currentUser?.email ?: return
        db.collection("actions")
            .whereEqualTo("userEmail", email)
            .whereEqualTo("title", oldTitle)
            .get()
            .addOnSuccessListener { snapshots ->
                for (doc in snapshots) {
                    db.collection("actions").document(doc.id).update("title", newTitle)
                }
                onComplete()
            }
    }

    // Updates the planned monthly budget (limit) for a specific action
    fun updateActionLimit(title: String, newLimit: Double, onComplete: () -> Unit) {
        val email = auth.currentUser?.email ?: return
        db.collection("actions")
            .whereEqualTo("userEmail", email)
            .whereEqualTo("title", title)
            .get()
            .addOnSuccessListener { snapshots ->
                for (doc in snapshots) {
                    // We update only the budget limit, keeping the current spending intact
                    db.collection("actions").document(doc.id).update("amount", newLimit)
                }
                onComplete()
            }
    }

    // Deletes an action document from the cloud
    fun deleteAction(title: String, onComplete: () -> Unit) {
        val email = auth.currentUser?.email ?: return
        db.collection("actions")
            .whereEqualTo("userEmail", email)
            .whereEqualTo("title", title)
            .get()
            .addOnSuccessListener { snapshots ->
                for (doc in snapshots) {
                    db.collection("actions").document(doc.id).delete()
                }
                onComplete()
            }
    }

    // Records a new transaction in the history log and updates the balance of its parent budget category.
    fun addTransactionAndUpdateAction(actionTitle: String, transactionTitle: String, amount: Double, category: String, isExpense: Boolean, onComplete: () -> Unit) {
        val email = auth.currentUser?.email ?: return

        // Creates a detailed transaction entry in the cloud, capturing the amount, title, and timestamp for historical tracking.
        db.collection("users").document(email).get().addOnSuccessListener { userDoc ->
            val familyId = userDoc.getString("familyId") ?: email

            val transactionMap = hashMapOf(
                "title" to transactionTitle,
                "amount" to amount,
                "category" to category,
                "isExpense" to isExpense,
                "parentAction" to actionTitle,
                "userEmail" to email,
                "familyId" to familyId,
                "timestamp" to System.currentTimeMillis()
            )
            db.collection("transactions").add(transactionMap)

            // Sync with the parent budget document
            db.collection("actions")
                .whereEqualTo("familyId", familyId)
                .whereEqualTo("title", actionTitle)
                .get()
                .addOnSuccessListener { snapshots ->
                    for (doc in snapshots) {
                        val current = doc.getDouble("currentAmount") ?: 0.0
                        db.collection("actions").document(doc.id).update("currentAmount", current + amount)
                    }
                    onComplete()
                }
        }
    }

    //Deletes a transaction from cloud and recalculates the action's current amount.
    fun deleteTransaction(transaction: com.mor.allocash1.data.classes.Transaction, onComplete: () -> Unit) {
        val email = auth.currentUser?.email ?: return

        // Find the specific transaction document based on user and timestamp
        db.collection("transactions")
            .whereEqualTo("userEmail", email)
            .whereEqualTo("timestamp", transaction.timestamp)
            .get()
            .addOnSuccessListener { snapshots ->
                for (doc in snapshots) {
                    val parentActionTitle = doc.getString("parentAction")
                    val amount = doc.getDouble("amount") ?: 0.0

                    // Delete the transaction record
                    db.collection("transactions").document(doc.id).delete()

                    // Adjust the parent action's balance in the cloud
                    if (parentActionTitle != null) {
                        db.collection("actions")
                            .whereEqualTo("userEmail", email)
                            .whereEqualTo("title", parentActionTitle)
                            .get()
                            .addOnSuccessListener { actionSnapshots ->
                                for (actionDoc in actionSnapshots) {
                                    val current = actionDoc.getDouble("currentAmount") ?: 0.0
                                    db.collection("actions").document(actionDoc.id).update("currentAmount", current - amount)
                                }
                                onComplete()
                            }
                    } else {
                        onComplete()
                    }
                }
            }
    }

    // Fetches all users who belong to the same family group as the current user.
    fun fetchFamilyMembers(onComplete: (List<com.mor.allocash1.data.local.FamilyMember>) -> Unit) {
        val email = auth.currentUser?.email ?: return

        db.collection("users").document(email).get().addOnSuccessListener { userDoc ->
            val familyId = userDoc.getString("familyId") ?: email

            db.collection("users")
                .whereEqualTo("familyId", familyId)
                .get()
                .addOnSuccessListener { result ->
                    val members = result.documents
                        .filter { it.id != email } // Filter out "Me" from the cloud list
                        .map { doc ->
                            com.mor.allocash1.data.local.FamilyMember(
                                name = doc.getString("name") ?: "User",
                                email = doc.getString("email") ?: ""
                            )
                        }
                    onComplete(members)
                }
        }
    }

    // Helper function to safely map string categories from Firestore to Enum
    private fun mapCategory(name: String?): com.mor.allocash1.data.classes.CategoryType {
        val cleanName = name?.trim() ?: ""
        return com.mor.allocash1.data.classes.CategoryType.values().find {
            it.displayName.equals(cleanName, ignoreCase = true) || it.name.equals(cleanName, ignoreCase = true)
        } ?: com.mor.allocash1.data.classes.CategoryType.OTHER
    }

    fun logout() {
        auth.signOut()
    }
}