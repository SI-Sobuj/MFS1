package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.OperationType
import com.example.data.handleFirestoreError
import com.example.data.model.MfsTransaction
import com.example.data.model.SavedBiller
import com.example.data.model.SavingsGoal
import com.example.data.model.TransactionType
import com.example.data.model.WalletProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class TakaPayRepository(private val db: FirebaseFirestore) {

  constructor(
    context: Context
  ) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private val auth = Firebase.auth

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException(
        "User must be signed in with Google before accessing Firestore."
      )
  }

  fun observeWallet(userId: String = requireUserId()): Flow<WalletProfile?> = flow {
    val path = "wallets/$userId"
    emitAll(
      db
        .collection("wallets")
        .document(userId)
        .snapshots()
        .map { snapshot ->
          if (snapshot.exists()) {
            snapshot.toObject(
              WalletProfile::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
          } else {
            null
          }
        }
        .catch { error ->
          val firestoreError =
            generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(exceptionToLog, OperationType.GET, path)
          }
          throw error
        }
    )
  }

  fun observeTransactions(userId: String = requireUserId()): Flow<List<MfsTransaction>> = flow {
    val path = "users/$userId/transactions"
    emitAll(
      db
        .collection("users")
        .document(userId)
        .collection("transactions")
        .whereEqualTo("userId", userId)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(
              MfsTransaction::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
        .catch { error ->
          val firestoreError =
            generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  fun observeSavingsGoals(userId: String = requireUserId()): Flow<List<SavingsGoal>> = flow {
    val path = "users/$userId/savings_goals"
    emitAll(
      db
        .collection("users")
        .document(userId)
        .collection("savings_goals")
        .whereEqualTo("userId", userId)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(
              SavingsGoal::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
        .catch { error ->
          val firestoreError =
            generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  fun observeSavedBillers(userId: String = requireUserId()): Flow<List<SavedBiller>> = flow {
    val path = "users/$userId/saved_billers"
    emitAll(
      db
        .collection("users")
        .document(userId)
        .collection("saved_billers")
        .whereEqualTo("userId", userId)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(
              SavedBiller::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
        .catch { error ->
          val firestoreError =
            generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  suspend fun ensureWalletInitialized(
    displayNameFallback: String = "TakaPay Member"
  ): Result<WalletProfile> = runCatching {
    val uid = requireUserId()
    val walletRef = db.collection("wallets").document(uid)
    val snapshot =
      try {
        walletRef.get().await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.GET, walletRef.path)
        throw e
      }

    if (snapshot.exists()) {
      return@runCatching checkNotNull(
        snapshot.toObject(
          WalletProfile::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        )
      )
    }

    val rawName = auth.currentUser?.displayName?.takeIf { it.isNotBlank() } ?: displayNameFallback
    val safeName = rawName.take(75)
    val suffix = (uid.hashCode().let { if (it < 0) -it else it } % 900000 + 100000).toString()
    val generatedAccountNumber = "01711-$suffix"

    val initialBalance = 18500.0
    val initialSavings = 8500.0

    val batch = db.batch()
    val walletPayload =
      mapOf(
        "userId" to uid,
        "displayName" to safeName,
        "accountNumber" to generatedAccountNumber,
        "pinHash" to "1234",
        "biometricEnabled" to true,
        "balanceBdt" to initialBalance,
        "totalSavingsBdt" to initialSavings,
        "rewardPoints" to 250L,
        "kycTier" to "Premier",
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    batch.set(walletRef, walletPayload)

    // Seed 2 starter savings vaults
    val goal1Ref = db.collection("users").document(uid).collection("savings_goals").document("goal_dps_main")
    batch.set(
      goal1Ref,
      mapOf(
        "userId" to uid,
        "title" to "IDLC Monthly Islamic DPS",
        "category" to "DPS",
        "targetAmountBdt" to 60000.0,
        "savedAmountBdt" to 6000.0,
        "monthlyDepositBdt" to 2000.0,
        "interestRatePercent" to 9.2,
        "institutionName" to "IDLC Finance DPS (9.2% p.a.)",
        "autoSaveRoundUp" to true,
        "status" to "ACTIVE",
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      ),
    )

    val goal2Ref = db.collection("users").document(uid).collection("savings_goals").document("goal_emergency")
    batch.set(
      goal2Ref,
      mapOf(
        "userId" to uid,
        "title" to "Emergency Rainy Day Vault",
        "category" to "Emergency",
        "targetAmountBdt" to 25000.0,
        "savedAmountBdt" to 2500.0,
        "monthlyDepositBdt" to 1000.0,
        "interestRatePercent" to 8.8,
        "institutionName" to "BRAC Bank Tara Savings (8.8% p.a.)",
        "autoSaveRoundUp" to false,
        "status" to "ACTIVE",
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      ),
    )

    // Seed starter saved billers & contacts
    val biller1Ref = db.collection("users").document(uid).collection("saved_billers").document("biller_desco")
    batch.set(
      biller1Ref,
      mapOf(
        "userId" to uid,
        "billerType" to "UTILITY",
        "name" to "Home DESCO Smart Meter",
        "accountOrMeterNo" to "44019283746",
        "providerCategory" to "Electricity",
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      ),
    )

    val biller2Ref = db.collection("users").document(uid).collection("saved_billers").document("biller_contact_1")
    batch.set(
      biller2Ref,
      mapOf(
        "userId" to uid,
        "billerType" to "CONTACT",
        "name" to "Tanvir Ahmed (Priyo Number)",
        "accountOrMeterNo" to "01712-908172",
        "providerCategory" to "Personal",
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      ),
    )

    // Seed initial welcome deposit transaction
    val welcomeTxRef = db.collection("users").document(uid).collection("transactions").document("tx_welcome")
    batch.set(
      welcomeTxRef,
      mapOf(
        "userId" to uid,
        "type" to TransactionType.ADD_MONEY.code,
        "title" to "Add Money from City Bank Visa",
        "recipientNumber" to generatedAccountNumber,
        "recipientName" to safeName,
        "operatorOrProvider" to "City Bank (Citytouch Visa)",
        "amountBdt" to initialBalance,
        "feeBdt" to 0.0,
        "cashbackBdt" to 50.0,
        "balanceAfterBdt" to initialBalance,
        "referenceNote" to "Opening Wallet Deposit",
        "trxId" to generateTrxId(),
        "authMethod" to "BIOMETRIC",
        "status" to "COMPLETED",
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      ),
    )

    try {
      batch.commit().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, walletRef.path)
      throw e
    }

    WalletProfile(
      id = uid,
      userId = uid,
      displayName = safeName,
      accountNumber = generatedAccountNumber,
      pinHash = "1234",
      biometricEnabled = true,
      balanceBdt = initialBalance,
      totalSavingsBdt = initialSavings,
      rewardPoints = 250L,
      kycTier = "Premier",
    )
  }

  suspend fun executeTransaction(
    currentWallet: WalletProfile,
    type: TransactionType,
    recipientNumber: String,
    recipientName: String,
    operatorOrProvider: String,
    amountBdt: Double,
    feeBdt: Double,
    cashbackBdt: Double,
    referenceNote: String,
    authMethod: String,
  ): Result<MfsTransaction> = runCatching {
    val uid = requireUserId()
    require(amountBdt > 0.0) { "Amount must be greater than ৳0" }
    val cleanRecipientNo = recipientNumber.trim().ifEmpty { currentWallet.accountNumber }.take(60)
    val cleanRecipientName = recipientName.trim().ifEmpty { cleanRecipientNo }.take(100)
    val cleanProvider = operatorOrProvider.trim().ifEmpty { type.displayName }.take(60)
    val cleanNote = referenceNote.trim().take(140)

    val netDelta =
      if (type.isCredit) {
        amountBdt + cashbackBdt - feeBdt
      } else {
        -(amountBdt + feeBdt - cashbackBdt)
      }

    val newBalance = roundCurrency(currentWallet.balanceBdt + netDelta)
    if (newBalance < 0.0) {
      throw IllegalArgumentException(
        "Insufficient TakaPay balance. Required ৳${formatBdtPlain(amountBdt + feeBdt)}, available ৳${formatBdtPlain(currentWallet.balanceBdt)}."
      )
    }

    val earnedPoints = max(1L, (amountBdt / 100.0).roundToInt().toLong())
    val newPoints = currentWallet.rewardPoints + earnedPoints
    val trxId = generateTrxId()
    val title = "${type.displayName} • $cleanRecipientName".take(100)

    val walletRef = db.collection("wallets").document(uid)
    val txDocId = "tx_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val txRef = db.collection("users").document(uid).collection("transactions").document(txDocId)

    val batch = db.batch()
    batch.update(
      walletRef,
      mapOf(
        "balanceBdt" to newBalance,
        "rewardPoints" to newPoints,
        "updatedAt" to FieldValue.serverTimestamp(),
      ),
    )

    val txPayload =
      mapOf(
        "userId" to uid,
        "type" to type.code,
        "title" to title,
        "recipientNumber" to cleanRecipientNo,
        "recipientName" to cleanRecipientName,
        "operatorOrProvider" to cleanProvider,
        "amountBdt" to roundCurrency(amountBdt),
        "feeBdt" to roundCurrency(feeBdt),
        "cashbackBdt" to roundCurrency(cashbackBdt),
        "balanceAfterBdt" to newBalance,
        "referenceNote" to cleanNote,
        "trxId" to trxId,
        "authMethod" to if (authMethod == "PIN") "PIN" else "BIOMETRIC",
        "status" to "COMPLETED",
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    batch.set(txRef, txPayload)

    try {
      batch.commit().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, txRef.path)
      throw e
    }

    MfsTransaction(
      id = txDocId,
      userId = uid,
      type = type.code,
      title = title,
      recipientNumber = cleanRecipientNo,
      recipientName = cleanRecipientName,
      operatorOrProvider = cleanProvider,
      amountBdt = roundCurrency(amountBdt),
      feeBdt = roundCurrency(feeBdt),
      cashbackBdt = roundCurrency(cashbackBdt),
      balanceAfterBdt = newBalance,
      referenceNote = cleanNote,
      trxId = trxId,
      authMethod = if (authMethod == "PIN") "PIN" else "BIOMETRIC",
      status = "COMPLETED",
    )
  }

  suspend fun createSavingsGoal(
    currentWallet: WalletProfile,
    title: String,
    category: String,
    targetAmountBdt: Double,
    initialDepositBdt: Double,
    monthlyDepositBdt: Double,
    interestRatePercent: Double,
    institutionName: String,
    autoSaveRoundUp: Boolean,
    authMethod: String,
  ): Result<String> = runCatching {
    val uid = requireUserId()
    require(title.isNotBlank()) { "Goal title cannot be blank" }
    require(targetAmountBdt > 0.0) { "Target amount must be positive" }
    require(initialDepositBdt >= 0.0) { "Initial deposit cannot be negative" }

    if (initialDepositBdt > currentWallet.balanceBdt) {
      throw IllegalArgumentException("Insufficient wallet balance for initial savings deposit.")
    }

    val goalId = "goal_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val goalRef = db.collection("users").document(uid).collection("savings_goals").document(goalId)
    val walletRef = db.collection("wallets").document(uid)

    val newBalance = roundCurrency(currentWallet.balanceBdt - initialDepositBdt)
    val newTotalSavings = roundCurrency(currentWallet.totalSavingsBdt + initialDepositBdt)

    val batch = db.batch()
    val goalPayload =
      mapOf(
        "userId" to uid,
        "title" to title.trim().take(80),
        "category" to category.takeIf {
          it in listOf("DPS", "Emergency", "Festival", "Education", "Gadget", "Travel")
        }.orEmpty().ifEmpty { "DPS" },
        "targetAmountBdt" to roundCurrency(targetAmountBdt),
        "savedAmountBdt" to roundCurrency(initialDepositBdt),
        "monthlyDepositBdt" to roundCurrency(monthlyDepositBdt),
        "interestRatePercent" to interestRatePercent.coerceIn(0.0, 25.0),
        "institutionName" to institutionName.trim().ifEmpty { "IDLC Finance DPS" }.take(60),
        "autoSaveRoundUp" to autoSaveRoundUp,
        "status" to if (initialDepositBdt >= targetAmountBdt) "COMPLETED" else "ACTIVE",
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    batch.set(goalRef, goalPayload)

    if (initialDepositBdt > 0.0) {
      batch.update(
        walletRef,
        mapOf(
          "balanceBdt" to newBalance,
          "totalSavingsBdt" to newTotalSavings,
          "rewardPoints" to (currentWallet.rewardPoints + 15L),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )

      val txDocId = "tx_${UUID.randomUUID().toString().replace("-", "").take(16)}"
      val txRef = db.collection("users").document(uid).collection("transactions").document(txDocId)
      batch.set(
        txRef,
        mapOf(
          "userId" to uid,
          "type" to TransactionType.SAVINGS_DEPOSIT.code,
          "title" to "Savings Deposit • ${title.trim().take(50)}",
          "recipientNumber" to goalId,
          "recipientName" to title.trim().take(80),
          "operatorOrProvider" to institutionName.trim().ifEmpty { "TakaPay Savings" }.take(60),
          "amountBdt" to roundCurrency(initialDepositBdt),
          "feeBdt" to 0.0,
          "cashbackBdt" to 0.0,
          "balanceAfterBdt" to newBalance,
          "referenceNote" to "Opening DPS Installment",
          "trxId" to generateTrxId(),
          "authMethod" to if (authMethod == "PIN") "PIN" else "BIOMETRIC",
          "status" to "COMPLETED",
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
    }

    try {
      batch.commit().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, goalRef.path)
      throw e
    }
    goalId
  }

  suspend fun transferSavings(
    currentWallet: WalletProfile,
    goal: SavingsGoal,
    amountBdt: Double,
    isDeposit: Boolean,
    authMethod: String,
  ): Result<MfsTransaction> = runCatching {
    val uid = requireUserId()
    require(goal.id.isNotBlank()) { "Savings goal ID required" }
    require(amountBdt > 0.0) { "Amount must be positive" }

    val newWalletBalance: Double
    val newTotalSavings: Double
    val newGoalSaved: Double

    if (isDeposit) {
      if (amountBdt > currentWallet.balanceBdt) {
        throw IllegalArgumentException("Insufficient main wallet balance for savings deposit.")
      }
      newWalletBalance = roundCurrency(currentWallet.balanceBdt - amountBdt)
      newTotalSavings = roundCurrency(currentWallet.totalSavingsBdt + amountBdt)
      newGoalSaved = roundCurrency(goal.savedAmountBdt + amountBdt)
    } else {
      if (amountBdt > goal.savedAmountBdt) {
        throw IllegalArgumentException("Withdrawal exceeds available vault balance.")
      }
      newWalletBalance = roundCurrency(currentWallet.balanceBdt + amountBdt)
      newTotalSavings = roundCurrency(max(0.0, currentWallet.totalSavingsBdt - amountBdt))
      newGoalSaved = roundCurrency(max(0.0, goal.savedAmountBdt - amountBdt))
    }

    val newStatus =
      when {
        newGoalSaved >= goal.targetAmountBdt -> "COMPLETED"
        else -> "ACTIVE"
      }

    val walletRef = db.collection("wallets").document(uid)
    val goalRef = db.collection("users").document(uid).collection("savings_goals").document(goal.id)
    val txDocId = "tx_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val txRef = db.collection("users").document(uid).collection("transactions").document(txDocId)

    val txType = if (isDeposit) TransactionType.SAVINGS_DEPOSIT else TransactionType.SAVINGS_WITHDRAW
    val title = "${txType.displayName} • ${goal.title}".take(100)
    val trxId = generateTrxId()

    val batch = db.batch()
    batch.update(
      walletRef,
      mapOf(
        "balanceBdt" to newWalletBalance,
        "totalSavingsBdt" to newTotalSavings,
        "rewardPoints" to (currentWallet.rewardPoints + if (isDeposit) 10L else 0L),
        "updatedAt" to FieldValue.serverTimestamp(),
      ),
    )
    batch.update(
      goalRef,
      mapOf(
        "savedAmountBdt" to newGoalSaved,
        "status" to newStatus,
        "updatedAt" to FieldValue.serverTimestamp(),
      ),
    )
    batch.set(
      txRef,
      mapOf(
        "userId" to uid,
        "type" to txType.code,
        "title" to title,
        "recipientNumber" to goal.id,
        "recipientName" to goal.title.take(100),
        "operatorOrProvider" to goal.institutionName.take(60),
        "amountBdt" to roundCurrency(amountBdt),
        "feeBdt" to 0.0,
        "cashbackBdt" to 0.0,
        "balanceAfterBdt" to newWalletBalance,
        "referenceNote" to if (isDeposit) "Real-time Savings Deposit" else "Savings Encashment to Wallet",
        "trxId" to trxId,
        "authMethod" to if (authMethod == "PIN") "PIN" else "BIOMETRIC",
        "status" to "COMPLETED",
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      ),
    )

    try {
      batch.commit().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, goalRef.path)
      throw e
    }

    MfsTransaction(
      id = txDocId,
      userId = uid,
      type = txType.code,
      title = title,
      recipientNumber = goal.id,
      recipientName = goal.title.take(100),
      operatorOrProvider = goal.institutionName.take(60),
      amountBdt = roundCurrency(amountBdt),
      feeBdt = 0.0,
      cashbackBdt = 0.0,
      balanceAfterBdt = newWalletBalance,
      referenceNote = if (isDeposit) "Real-time Savings Deposit" else "Savings Encashment to Wallet",
      trxId = trxId,
      authMethod = if (authMethod == "PIN") "PIN" else "BIOMETRIC",
      status = "COMPLETED",
    )
  }

  suspend fun addSavedBiller(
    billerType: String,
    name: String,
    accountOrMeterNo: String,
    providerCategory: String,
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val docId = "biller_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val docRef = db.collection("users").document(uid).collection("saved_billers").document(docId)
    val payload =
      mapOf(
        "userId" to uid,
        "billerType" to billerType.takeIf { it in listOf("CONTACT", "UTILITY", "MERCHANT", "AGENT") }
          .orEmpty()
          .ifEmpty { "UTILITY" },
        "name" to name.trim().take(80),
        "accountOrMeterNo" to accountOrMeterNo.trim().take(50),
        "providerCategory" to providerCategory.trim().take(50),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    try {
      docRef.set(payload).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
    docId
  }

  suspend fun deleteSavedBiller(billerId: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("saved_billers").document(billerId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  suspend fun updateSecuritySettings(
    biometricEnabled: Boolean,
    newPinHash: String? = null,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val walletRef = db.collection("wallets").document(uid)
    val updates = mutableMapOf<String, Any>(
      "biometricEnabled" to biometricEnabled,
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    if (!newPinHash.isNullOrBlank() && newPinHash.length in 4..64) {
      updates["pinHash"] = newPinHash
    }
    try {
      walletRef.update(updates).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, walletRef.path)
      throw e
    }
  }

  companion object {
    fun generateTrxId(): String {
      val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
      val randomPart = (1..7).map { chars.random() }.joinToString("")
      return "TKP$randomPart"
    }

    fun roundCurrency(value: Double): Double {
      return (value * 100.0).roundToInt() / 100.0
    }

    fun formatBdtPlain(value: Double): String {
      return String.format(java.util.Locale.US, "%,.2f", value)
    }
  }
}
