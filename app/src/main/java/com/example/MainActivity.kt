package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.TakaPayAuthScreen
import com.example.auth.authStateFlow
import com.example.auth.signOut
import com.example.data.model.MfsTransaction
import com.example.data.model.SavedBiller
import com.example.data.model.SavingsGoal
import com.example.data.model.WalletProfile
import com.example.data.repository.TakaPayRepository
import com.example.security.BiometricOrPinConfirmSheet
import com.example.ui.screens.AddMoneySheet
import com.example.ui.screens.CashOutSheet
import com.example.ui.screens.CreateSavingsGoalSheet
import com.example.ui.screens.MfsReceiptDialog
import com.example.ui.screens.MobileRechargeSheet
import com.example.ui.screens.PendingTransactionDraft
import com.example.ui.screens.ProfileAndSecurityScreen
import com.example.ui.screens.QrScannerAndMerchantScreen
import com.example.ui.screens.SavingsVaultScreen
import com.example.ui.screens.SendMoneySheet
import com.example.ui.screens.StatementScreen
import com.example.ui.screens.TransactionListItemCard
import com.example.ui.screens.TransferSavingsVaultSheet
import com.example.ui.screens.UtilityBillSheet
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TakaEmerald
import com.example.ui.theme.TakaGold
import com.example.ui.theme.TakaMagenta
import com.example.ui.theme.TakaMagentaDark
import com.example.ui.theme.TakaPlumSurface
import com.example.viewmodel.TakaPayViewModel
import com.example.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        AppNavigation()
      }
    }
  }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
  val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
  val user = currentUser
  if (user == null) {
    TakaPayAuthScreen(onAuthSuccess = {})
  } else {
    key(user.uid) {
      TakaPayMainShell(currentUserId = user.uid)
    }
  }
}

enum class MainTab {
  HOME,
  SAVINGS,
  SCAN_QR,
  STATEMENT,
  PROFILE,
}

enum class ActiveModal {
  NONE,
  SEND_MONEY,
  ADD_MONEY,
  CASH_OUT,
  MOBILE_RECHARGE,
  UTILITY_BILL,
  CREATE_SAVINGS,
}

private data class PendingSavingsVaultAction(
  val goal: SavingsGoal,
  val isDeposit: Boolean,
  val amountBdt: Double? = null,
)

private data class PendingCreateVaultDraft(
  val title: String,
  val category: String,
  val targetAmount: Double,
  val initialDeposit: Double,
  val monthlyDeposit: Double,
  val interestRate: Double,
  val institution: String,
  val autoRoundUp: Boolean,
)

