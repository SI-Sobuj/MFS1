package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.MfsTransaction
import com.example.data.model.SavedBiller
import com.example.data.model.SavingsGoal
import com.example.data.model.TransactionType
import com.example.data.model.WalletProfile
import com.example.data.repository.TakaPayRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>

  data class Success<T>(val data: T) : UiState<T>

  data class Error(val message: String) : UiState<Nothing>
}

class TakaPayViewModel(
  private val repository: TakaPayRepository,
  private val currentUserId: String,
) : ViewModel() {

  val walletState: StateFlow<UiState<WalletProfile?>> =
    repository
      .observeWallet(currentUserId)
      .map<WalletProfile?, UiState<WalletProfile?>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing wallet", rootError)
        emit(UiState.Error(rootError.message ?: "Failed to load wallet"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val transactionsState: StateFlow<UiState<List<MfsTransaction>>> =
    repository
      .observeTransactions(currentUserId)
      .map<List<MfsTransaction>, UiState<List<MfsTransaction>>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing transactions", rootError)
        emit(UiState.Error(rootError.message ?: "Failed to load transactions"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val savingsGoalsState: StateFlow<UiState<List<SavingsGoal>>> =
    repository
      .observeSavingsGoals(currentUserId)
      .map<List<SavingsGoal>, UiState<List<SavingsGoal>>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing savings goals", rootError)
        emit(UiState.Error(rootError.message ?: "Failed to load savings goals"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val savedBillersState: StateFlow<UiState<List<SavedBiller>>> =
    repository
      .observeSavedBillers(currentUserId)
      .map<List<SavedBiller>, UiState<List<SavedBiller>>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing saved billers", rootError)
        emit(UiState.Error(rootError.message ?: "Failed to load saved billers"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  private val _isProcessing = MutableStateFlow(false)
  val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

  private val _latestReceipt = MutableStateFlow<MfsTransaction?>(null)
  val latestReceipt: StateFlow<MfsTransaction?> = _latestReceipt.asStateFlow()

  private val _bannerMessage = MutableStateFlow<String?>(null)
  val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

  init {
    viewModelScope.launch {
      repository.ensureWalletInitialized().onFailure { e ->
        Log.w(TAG, "Wallet initialization failed: ${e.message}", e)
      }
    }
  }

  fun clearReceipt() {
    _latestReceipt.value = null
  }

  fun clearBannerMessage() {
    _bannerMessage.value = null
  }

  fun executeMfsTransaction(
    wallet: WalletProfile,
    type: TransactionType,
    recipientNumber: String,
    recipientName: String,
    operatorOrProvider: String,
    amountBdt: Double,
    feeBdt: Double,
    cashbackBdt: Double,
    referenceNote: String,
    authMethod: String,
    saveAsFavorite: Boolean = false,
    favoriteBillerType: String = "CONTACT",
    onSuccess: () -> Unit = {},
  ) {
    viewModelScope.launch {
      _isProcessing.value = true
      val result =
        repository.executeTransaction(
          currentWallet = wallet,
          type = type,
          recipientNumber = recipientNumber,
          recipientName = recipientName,
          operatorOrProvider = operatorOrProvider,
          amountBdt = amountBdt,
          feeBdt = feeBdt,
          cashbackBdt = cashbackBdt,
          referenceNote = referenceNote,
          authMethod = authMethod,
        )
      if (saveAsFavorite && recipientNumber.isNotBlank()) {
        repository.addSavedBiller(
          billerType = favoriteBillerType,
          name = recipientName.ifBlank { recipientNumber },
          accountOrMeterNo = recipientNumber,
          providerCategory = operatorOrProvider,
        )
      }
      _isProcessing.value = false
      result
        .onSuccess { tx ->
          _latestReceipt.value = tx
          onSuccess()
        }
        .onFailure { err ->
          _bannerMessage.value = err.localizedMessage ?: "Transaction failed"
        }
    }
  }

  fun createNewSavingsGoal(
    wallet: WalletProfile,
    title: String,
    category: String,
    targetAmountBdt: Double,
    initialDepositBdt: Double,
    monthlyDepositBdt: Double,
    interestRatePercent: Double,
    institutionName: String,
    autoSaveRoundUp: Boolean,
    authMethod: String,
    onSuccess: () -> Unit = {},
  ) {
    viewModelScope.launch {
      _isProcessing.value = true
      val result =
        repository.createSavingsGoal(
          currentWallet = wallet,
          title = title,
          category = category,
          targetAmountBdt = targetAmountBdt,
          initialDepositBdt = initialDepositBdt,
          monthlyDepositBdt = monthlyDepositBdt,
          interestRatePercent = interestRatePercent,
          institutionName = institutionName,
          autoSaveRoundUp = autoSaveRoundUp,
          authMethod = authMethod,
        )
      _isProcessing.value = false
      result
        .onSuccess {
          _bannerMessage.value = "Created '$title' Savings Vault!"
          onSuccess()
        }
        .onFailure { err ->
          _bannerMessage.value = err.localizedMessage ?: "Could not create savings vault"
        }
    }
  }

  fun transferSavingsVault(
    wallet: WalletProfile,
    goal: SavingsGoal,
    amountBdt: Double,
    isDeposit: Boolean,
    authMethod: String,
    onSuccess: () -> Unit = {},
  ) {
    viewModelScope.launch {
      _isProcessing.value = true
      val result =
        repository.transferSavings(
          currentWallet = wallet,
          goal = goal,
          amountBdt = amountBdt,
          isDeposit = isDeposit,
          authMethod = authMethod,
        )
      _isProcessing.value = false
      result
        .onSuccess { tx ->
          _latestReceipt.value = tx
          onSuccess()
        }
        .onFailure { err ->
          _bannerMessage.value = err.localizedMessage ?: "Savings transfer failed"
        }
    }
  }

  fun addSavedBiller(
    billerType: String,
    name: String,
    accountOrMeterNo: String,
    providerCategory: String,
  ) {
    viewModelScope.launch {
      repository
        .addSavedBiller(billerType, name, accountOrMeterNo, providerCategory)
        .onSuccess { _bannerMessage.value = "Saved '$name' for 1-Tap Payments" }
        .onFailure { _bannerMessage.value = it.localizedMessage ?: "Failed to save biller" }
    }
  }

  fun deleteSavedBiller(billerId: String) {
    viewModelScope.launch {
      repository.deleteSavedBiller(billerId)
    }
  }

  fun updateSecuritySettings(biometricEnabled: Boolean, newPinHash: String? = null) {
    viewModelScope.launch {
      repository
        .updateSecuritySettings(biometricEnabled, newPinHash)
        .onSuccess { _bannerMessage.value = "Security preferences updated" }
        .onFailure { _bannerMessage.value = it.localizedMessage ?: "Failed to update security" }
    }
  }

  companion object {
    private const val TAG = "TakaPayVM"
    private const val STOP_TIMEOUT_MILLIS = 5000L

    fun calculateFee(type: TransactionType, amountBdt: Double): Double {
      if (amountBdt <= 0.0) return 0.0
      return when (type) {
        // bKash / Nagad style P2P Send Money fee: Free up to 500 BDT, 5 BDT above
        TransactionType.SEND_MONEY -> if (amountBdt <= 500.0) 0.0 else 5.0
        // Cash Out fee: 1.49% (14.90 BDT per 1,000 BDT) like Priyo Agent / Nagad rate
        TransactionType.CASH_OUT -> ((amountBdt * 0.0149) * 100.0).roundToInt() / 100.0
        // Utility Bill: Free for first 2 bills or 1% above 2000 BDT
        TransactionType.UTILITY_BILL -> if (amountBdt <= 2000.0) 0.0 else 10.0
        // Add Money, Mobile Recharge, Merchant Pay, Savings are 0 fee
        else -> 0.0
      }
    }
  }
}
