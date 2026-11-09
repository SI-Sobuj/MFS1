package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.TransactionType
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TakaPayRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun initializeWallet_andExecuteTransactions_updatesBalanceAndCreatesRecord() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = TakaPayRepository(firestore)

    val walletResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.ensureWalletInitialized("Alice Rahman")
    }
    assertTrue("Expected wallet initialization to succeed", walletResult.isSuccess)

    val observedWallet = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeWallet(uid).first { it != null }
    }
    assertNotNull(observedWallet)

    val txResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.executeTransaction(
        currentWallet = observedWallet!!,
        type = TransactionType.SEND_MONEY,
        recipientNumber = "01715-998877",
        recipientName = "Karim Mia",
        operatorOrProvider = "TakaPay P2P",
        amountBdt = 500.0,
        feeBdt = 5.0,
        cashbackBdt = 0.0,
        referenceNote = "Dinner share",
        authMethod = "BIOMETRIC",
      )
    }
    assertTrue("Expected send money transaction to succeed: ${txResult.exceptionOrNull()}", txResult.isSuccess)

    val txs = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeTransactions(uid).first { it.isNotEmpty() }
    }
    assertTrue(txs.any { it.recipientName == "Karim Mia" })
  }

  @Test
  fun createSavingsGoal_andBiller_succeedsForAuthenticatedOwner() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = TakaPayRepository(firestore)

    val wallet = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.ensureWalletInitialized("Alice Rahman").getOrThrow()
    }

    val goalResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.createSavingsGoal(
        currentWallet = wallet,
        title = "Eid Bonus Vault",
        category = "Festival",
        targetAmountBdt = 20000.0,
        initialDepositBdt = 1000.0,
        monthlyDepositBdt = 2000.0,
        interestRatePercent = 9.0,
        institutionName = "IDLC Finance",
        autoSaveRoundUp = true,
        authMethod = "BIOMETRIC",
      )
    }
    assertTrue("Expected savings goal creation to succeed: ${goalResult.exceptionOrNull()}", goalResult.isSuccess)

    val goals = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeSavingsGoals(uid).first { it.isNotEmpty() }
    }
    assertTrue(goals.any { it.title == "Eid Bonus Vault" })

    val billerResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.addSavedBiller("UTILITY", "Dhaka WASA Home", "99102938", "Water")
    }
    assertTrue(billerResult.isSuccess)
  }

  @Test
  fun crossUserWalletAccess_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = TakaPayRepository(firestore)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.ensureWalletInitialized("Alice Rahman").getOrThrow()
    }

    signInTestUser(BOB_EMAIL)
    var thrown: FirebaseFirestoreException? = null
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        firestore.collection("wallets").document(aliceUid).get().await()
      }
    } catch (e: Exception) {
      thrown = e.unwrapFirestoreException()
    }
    assertNotNull("Expected PERMISSION_DENIED when reading another user's wallet", thrown)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, thrown?.code)
  }

  @Test
  fun unauthenticatedObserveTransactions_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    auth.signOut()
    val repo = TakaPayRepository(firestore)

    var thrown: FirebaseFirestoreException? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeTransactions(aliceUid).first()
      }
    } catch (e: Exception) {
      thrown = e.unwrapFirestoreException()
    }
    assertNotNull("Expected PERMISSION_DENIED for unauthenticated caller", thrown)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, thrown?.code)
  }

  private companion object {
    const val ALICE_EMAIL = "alice.mfs@test.com"
    const val BOB_EMAIL = "bob.mfs@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 4000L
  }
}
