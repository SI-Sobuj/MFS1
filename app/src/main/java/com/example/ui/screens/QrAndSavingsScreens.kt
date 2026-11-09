package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.MfsPresets
import com.example.data.model.SavingsGoal
import com.example.data.model.TransactionType
import com.example.data.model.WalletProfile
import com.example.data.repository.TakaPayRepository
import com.example.ui.theme.TakaEmerald
import com.example.ui.theme.TakaGold
import com.example.ui.theme.TakaMagenta
import com.example.ui.theme.TakaMagentaDark
import com.example.ui.theme.TakaPlumSurface

@Composable
fun QrScannerAndMerchantScreen(
  wallet: WalletProfile,
  onProceedToMerchantPayment: (PendingTransactionDraft) -> Unit,
) {
  var showMyPersonalQr by remember { mutableStateOf(false) }
  var selectedMerchant by remember { mutableStateOf(MfsPresets.merchantQrPresets.first()) }
  var merchantCodeInput by remember { mutableStateOf(selectedMerchant.merchantCode) }
  var merchantNameInput by remember { mutableStateOf(selectedMerchant.merchantName) }
  var amountText by remember { mutableStateOf(selectedMerchant.suggestedAmount.toInt().toString()) }
  var invoiceNote by remember { mutableStateOf("Counter QR Payment") }

  val amountBdt = amountText.toDoubleOrNull() ?: 0.0
  val cashbackBdt =
    TakaPayRepository.roundCurrency(amountBdt * (selectedMerchant.cashbackPercent / 100.0))

  val infiniteTransition = rememberInfiniteTransition(label = "qrScanLine")
  val laserProgress by
    infiniteTransition.animateFloat(
      initialValue = 0.1f,
      targetValue = 0.9f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(1600, easing = LinearEasing),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "laserY",
    )

  Column(
    modifier =
      Modifier.fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 16.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column {
        Text(
          text = "QR Code Payment",
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = "Scan BanglaQR / TakaPay Merchant or share your QR",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      FilterChip(
        selected = showMyPersonalQr,
        onClick = { showMyPersonalQr = !showMyPersonalQr },
        label = { Text(if (showMyPersonalQr) "Scan Merchant" else "My QR Code") },
        leadingIcon = {
          Icon(
            imageVector = if (showMyPersonalQr) Icons.Default.QrCodeScanner else Icons.Default.QrCode2,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
        },
        modifier = Modifier.testTag("toggle_my_qr_chip"),
      )
    }

    Spacer(modifier = Modifier.height(16.dp))

    if (showMyPersonalQr) {
      // Personal TakaPay Receive Money QR Card
      Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Text(
            text = wallet.displayName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "TakaPay A/C: ${wallet.accountNumber} • ${wallet.kycTier}",
            style = MaterialTheme.typography.bodyMedium,
            color = TakaMagenta,
            fontWeight = FontWeight.SemiBold,
          )
          Spacer(modifier = Modifier.height(20.dp))

          Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 6.dp,
            modifier = Modifier.size(220.dp).padding(8.dp),
          ) {
            QrMatrixCanvas(
              seedText = wallet.accountNumber,
              modifier = Modifier.fillMaxSize().padding(16.dp),
            )
          }

          Spacer(modifier = Modifier.height(14.dp))
          Text(
            text = "Anyone with TakaPay, bKash, or BanglaQR can scan this code to send you funds instantly.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    } else {
      // Interactive QR Viewfinder Frame + Instant Merchant Presets
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .height(190.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(TakaPlumSurface),
        contentAlignment = Alignment.Center,
      ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(24.dp)) {
          val w = size.width
          val h = size.height
          val cornerLen = 36.dp.toPx()
          val strokeWidth = 4.dp.toPx()

          // Draw viewfinder corner brackets
          drawRoundRect(
            color = Color.White.copy(alpha = 0.22f),
            style = Stroke(width = 1.5.dp.toPx()),
            cornerRadius = CornerRadius(18.dp.toPx()),
          )

          // Animated laser line
          val laserY = h * laserProgress
          drawLine(
            brush =
              Brush.horizontalGradient(
                listOf(Color.Transparent, TakaGold, TakaMagenta, TakaGold, Color.Transparent)
              ),
            start = Offset(16f, laserY),
            end = Offset(w - 16f, laserY),
            strokeWidth = strokeWidth,
          )

          // Corner highlights
          drawLine(TakaGold, Offset(0f, 0f), Offset(cornerLen, 0f), strokeWidth)
          drawLine(TakaGold, Offset(0f, 0f), Offset(0f, cornerLen), strokeWidth)
          drawLine(TakaGold, Offset(w, 0f), Offset(w - cornerLen, 0f), strokeWidth)
          drawLine(TakaGold, Offset(w, 0f), Offset(w, cornerLen), strokeWidth)
          drawLine(TakaGold, Offset(0f, h), Offset(cornerLen, h), strokeWidth)
          drawLine(TakaGold, Offset(0f, h), Offset(0f, h - cornerLen), strokeWidth)
          drawLine(TakaGold, Offset(w, h), Offset(w - cornerLen, h), strokeWidth)
          drawLine(TakaGold, Offset(w, h), Offset(w, h - cornerLen), strokeWidth)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
            imageVector = Icons.Default.QrCodeScanner,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(42.dp),
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Active QR Target: ${selectedMerchant.merchantCode}",
            style = MaterialTheme.typography.labelLarge,
            color = TakaGold,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "Tap any Partner Merchant QR below or enter code manually",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.8f),
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = "Verified BanglaQR Partner Merchants (Instant Cashback)",
        style = MaterialTheme.typography.titleSmall,
        color = TakaMagenta,
        fontWeight = FontWeight.Bold,
      )
      Spacer(modifier = Modifier.height(8.dp))

      MfsPresets.merchantQrPresets.chunked(2).forEach { rowMerchants ->
        Row(
          modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          rowMerchants.forEach { merchant ->
            val isSelected = merchant.merchantCode == selectedMerchant.merchantCode
            Surface(
              shape = RoundedCornerShape(14.dp),
              color =
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
              modifier =
                Modifier.weight(1f).clickable {
                  selectedMerchant = merchant
                  merchantCodeInput = merchant.merchantCode
                  merchantNameInput = merchant.merchantName
                  amountText = merchant.suggestedAmount.toInt().toString()
                },
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Default.Storefront,
                    contentDescription = null,
                    tint = TakaMagenta,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = merchant.merchantName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                  )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "${merchant.cashbackPercent.toInt()}% Instant Cashback",
                  style = MaterialTheme.typography.labelSmall,
                  color = TakaEmerald,
                  fontWeight = FontWeight.Bold,
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      OutlinedTextField(
        value = merchantCodeInput,
        onValueChange = { merchantCodeInput = it },
        label = { Text("Merchant QR / Till Code") },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().testTag("qr_merchant_code_input"),
      )

      Spacer(modifier = Modifier.height(10.dp))

      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = amountText,
          onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
          label = { Text("Payment Amount (৳)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1f).testTag("qr_payment_amount_input"),
        )
        OutlinedTextField(
          value = invoiceNote,
          onValueChange = { invoiceNote = it },
          label = { Text("Invoice / Counter") },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1f),
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      Button(
        onClick = {
          onProceedToMerchantPayment(
            PendingTransactionDraft(
              type = TransactionType.MERCHANT_PAY,
              recipientNumber = merchantCodeInput,
              recipientName = merchantNameInput,
              operatorOrProvider = selectedMerchant.category,
              amountBdt = amountBdt,
              feeBdt = 0.0,
              cashbackBdt = cashbackBdt,
              referenceNote = invoiceNote,
            )
          )
        },
        enabled = amountBdt > 0.0 && merchantCodeInput.isNotBlank(),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = TakaMagenta),
        modifier = Modifier.fillMaxWidth().height(54.dp).testTag("qr_pay_proceed_button"),
      ) {
        Icon(Icons.Default.QrCodeScanner, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Pay ৳${TakaPayRepository.formatBdtPlain(amountBdt)} (+৳${TakaPayRepository.formatBdtPlain(cashbackBdt)} Cashback)",
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}

@Composable
private fun QrMatrixCanvas(seedText: String, modifier: Modifier = Modifier) {
  Canvas(modifier = modifier) {
    val gridSize = 15
    val cellSize = size.width / gridSize
    val hash = seedText.hashCode()

    for (r in 0 until gridSize) {
      for (c in 0 until gridSize) {
        val isFinderTopLeft = r < 4 && c < 4
        val isFinderTopRight = r < 4 && c >= gridSize - 4
        val isFinderBottomLeft = r >= gridSize - 4 && c < 4
        val bit = ((hash shr ((r * gridSize + c) % 28)) and 1) == 1
        if (isFinderTopLeft || isFinderTopRight || isFinderBottomLeft || bit) {
          drawRect(
            color = if (isFinderTopLeft || isFinderTopRight || isFinderBottomLeft) TakaMagentaDark else Color(0xFF1F1A1C),
            topLeft = Offset(c * cellSize, r * cellSize),
            size = Size(cellSize * 0.88f, cellSize * 0.88f),
          )
        }
      }
    }
  }
}

@Composable
fun SavingsVaultScreen(
  wallet: WalletProfile,
  goals: List<SavingsGoal>,
  onCreateNewVaultClick: () -> Unit,
  onTransferVaultClick: (goal: SavingsGoal, isDeposit: Boolean) -> Unit,
) {
  val averageRate =
    if (goals.isNotEmpty()) goals.map { it.interestRatePercent }.average() else 9.0
  val projectedYearlyProfit =
    TakaPayRepository.roundCurrency(wallet.totalSavingsBdt * (averageRate / 100.0))

  Scaffold(
    floatingActionButton = {
      ExtendedFloatingActionButton(
        onClick = onCreateNewVaultClick,
        containerColor = TakaMagenta,
        contentColor = Color.White,
        icon = { Icon(Icons.Default.Add, contentDescription = null) },
        text = { Text("New DPS / Vault", fontWeight = FontWeight.Bold) },
        modifier = Modifier.testTag("create_savings_fab"),
      )
    }
  ) { innerPadding ->
    LazyColumn(
      modifier =
        Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 20.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      item {
        Spacer(modifier = Modifier.height(8.dp))
        // Savings Summary Hero Card
        Card(
          shape = RoundedCornerShape(24.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .background(
                  Brush.linearGradient(
                    colors = listOf(Color(0xFF064E3B), TakaEmerald)
                  )
                )
                .padding(22.dp)
          ) {
            Column {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = "REAL-TIME SAVINGS & DPS PORTFOLIO",
                  style = MaterialTheme.typography.labelMedium,
                  color = Color.White.copy(alpha = 0.85f),
                  fontWeight = FontWeight.Bold,
                )
                Icon(
                  imageVector = Icons.Default.TrendingUp,
                  contentDescription = null,
                  tint = TakaGold,
                )
              }
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "৳${TakaPayRepository.formatBdtPlain(wallet.totalSavingsBdt)}",
                style = MaterialTheme.typography.displayMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "Est. Annual Profit: +৳${TakaPayRepository.formatBdtPlain(projectedYearlyProfit)} (${String.format(java.util.Locale.US, "%.1f", averageRate)}% avg p.a.)",
                style = MaterialTheme.typography.bodySmall,
                color = TakaGold,
                fontWeight = FontWeight.SemiBold,
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "Liquid Wallet Balance Available: ৳${TakaPayRepository.formatBdtPlain(wallet.balanceBdt)}",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.9f),
              )
            }
          }
        }
      }

      if (goals.isEmpty()) {
        item {
          Card(
            shape = RoundedCornerShape(20.dp),
            colors =
              CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Icon(
                imageVector = Icons.Default.Savings,
                contentDescription = null,
                tint = TakaMagenta,
                modifier = Modifier.size(48.dp),
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "Start Your First Real-Time DPS Vault",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "Earn up to 9.2% p.a. with instant deposit and flexible encashment.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      } else {
        items(goals, key = { it.id }) { goal ->
          val progress =
            if (goal.targetAmountBdt > 0) {
              (goal.savedAmountBdt / goal.targetAmountBdt).toFloat().coerceIn(0f, 1f)
            } else 0f

          Card(
            shape = RoundedCornerShape(20.dp),
            colors =
              CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth().testTag("savings_goal_card_${goal.id}"),
          ) {
            Column(modifier = Modifier.padding(18.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = goal.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text = "${goal.institutionName} • ${goal.category}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = TakaEmerald.copy(alpha = 0.15f),
                ) {
                  Text(
                    text = "${goal.interestRatePercent}% p.a.",
                    style = MaterialTheme.typography.labelMedium,
                    color = TakaEmerald,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                  )
                }
              }

              Spacer(modifier = Modifier.height(12.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                  text = "Saved: ৳${TakaPayRepository.formatBdtPlain(goal.savedAmountBdt)}",
                  style = MaterialTheme.typography.titleSmall,
                  color = TakaMagenta,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = "Target: ৳${TakaPayRepository.formatBdtPlain(goal.targetAmountBdt)}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }

              Spacer(modifier = Modifier.height(8.dp))

              LinearProgressIndicator(
                progress = { progress },
                color = TakaEmerald,
                trackColor = MaterialTheme.colorScheme.surface,
                modifier =
                  Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
              )

              Spacer(modifier = Modifier.height(14.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Button(
                  onClick = { onTransferVaultClick(goal, true) },
                  shape = RoundedCornerShape(12.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = TakaEmerald),
                  modifier = Modifier.weight(1f).testTag("deposit_goal_${goal.id}"),
                ) {
                  Icon(
                    Icons.Default.ArrowUpward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Deposit", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                  onClick = { onTransferVaultClick(goal, false) },
                  enabled = goal.savedAmountBdt > 0.0,
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.weight(1f).testTag("withdraw_goal_${goal.id}"),
                ) {
                  Icon(
                    Icons.Default.ArrowDownward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Encash", fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }

      item { Spacer(modifier = Modifier.height(88.dp)) }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSavingsGoalSheet(
  wallet: WalletProfile,
  onProceedToAuth: (
    title: String,
    category: String,
    targetAmount: Double,
    initialDeposit: Double,
    monthlyDeposit: Double,
    interestRate: Double,
    institution: String,
    autoRoundUp: Boolean,
  ) -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var title by remember { mutableStateOf("") }
  val categories = listOf("DPS", "Emergency", "Festival", "Education", "Gadget", "Travel")
  var selectedCategory by remember { mutableStateOf("DPS") }
  var selectedInstitution by remember { mutableStateOf(MfsPresets.savingsInstitutions.first()) }
  var targetText by remember { mutableStateOf("50000") }
  var initialDepositText by remember { mutableStateOf("1000") }
  var monthlyDepositText by remember { mutableStateOf("2000") }
  var autoRoundUp by remember { mutableStateOf(true) }

  val targetBdt = targetText.toDoubleOrNull() ?: 0.0
  val initialBdt = initialDepositText.toDoubleOrNull() ?: 0.0
  val monthlyBdt = monthlyDepositText.toDoubleOrNull() ?: 0.0

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 24.dp)
          .padding(bottom = 28.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Open New DPS / Savings Vault",
          style = MaterialTheme.typography.titleLarge,
          color = TakaMagenta,
          fontWeight = FontWeight.Bold,
        )
        IconButton(onClick = onDismiss) {
          Icon(Icons.Default.Close, contentDescription = "Close")
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      OutlinedTextField(
        value = title,
        onValueChange = { title = it },
        label = { Text("Vault / Goal Name") },
        placeholder = { Text("e.g. Hajj Fund, Laptop Goal, Eid DPS") },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().testTag("new_goal_title_input"),
      )

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        categories.take(4).forEach { cat ->
          FilterChip(
            selected = cat == selectedCategory,
            onClick = { selectedCategory = cat },
            label = { Text(cat) },
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      MfsPresets.savingsInstitutions.forEach { inst ->
        val isSelected = inst == selectedInstitution
        Surface(
          shape = RoundedCornerShape(12.dp),
          color =
            if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
          modifier =
            Modifier.fillMaxWidth()
              .padding(vertical = 3.dp)
              .clickable { selectedInstitution = inst },
        ) {
          Text(
            text = inst,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(12.dp),
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = targetText,
          onValueChange = { targetText = it.filter { ch -> ch.isDigit() || ch == '.' } },
          label = { Text("Target (৳)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
          value = initialDepositText,
          onValueChange = { initialDepositText = it.filter { ch -> ch.isDigit() || ch == '.' } },
          label = { Text("Initial Deposit (৳)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1f).testTag("new_goal_initial_deposit_input"),
        )
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 6.dp),
      ) {
        Checkbox(checked = autoRoundUp, onCheckedChange = { autoRoundUp = it })
        Text(
          text = "Enable Auto-Save Spare Change on Merchant QR Payments",
          style = MaterialTheme.typography.bodySmall,
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      Button(
        onClick = {
          onProceedToAuth(
            title.ifBlank { "$selectedCategory Savings Vault" },
            selectedCategory,
            targetBdt,
            initialBdt,
            monthlyBdt,
            9.0,
            selectedInstitution,
            autoRoundUp,
          )
        },
        enabled = targetBdt > 0.0 && initialBdt <= wallet.balanceBdt,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = TakaEmerald),
        modifier = Modifier.fillMaxWidth().height(54.dp).testTag("confirm_new_goal_button"),
      ) {
        Text(
          text = "Open Vault & Deposit ৳${TakaPayRepository.formatBdtPlain(initialBdt)}",
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferSavingsVaultSheet(
  wallet: WalletProfile,
  goal: SavingsGoal,
  isDeposit: Boolean,
  onProceedToAuth: (amountBdt: Double) -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var amountText by remember {
    mutableStateOf(
      if (isDeposit) goal.monthlyDepositBdt.toInt().coerceAtLeast(500).toString()
      else (goal.savedAmountBdt.toInt().coerceAtMost(1000)).toString()
    )
  }
  val amountBdt = amountText.toDoubleOrNull() ?: 0.0
  val maxAllowed = if (isDeposit) wallet.balanceBdt else goal.savedAmountBdt

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 28.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = if (isDeposit) "Deposit to ${goal.title}" else "Encash from ${goal.title}",
            style = MaterialTheme.typography.titleLarge,
            color = TakaMagenta,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text =
              if (isDeposit) "Available Wallet Balance: ৳${TakaPayRepository.formatBdtPlain(wallet.balanceBdt)}"
              else "Available in Vault: ৳${TakaPayRepository.formatBdtPlain(goal.savedAmountBdt)}",
            style = MaterialTheme.typography.bodySmall,
          )
        }
        IconButton(onClick = onDismiss) {
          Icon(Icons.Default.Close, contentDescription = "Close")
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      OutlinedTextField(
        value = amountText,
        onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
        label = { Text("Amount (BDT ৳)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().testTag("vault_transfer_amount_input"),
      )

      Spacer(modifier = Modifier.height(10.dp))

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(500, 1000, 2000, 5000).forEach { preset ->
          AssistChip(
            onClick = { amountText = preset.toString() },
            label = { Text("৳$preset") },
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      Button(
        onClick = { onProceedToAuth(amountBdt) },
        enabled = amountBdt > 0.0 && amountBdt <= maxAllowed,
        shape = RoundedCornerShape(14.dp),
        colors =
          ButtonDefaults.buttonColors(
            containerColor = if (isDeposit) TakaEmerald else TakaMagenta
          ),
        modifier = Modifier.fillMaxWidth().height(54.dp).testTag("vault_transfer_proceed_button"),
      ) {
        Text(
          text =
            if (isDeposit) "Authorize Vault Deposit (৳${TakaPayRepository.formatBdtPlain(amountBdt)})"
            else "Encash to Main Wallet (৳${TakaPayRepository.formatBdtPlain(amountBdt)})",
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}