@Composable
fun TakaPayMainShell(currentUserId: String) {
  val viewModel: TakaPayViewModel =
    viewModel(
      key = currentUserId,
      factory =
        viewModelFactory {
          initializer {
            val app = checkNotNull(this[APPLICATION_KEY]) { "APPLICATION_KEY missing" }
            val databaseId = app.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(databaseId)
            TakaPayViewModel(TakaPayRepository(db), currentUserId)
          }
        },
    )

  val walletUiState by viewModel.walletState.collectAsStateWithLifecycle()
  val transactionsUiState by viewModel.transactionsState.collectAsStateWithLifecycle()
  val savingsUiState by viewModel.savingsGoalsState.collectAsStateWithLifecycle()
  val billersUiState by viewModel.savedBillersState.collectAsStateWithLifecycle()
  val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
  val latestReceipt by viewModel.latestReceipt.collectAsStateWithLifecycle()
  val bannerMessage by viewModel.bannerMessage.collectAsStateWithLifecycle()

  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  val snackbarHostState = remember { SnackbarHostState() }

  var currentTab by rememberSaveable { mutableStateOf(MainTab.HOME) }
  var activeModal by rememberSaveable { mutableStateOf(ActiveModal.NONE) }
  var pendingDraft by remember { mutableStateOf<PendingTransactionDraft?>(null) }
  var pendingSavingsTransfer by remember { mutableStateOf<PendingSavingsVaultAction?>(null) }
  var pendingNewVaultDraft by remember { mutableStateOf<PendingCreateVaultDraft?>(null) }
  var selectedReceiptTx by remember { mutableStateOf<MfsTransaction?>(null) }

  BackHandler(enabled = currentTab != MainTab.HOME || activeModal != ActiveModal.NONE) {
    if (activeModal != ActiveModal.NONE) {
      activeModal = ActiveModal.NONE
    } else {
      currentTab = MainTab.HOME
    }
  }

  LaunchedEffect(bannerMessage) {
    bannerMessage?.let {
      snackbarHostState.showSnackbar(it)
      viewModel.clearBannerMessage()
    }
  }

  val wallet =
    (walletUiState as? UiState.Success)?.data
      ?: WalletProfile(
        id = currentUserId,
        userId = currentUserId,
        displayName = "TakaPay Member",
        accountNumber = "01711-000000",
        balanceBdt = 18500.0,
        totalSavingsBdt = 8500.0,
        rewardPoints = 250L,
        kycTier = "Premier",
      )

  val transactions = (transactionsUiState as? UiState.Success)?.data ?: emptyList()
  val savingsGoals = (savingsUiState as? UiState.Success)?.data ?: emptyList()
  val savedBillers = (billersUiState as? UiState.Success)?.data ?: emptyList()

  Scaffold(
    contentWindowInsets = WindowInsets.safeDrawing,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    bottomBar = {
      NavigationBar {
        NavigationBarItem(
          selected = currentTab == MainTab.HOME,
          onClick = { currentTab = MainTab.HOME },
          icon = { Icon(Icons.Default.Home, contentDescription = stringResource(R.string.nav_home)) },
          label = { Text(stringResource(R.string.nav_home)) },
          modifier = Modifier.testTag("nav_tab_home"),
        )
        NavigationBarItem(
          selected = currentTab == MainTab.SAVINGS,
          onClick = { currentTab = MainTab.SAVINGS },
          icon = { Icon(Icons.Default.Savings, contentDescription = stringResource(R.string.nav_savings)) },
          label = { Text(stringResource(R.string.nav_savings)) },
          modifier = Modifier.testTag("nav_tab_savings"),
        )
        NavigationBarItem(
          selected = currentTab == MainTab.SCAN_QR,
          onClick = { currentTab = MainTab.SCAN_QR },
          icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = stringResource(R.string.nav_scan_qr)) },
          label = { Text(stringResource(R.string.nav_scan_qr)) },
          modifier = Modifier.testTag("nav_tab_qr"),
        )
        NavigationBarItem(
          selected = currentTab == MainTab.STATEMENT,
          onClick = { currentTab = MainTab.STATEMENT },
          icon = {
            Icon(
              Icons.AutoMirrored.Filled.ReceiptLong,
              contentDescription = stringResource(R.string.nav_inbox),
            )
          },
          label = { Text(stringResource(R.string.nav_inbox)) },
          modifier = Modifier.testTag("nav_tab_statement"),
        )
        NavigationBarItem(
          selected = currentTab == MainTab.PROFILE,
          onClick = { currentTab = MainTab.PROFILE },
          icon = { Icon(Icons.Default.Person, contentDescription = stringResource(R.string.nav_profile)) },
          label = { Text(stringResource(R.string.nav_profile)) },
          modifier = Modifier.testTag("nav_tab_profile"),
        )
      }
    },
  ) { innerPadding ->
    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
      when (currentTab) {
        MainTab.HOME -> {
          TakaPayHomeDashboard(
            wallet = wallet,
            isLoadingWallet = walletUiState is UiState.Loading,
            transactions = transactions,
            savingsGoals = savingsGoals,
            savedBillers = savedBillers,
            onOpenModal = { activeModal = it },
            onNavigateTab = { currentTab = it },
            onTransactionClick = { selectedReceiptTx = it },
          )
        }
        MainTab.SAVINGS -> {
          SavingsVaultScreen(
            wallet = wallet,
            goals = savingsGoals,
            onCreateNewVaultClick = { activeModal = ActiveModal.CREATE_SAVINGS },
            onTransferVaultClick = { goal, isDeposit ->
              pendingSavingsTransfer = PendingSavingsVaultAction(goal, isDeposit, null)
            },
          )
        }
        MainTab.SCAN_QR -> {
          QrScannerAndMerchantScreen(
            wallet = wallet,
            onProceedToMerchantPayment = { draft -> pendingDraft = draft },
          )
        }
        MainTab.STATEMENT -> {
          StatementScreen(
            transactions = transactions,
            onTransactionClick = { selectedReceiptTx = it },
          )
        }
        MainTab.PROFILE -> {
          ProfileAndSecurityScreen(
            wallet = wallet,
            savedBillers = savedBillers,
            onUpdateSecurity = { bio, pin -> viewModel.updateSecuritySettings(bio, pin) },
            onDeleteBiller = { viewModel.deleteSavedBiller(it) },
            onSignOutClick = {
              signOut(credentialManager = credentialManager, scope = scope)
            },
          )
        }
      }
    }
  }

  // Action Modals
  when (activeModal) {
    ActiveModal.SEND_MONEY -> {
      SendMoneySheet(
        wallet = wallet,
        savedContacts = savedBillers.filter { it.billerType == "CONTACT" },
        onProceedToAuth = { draft ->
          activeModal = ActiveModal.NONE
          pendingDraft = draft
        },
        onDismiss = { activeModal = ActiveModal.NONE },
      )
    }
    ActiveModal.ADD_MONEY -> {
      AddMoneySheet(
        wallet = wallet,
        onProceedToAuth = { draft ->
          activeModal = ActiveModal.NONE
          pendingDraft = draft
        },
        onDismiss = { activeModal = ActiveModal.NONE },
      )
    }
    ActiveModal.CASH_OUT -> {
      CashOutSheet(
        wallet = wallet,
        onProceedToAuth = { draft ->
          activeModal = ActiveModal.NONE
          pendingDraft = draft
        },
        onDismiss = { activeModal = ActiveModal.NONE },
      )
    }
    ActiveModal.MOBILE_RECHARGE -> {
      MobileRechargeSheet(
        wallet = wallet,
        onProceedToAuth = { draft ->
          activeModal = ActiveModal.NONE
          pendingDraft = draft
        },
        onDismiss = { activeModal = ActiveModal.NONE },
      )
    }
    ActiveModal.UTILITY_BILL -> {
      UtilityBillSheet(
        wallet = wallet,
        savedBillers = savedBillers.filter { it.billerType == "UTILITY" },
        onProceedToAuth = { draft ->
          activeModal = ActiveModal.NONE
          pendingDraft = draft
        },
        onDismiss = { activeModal = ActiveModal.NONE },
      )
    }
    ActiveModal.CREATE_SAVINGS -> {
      CreateSavingsGoalSheet(
        wallet = wallet,
        onProceedToAuth = { title, cat, target, initial, monthly, rate, inst, roundUp ->
          activeModal = ActiveModal.NONE
          pendingNewVaultDraft =
            PendingCreateVaultDraft(title, cat, target, initial, monthly, rate, inst, roundUp)
        },
        onDismiss = { activeModal = ActiveModal.NONE },
      )
    }
    ActiveModal.NONE -> {}
  }

  // Savings Deposit / Encash Amount Modal
  pendingSavingsTransfer?.let { action ->
    if (action.amountBdt == null) {
      TransferSavingsVaultSheet(
        wallet = wallet,
        goal = action.goal,
        isDeposit = action.isDeposit,
        onProceedToAuth = { amt ->
          pendingSavingsTransfer = action.copy(amountBdt = amt)
        },
        onDismiss = { pendingSavingsTransfer = null },
      )
    } else {
      BiometricOrPinConfirmSheet(
        actionTitle = if (action.isDeposit) "Savings Vault Deposit" else "Savings Vault Encashment",
        recipientSummary = "${action.goal.title} • ${action.goal.institutionName}",
        amountBdt = action.amountBdt,
        feeBdt = 0.0,
        cashbackBdt = 0.0,
        expectedPin = wallet.pinHash,
        biometricPreferred = wallet.biometricEnabled,
        isProcessing = isProcessing,
        onConfirmed = { authMethod ->
          viewModel.transferSavingsVault(
            wallet = wallet,
            goal = action.goal,
            amountBdt = action.amountBdt,
            isDeposit = action.isDeposit,
            authMethod = authMethod,
            onSuccess = { pendingSavingsTransfer = null },
          )
        },
        onDismiss = { pendingSavingsTransfer = null },
      )
    }
  }

  // New Savings Goal Biometric / PIN Confirmation
  pendingNewVaultDraft?.let { vaultDraft ->
    BiometricOrPinConfirmSheet(
      actionTitle = "Open DPS / Savings Vault",
      recipientSummary = "${vaultDraft.title} • ${vaultDraft.institution}",
      amountBdt = vaultDraft.initialDeposit,
      feeBdt = 0.0,
      cashbackBdt = 0.0,
      expectedPin = wallet.pinHash,
      biometricPreferred = wallet.biometricEnabled,
      isProcessing = isProcessing,
      onConfirmed = { authMethod ->
        viewModel.createNewSavingsGoal(
          wallet = wallet,
          title = vaultDraft.title,
          category = vaultDraft.category,
          targetAmountBdt = vaultDraft.targetAmount,
          initialDepositBdt = vaultDraft.initialDeposit,
          monthlyDepositBdt = vaultDraft.monthlyDeposit,
          interestRatePercent = vaultDraft.interestRate,
          institutionName = vaultDraft.institution,
          autoSaveRoundUp = vaultDraft.autoRoundUp,
          authMethod = authMethod,
          onSuccess = { pendingNewVaultDraft = null },
        )
      },
      onDismiss = { pendingNewVaultDraft = null },
    )
  }

  // Standard MFS Transaction Biometric / PIN Confirmation Sheet
  pendingDraft?.let { draft ->
    BiometricOrPinConfirmSheet(
      actionTitle = draft.type.displayName,
      recipientSummary = "${draft.recipientName} (${draft.recipientNumber})",
      amountBdt = draft.amountBdt,
      feeBdt = draft.feeBdt,
      cashbackBdt = draft.cashbackBdt,
      expectedPin = wallet.pinHash,
      biometricPreferred = wallet.biometricEnabled,
      isProcessing = isProcessing,
      onConfirmed = { authMethod ->
        viewModel.executeMfsTransaction(
          wallet = wallet,
          type = draft.type,
          recipientNumber = draft.recipientNumber,
          recipientName = draft.recipientName,
          operatorOrProvider = draft.operatorOrProvider,
          amountBdt = draft.amountBdt,
          feeBdt = draft.feeBdt,
          cashbackBdt = draft.cashbackBdt,
          referenceNote = draft.referenceNote,
          authMethod = authMethod,
          saveAsFavorite = draft.saveAsFavorite,
          favoriteBillerType = draft.favoriteBillerType,
          onSuccess = { pendingDraft = null },
        )
      },
      onDismiss = { pendingDraft = null },
    )
  }

  // Receipt Dialog
  val activeReceipt = latestReceipt ?: selectedReceiptTx
  if (activeReceipt != null) {
    MfsReceiptDialog(
      transaction = activeReceipt,
      onDismiss = {
        viewModel.clearReceipt()
        selectedReceiptTx = null
      },
    )
  }
}

@Composable
private fun TakaPayHomeDashboard(
  wallet: WalletProfile,
  isLoadingWallet: Boolean,
  transactions: List<MfsTransaction>,
  savingsGoals: List<SavingsGoal>,
  savedBillers: List<SavedBiller>,
  onOpenModal: (ActiveModal) -> Unit,
  onNavigateTab: (MainTab) -> Unit,
  onTransactionClick: (MfsTransaction) -> Unit,
) {
  // Signature bKash / Nagad "Tap for Balance" auto-hiding reveal
  var isBalanceRevealed by remember { mutableStateOf(true) }
  LaunchedEffect(isBalanceRevealed) {
    if (isBalanceRevealed) {
      delay(6500L)
      isBalanceRevealed = false
    }
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    // 1. Signature bKash / Nagad curved magenta header with Tap-for-Balance pill
    item {
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(
              Brush.verticalGradient(
                colors = listOf(TakaPlumSurface, TakaMagentaDark, TakaMagenta)
              )
            )
            .padding(horizontal = 20.dp, vertical = 20.dp)
      ) {
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.size(46.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text(
                    text = wallet.displayName.take(1).uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                  )
                }
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = wallet.displayName,
                  style = MaterialTheme.typography.titleMedium,
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = "${wallet.accountNumber} • ${wallet.kycTier}",
                  style = MaterialTheme.typography.labelMedium,
                  color = TakaGold,
                )
              }
            }

            // Reward Points Pill
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = Color.White.copy(alpha = 0.16f),
              modifier = Modifier.clickable { onNavigateTab(MainTab.PROFILE) },
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = "★ ${wallet.rewardPoints} Pts",
                  style = MaterialTheme.typography.labelMedium,
                  color = TakaGold,
                  fontWeight = FontWeight.Bold,
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(18.dp))

          // Interactive "Tap for Balance" Pill + Live Savings Chip
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Surface(
              shape = RoundedCornerShape(24.dp),
              color = Color.White,
              shadowElevation = 6.dp,
              modifier =
                Modifier.weight(1.25f)
                  .height(52.dp)
                  .clickable { isBalanceRevealed = !isBalanceRevealed }
                  .testTag("tap_for_balance_pill"),
            ) {
              Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Box(
                    modifier =
                      Modifier.size(30.dp).clip(CircleShape).background(TakaMagenta),
                    contentAlignment = Alignment.Center,
                  ) {
                    Text(
                      text = "৳",
                      style = MaterialTheme.typography.titleMedium,
                      color = Color.White,
                      fontWeight = FontWeight.Bold,
                    )
                  }
                  Spacer(modifier = Modifier.width(10.dp))
                  AnimatedContent(targetState = isBalanceRevealed, label = "balanceReveal") { revealed ->
                    if (isLoadingWallet) {
                      CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = TakaMagenta,
                      )
                    } else if (revealed) {
                      Column {
                        Text(
                          text = "৳${TakaPayRepository.formatBdtPlain(wallet.balanceBdt)}",
                          style = MaterialTheme.typography.titleMedium,
                          color = TakaMagentaDark,
                          fontWeight = FontWeight.Bold,
                        )
                        Text(
                          text = "Available Balance",
                          style = MaterialTheme.typography.labelSmall,
                          color = Color.Gray,
                        )
                      }
                    } else {
                      Text(
                        text = stringResource(R.string.tap_for_balance),
                        style = MaterialTheme.typography.titleSmall,
                        color = TakaMagenta,
                        fontWeight = FontWeight.Bold,
                      )
                    }
                  }
                }
                Icon(
                  imageVector =
                    if (isBalanceRevealed) Icons.Default.VisibilityOff
                    else Icons.Default.Visibility,
                  contentDescription = "Toggle Balance Visibility",
                  tint = TakaMagenta,
                  modifier = Modifier.size(18.dp),
                )
              }
            }

            // Real-time Savings Quick Pill
            Surface(
              shape = RoundedCornerShape(24.dp),
              color = Color.White.copy(alpha = 0.16f),
              modifier =
                Modifier.weight(0.95f)
                  .height(52.dp)
                  .clickable { onNavigateTab(MainTab.SAVINGS) }
                  .testTag("header_savings_pill"),
            ) {
              Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.Center,
              ) {
                Text(
                  text = "Live Savings",
                  style = MaterialTheme.typography.labelSmall,
                  color = Color.White.copy(alpha = 0.85f),
                )
                Text(
                  text = "৳${TakaPayRepository.formatBdtPlain(wallet.totalSavingsBdt)}",
                  style = MaterialTheme.typography.titleSmall,
                  color = TakaGold,
                  fontWeight = FontWeight.Bold,
                )
              }
            }
          }
        }
      }
    }

    // 2. Signature 8-Service MFS Action Grid (bKash / Nagad / Rocket Core Services)
    item {
      Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "MFS Financial Services",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
          )
          Spacer(modifier = Modifier.height(12.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            MfsServiceTile(
              icon = Icons.AutoMirrored.Filled.Send,
              label = "Send Money",
              color = Color(0xFFD81B60),
              tag = "service_send_money",
              onClick = { onOpenModal(ActiveModal.SEND_MONEY) },
              modifier = Modifier.weight(1f),
            )
            MfsServiceTile(
              icon = Icons.Default.PhoneAndroid,
              label = "Recharge",
              color = Color(0xFF059669),
              tag = "service_mobile_recharge",
              onClick = { onOpenModal(ActiveModal.MOBILE_RECHARGE) },
              modifier = Modifier.weight(1f),
            )
            MfsServiceTile(
              icon = Icons.Default.LocalAtm,
              label = "Cash Out",
              color = Color(0xFFEA580C),
              tag = "service_cash_out",
              onClick = { onOpenModal(ActiveModal.CASH_OUT) },
              modifier = Modifier.weight(1f),
            )
            MfsServiceTile(
              icon = Icons.Default.Storefront,
              label = "Merchant QR",
              color = Color(0xFF7C3AED),
              tag = "service_merchant_qr",
              onClick = { onNavigateTab(MainTab.SCAN_QR) },
              modifier = Modifier.weight(1f),
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            MfsServiceTile(
              icon = Icons.Default.AddCard,
              label = "Add Money",
              color = Color(0xFF0284C7),
              tag = "service_add_money",
              onClick = { onOpenModal(ActiveModal.ADD_MONEY) },
              modifier = Modifier.weight(1f),
            )
            MfsServiceTile(
              icon = Icons.Default.ElectricBolt,
              label = "Pay Bill",
              color = Color(0xFFD97706),
              tag = "service_pay_bill",
              onClick = { onOpenModal(ActiveModal.UTILITY_BILL) },
              modifier = Modifier.weight(1f),
            )
            MfsServiceTile(
              icon = Icons.Default.Savings,
              label = "Savings DPS",
              color = Color(0xFF0D9488),
              tag = "service_savings_dps",
              onClick = { onNavigateTab(MainTab.SAVINGS) },
              modifier = Modifier.weight(1f),
            )
            MfsServiceTile(
              icon = Icons.Default.QrCode2,
              label = "My QR",
              color = Color(0xFFBE185D),
              tag = "service_my_qr",
              onClick = { onNavigateTab(MainTab.SCAN_QR) },
              modifier = Modifier.weight(1f),
            )
          }
        }
      }
    }

    // 3. Promotional Offer Banner with Generated Artwork
    item {
      Card(
        shape = RoundedCornerShape(22.dp),
        modifier =
          Modifier.fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onNavigateTab(MainTab.SCAN_QR) },
      ) {
        Box(modifier = Modifier.fillMaxWidth().height(135.dp)) {
          Image(
            painter = painterResource(id = R.drawable.img_mfs_promo_banner_1794239780791),
            contentDescription = "Cashback Offer Banner",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
          Box(
            modifier =
              Modifier.fillMaxSize()
                .background(
                  Brush.horizontalGradient(
                    colors =
                      listOf(
                        TakaPlumSurface.copy(alpha = 0.92f),
                        TakaPlumSurface.copy(alpha = 0.45f),
                        Color.Transparent,
                      )
                  )
                )
                .padding(18.dp),
            contentAlignment = Alignment.CenterStart,
          ) {
            Column(modifier = Modifier.fillMaxWidth(0.75f)) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = TakaGold,
              ) {
                Text(
                  text = "INSTANT QR CASHBACK",
                  style = MaterialTheme.typography.labelSmall,
                  color = Color.Black,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
              }
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "Up to 10% Instant Cashback at Aarong, Swapno & Unimart",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "Tap to Scan BanglaQR with Biometric Touch →",
                style = MaterialTheme.typography.bodySmall,
                color = TakaGold,
              )
            }
          }
        }
      }
    }

    // 4. Real-Time Savings Vault Tracker Preview
    if (savingsGoals.isNotEmpty()) {
      item {
        val topGoal = savingsGoals.first()
        val progress =
          if (topGoal.targetAmountBdt > 0) {
            (topGoal.savedAmountBdt / topGoal.targetAmountBdt).toFloat().coerceIn(0f, 1f)
          } else 0f

        Card(
          shape = RoundedCornerShape(20.dp),
          colors =
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
          modifier =
            Modifier.fillMaxWidth()
              .padding(horizontal = 16.dp)
              .clickable { onNavigateTab(MainTab.SAVINGS) },
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  Icons.Default.Savings,
                  contentDescription = null,
                  tint = TakaEmerald,
                  modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                  Text(
                    text = topGoal.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text = "${topGoal.institutionName} • ${topGoal.interestRatePercent}% p.a.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
              Text(
                text = "৳${TakaPayRepository.formatBdtPlain(topGoal.savedAmountBdt)} / ৳${TakaPayRepository.formatBdtPlain(topGoal.targetAmountBdt)}",
                style = MaterialTheme.typography.labelLarge,
                color = TakaEmerald,
                fontWeight = FontWeight.Bold,
              )
            }
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
              progress = { progress },
              color = TakaEmerald,
              trackColor = MaterialTheme.colorScheme.surface,
              modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            )
          }
        }
      }
    }

    // 5. 1-Tap Saved Billers & Priyo Contacts Strip
    if (savedBillers.isNotEmpty()) {
      item {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
          Text(
            text = "1-Tap Saved Billers & Priyo Contacts",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
          )
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            savedBillers.forEach { biller ->
              AssistChip(
                onClick = {
                  if (biller.billerType == "UTILITY") {
                    onOpenModal(ActiveModal.UTILITY_BILL)
                  } else {
                    onOpenModal(ActiveModal.SEND_MONEY)
                  }
                },
                label = { Text("${biller.name} (${biller.providerCategory})") },
                leadingIcon = {
                  Icon(
                    imageVector =
                      if (biller.billerType == "UTILITY") Icons.Default.ElectricBolt
                      else Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = TakaMagenta,
                    modifier = Modifier.size(16.dp),
                  )
                },
              )
            }
          }
        }
      }
    }

    // 6. Recent Transactions Header
    item {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Recent Transactions",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
        TextButton(onClick = { onNavigateTab(MainTab.STATEMENT) }) {
          Text("See All (${transactions.size})", color = TakaMagenta, fontWeight = FontWeight.Bold)
        }
      }
    }

    items(transactions.take(5), key = { it.id }) { tx ->
      Box(modifier = Modifier.padding(horizontal = 16.dp)) {
        TransactionListItemCard(tx = tx, onClick = { onTransactionClick(tx) })
      }
    }

    item { Spacer(modifier = Modifier.height(24.dp)) }
  }
}

@Composable
private fun MfsServiceTile(
  icon: ImageVector,
  label: String,
  color: Color,
  tag: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier =
      modifier
        .clip(RoundedCornerShape(14.dp))
        .clickable(onClick = onClick)
        .padding(vertical = 6.dp)
        .testTag(tag),
  ) {
    Box(
      modifier =
        Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(color.copy(alpha = 0.14f)),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = color,
        modifier = Modifier.size(26.dp),
      )
    }
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.SemiBold,
      textAlign = TextAlign.Center,
      maxLines = 1,
    )
  }
}
