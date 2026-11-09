package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.MfsPresets
import com.example.data.model.MfsTransaction
import com.example.data.model.SavedBiller
import com.example.data.model.TransactionType
import com.example.data.model.WalletProfile
import com.example.data.repository.TakaPayRepository
import com.example.ui.theme.TakaEmerald
import com.example.ui.theme.TakaGoldDark
import com.example.ui.theme.TakaMagenta
import com.example.viewmodel.TakaPayViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PendingTransactionDraft(
  val type: TransactionType,
  val recipientNumber: String,
  val recipientName: String,
  val operatorOrProvider: String,
  val amountBdt: Double,
  val feeBdt: Double,
  val cashbackBdt: Double,
  val referenceNote: String,
  val saveAsFavorite: Boolean = false,
  val favoriteBillerType: String = "CONTACT",
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SendMoneySheet(
  wallet: WalletProfile,
  savedContacts: List<SavedBiller>,
  onProceedToAuth: (PendingTransactionDraft) -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var recipientNumber by remember { mutableStateOf("") }
  var recipientName by remember { mutableStateOf("") }
  var amountText by remember { mutableStateOf("") }
  var referenceNote by remember { mutableStateOf("") }
  var saveContact by remember { mutableStateOf(false) }

  val amountBdt = amountText.toDoubleOrNull() ?: 0.0
  val feeBdt = TakaPayViewModel.calculateFee(TransactionType.SEND_MONEY, amountBdt)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 24.dp)
          .padding(bottom = 28.dp),
    ) {
      SheetHeader(
        title = "Send Money (P2P Transfer)",
        subtitle = "Available: ৳${TakaPayRepository.formatBdtPlain(wallet.balanceBdt)} • Free up to ৳500",
        onDismiss = onDismiss,
      )

      if (savedContacts.isNotEmpty()) {
        Text(
          text = "Priyo Numbers & Recent Contacts",
          style = MaterialTheme.typography.labelLarge,
          color = TakaMagenta,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          savedContacts.forEach { contact ->
            AssistChip(
              onClick = {
                recipientNumber = contact.accountOrMeterNo
                recipientName = contact.name
              },
              label = { Text("${contact.name} (${contact.accountOrMeterNo})") },
              leadingIcon = {
                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
              },
            )
          }
        }
        Spacer(modifier = Modifier.height(12.dp))
      }

      OutlinedTextField(
        value = recipientNumber,
        onValueChange = { recipientNumber = it },
        label = { Text("Recipient TakaPay / Mobile Number") },
        placeholder = { Text("e.g. 01712-908172") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().testTag("send_money_recipient_input"),
      )

      Spacer(modifier = Modifier.height(10.dp))

      OutlinedTextField(
        value = recipientName,
        onValueChange = { recipientName = it },
        label = { Text("Recipient Name") },
        placeholder = { Text("e.g. Tanvir Ahmed") },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().testTag("send_money_name_input"),
      )

      Spacer(modifier = Modifier.height(10.dp))

      OutlinedTextField(
        value = amountText,
        onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
        label = { Text("Amount (BDT ৳)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().testTag("send_money_amount_input"),
      )

      Spacer(modifier = Modifier.height(8.dp))

      QuickAmountRow(
        amounts = listOf(100, 500, 1000, 2500, 5000),
        onSelect = { amountText = it.toString() },
      )

      Spacer(modifier = Modifier.height(10.dp))

      OutlinedTextField(
        value = referenceNote,
        onValueChange = { referenceNote = it },
        label = { Text("Reference / Short Note (Optional)") },
        placeholder = { Text("e.g. Dinner bill, Family support") },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
      )

      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 6.dp),
      ) {
        Checkbox(checked = saveContact, onCheckedChange = { saveContact = it })
        Text(
          text = "Save as Priyo Contact for 1-Tap Send Money",
          style = MaterialTheme.typography.bodySmall,
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      Button(
        onClick = {
          onProceedToAuth(
            PendingTransactionDraft(
              type = TransactionType.SEND_MONEY,
              recipientNumber = recipientNumber.ifBlank { "01712-908172" },
              recipientName = recipientName.ifBlank { recipientNumber.ifBlank { "Priyo Contact" } },
              operatorOrProvider = "TakaPay P2P",
              amountBdt = amountBdt,
              feeBdt = feeBdt,
              cashbackBdt = 0.0,
              referenceNote = referenceNote,
              saveAsFavorite = saveContact,
              favoriteBillerType = "CONTACT",
            )
          )
        },
        enabled = amountBdt > 0.0 && recipientNumber.isNotBlank(),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = TakaMagenta),
        modifier = Modifier.fillMaxWidth().height(54.dp).testTag("send_money_proceed_button"),
      ) {
        Icon(Icons.Default.Fingerprint, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Proceed with Biometric / PIN (Fee: ৳${TakaPayRepository.formatBdtPlain(feeBdt)})",
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMoneySheet(
  wallet: WalletProfile,
  onProceedToAuth: (PendingTransactionDraft) -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var selectedSource by remember { mutableStateOf(MfsPresets.depositSources.first()) }
  var amountText by remember { mutableStateOf("2000") }
  var referenceNote by remember { mutableStateOf("Bank to TakaPay Top-Up") }

  val amountBdt = amountText.toDoubleOrNull() ?: 0.0

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 24.dp)
          .padding(bottom = 28.dp),
    ) {
      SheetHeader(
        title = "Add Money (Deposit to Wallet)",
        subtitle = "Instant 0-Fee Deposit from Linked Bank, Card, or MFS Bridge",
        onDismiss = onDismiss,
      )

      Text(
        text = "Select Funding Source",
        style = MaterialTheme.typography.labelLarge,
        color = TakaMagenta,
      )
      Spacer(modifier = Modifier.height(8.dp))

      MfsPresets.depositSources.forEach { source ->
        val isSelected = source == selectedSource
        Surface(
          shape = RoundedCornerShape(12.dp),
          color =
            if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
          modifier =
            Modifier.fillMaxWidth()
              .padding(vertical = 4.dp)
              .clickable { selectedSource = source },
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Default.AccountBalance,
              contentDescription = null,
              tint = if (isSelected) TakaMagenta else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
              text = source,
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      OutlinedTextField(
        value = amountText,
        onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
        label = { Text("Deposit Amount (BDT ৳)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().testTag("add_money_amount_input"),
      )

      Spacer(modifier = Modifier.height(8.dp))
      QuickAmountRow(
        amounts = listOf(500, 1000, 2000, 5000, 10000),
        onSelect = { amountText = it.toString() },
      )

      Spacer(modifier = Modifier.height(10.dp))

      OutlinedTextField(
        value = referenceNote,
        onValueChange = { referenceNote = it },
        label = { Text("Note") },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
      )

      Spacer(modifier = Modifier.height(16.dp))

      Button(
        onClick = {
          onProceedToAuth(
            PendingTransactionDraft(
              type = TransactionType.ADD_MONEY,
              recipientNumber = wallet.accountNumber,
              recipientName = wallet.displayName,
              operatorOrProvider = selectedSource,
              amountBdt = amountBdt,
              feeBdt = 0.0,
              cashbackBdt = 0.0,
              referenceNote = referenceNote,
            )
          )
        },
        enabled = amountBdt > 0.0,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = TakaEmerald),
        modifier = Modifier.fillMaxWidth().height(54.dp).testTag("add_money_proceed_button"),
      ) {
        Icon(Icons.Default.Fingerprint, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Authorize Deposit of ৳${TakaPayRepository.formatBdtPlain(amountBdt)}",
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashOutSheet(
  wallet: WalletProfile,
  onProceedToAuth: (PendingTransactionDraft) -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var selectedAgent by remember { mutableStateOf(MfsPresets.cashOutAgents.first()) }
  var customAgentNumber by remember { mutableStateOf("01715-882910") }
  var amountText by remember { mutableStateOf("1000") }

  val amountBdt = amountText.toDoubleOrNull() ?: 0.0
  val feeBdt = TakaPayViewModel.calculateFee(TransactionType.CASH_OUT, amountBdt)

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 24.dp)
          .padding(bottom = 28.dp),
    ) {
      SheetHeader(
        title = "Cash Out (Agent & ATM)",
        subtitle = "Priyo Agent Rate: 1.49% (৳14.90 per ৳1,000) • Available: ৳${TakaPayRepository.formatBdtPlain(wallet.balanceBdt)}",
        onDismiss = onDismiss,
      )

      Text(
        text = "Select Nearby Verified Agent / ATM Booth",
        style = MaterialTheme.typography.labelLarge,
        color = TakaMagenta,
      )
      Spacer(modifier = Modifier.height(6.dp))

      MfsPresets.cashOutAgents.forEach { agent ->
        val isSelected = agent == selectedAgent
        Surface(
          shape = RoundedCornerShape(12.dp),
          color =
            if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
          modifier =
            Modifier.fillMaxWidth()
              .padding(vertical = 4.dp)
              .clickable {
                selectedAgent = agent
                customAgentNumber =
                  agent.substringAfter("(", "01715-882910").substringBefore(")")
              },
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Default.LocalAtm,
              contentDescription = null,
              tint = if (isSelected) TakaMagenta else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = agent,
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      OutlinedTextField(
        value = customAgentNumber,
        onValueChange = { customAgentNumber = it },
        label = { Text("Agent Account Number") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().testTag("cash_out_agent_input"),
      )

      Spacer(modifier = Modifier.height(10.dp))

      OutlinedTextField(
        value = amountText,
        onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
        label = { Text("Cash Out Amount (BDT ৳)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().testTag("cash_out_amount_input"),
      )

      Spacer(modifier = Modifier.height(8.dp))
      QuickAmountRow(
        amounts = listOf(500, 1000, 2000, 5000, 10000),
        onSelect = { amountText = it.toString() },
      )

      Spacer(modifier = Modifier.height(16.dp))

      Button(
        onClick = {
          onProceedToAuth(
            PendingTransactionDraft(
              type = TransactionType.CASH_OUT,
              recipientNumber = customAgentNumber,
              recipientName = selectedAgent.substringBefore(" ("),
              operatorOrProvider = "TakaPay Priyo Agent",
              amountBdt = amountBdt,
              feeBdt = feeBdt,
              cashbackBdt = 0.0,
              referenceNote = "Agent Cash Out",
            )
          )
        },
        enabled = amountBdt > 0.0 && customAgentNumber.isNotBlank(),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = TakaMagenta),
        modifier = Modifier.fillMaxWidth().height(54.dp).testTag("cash_out_proceed_button"),
      ) {
        Icon(Icons.Default.Fingerprint, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Cash Out ৳${TakaPayRepository.formatBdtPlain(amountBdt)} (Charge: ৳${TakaPayRepository.formatBdtPlain(feeBdt)})",
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileRechargeSheet(
  wallet: WalletProfile,
  onProceedToAuth: (PendingTransactionDraft) -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var selectedOperator by remember { mutableStateOf(MfsPresets.telcoOperators.first()) }
  var mobileNumber by remember { mutableStateOf(wallet.accountNumber) }
  var amountText by remember { mutableStateOf("100") }
  var cashbackOverride by remember { mutableStateOf(0.0) }
  var selectedBundleName by remember { mutableStateOf("Prepaid Top-Up") }

  val amountBdt = amountText.toDoubleOrNull() ?: 0.0
  val calculatedCashback =
    if (cashbackOverride > 0.0) cashbackOverride
    else TakaPayRepository.roundCurrency(amountBdt * (selectedOperator.cashbackPercent / 100.0))

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 24.dp)
          .padding(bottom = 28.dp),
    ) {
      SheetHeader(
        title = "Mobile Recharge & Bundles",
        subtitle = "Instant Top-Up & 4G Data Packs with Cashback",
        onDismiss = onDismiss,
      )

      Text(
        text = "Select Mobile Operator",
        style = MaterialTheme.typography.labelLarge,
        color = TakaMagenta,
      )
      Spacer(modifier = Modifier.height(6.dp))

      Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        MfsPresets.telcoOperators.forEach { op ->
          FilterChip(
            selected = op.name == selectedOperator.name,
            onClick = {
              selectedOperator = op
              cashbackOverride = 0.0
              selectedBundleName = "Prepaid Top-Up"
            },
            label = { Text("${op.name} (${op.prefixHint})") },
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      OutlinedTextField(
        value = mobileNumber,
        onValueChange = { mobileNumber = it },
        label = { Text("Mobile Number") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().testTag("recharge_number_input"),
      )

      Spacer(modifier = Modifier.height(10.dp))

      OutlinedTextField(
        value = amountText,
        onValueChange = {
          amountText = it.filter { ch -> ch.isDigit() || ch == '.' }
          cashbackOverride = 0.0
          selectedBundleName = "Direct Recharge"
        },
        label = { Text("Recharge Amount (BDT ৳)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().testTag("recharge_amount_input"),
      )

      Spacer(modifier = Modifier.height(8.dp))
      QuickAmountRow(
        amounts = listOf(20, 50, 100, 200, 500),
        onSelect = {
          amountText = it.toString()
          cashbackOverride = 0.0
          selectedBundleName = "Direct Recharge"
        },
      )

      Spacer(modifier = Modifier.height(14.dp))
      Text(
        text = "${selectedOperator.name} Exclusive Internet & Voice Offers",
        style = MaterialTheme.typography.labelLarge,
        color = TakaMagenta,
      )
      Spacer(modifier = Modifier.height(8.dp))

      selectedOperator.bundles.forEach { bundle ->
        Card(
          shape = RoundedCornerShape(14.dp),
          colors =
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
          modifier =
            Modifier.fillMaxWidth()
              .padding(vertical = 4.dp)
              .clickable {
                amountText = bundle.amountBdt.toInt().toString()
                cashbackOverride = bundle.cashbackBdt
                selectedBundleName = bundle.label
              },
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = bundle.label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "Validity: ${bundle.validity}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Column(horizontalAlignment = Alignment.End) {
              Text(
                text = "৳${bundle.amountBdt.toInt()}",
                style = MaterialTheme.typography.titleMedium,
                color = TakaMagenta,
                fontWeight = FontWeight.Bold,
              )
              if (bundle.cashbackBdt > 0) {
                Text(
                  text = "+৳${bundle.cashbackBdt.toInt()} Cashback",
                  style = MaterialTheme.typography.labelSmall,
                  color = TakaEmerald,
                  fontWeight = FontWeight.Bold,
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      Button(
        onClick = {
          onProceedToAuth(
            PendingTransactionDraft(
              type = TransactionType.MOBILE_RECHARGE,
              recipientNumber = mobileNumber,
              recipientName = "${selectedOperator.name} ($mobileNumber)",
              operatorOrProvider = selectedOperator.name,
              amountBdt = amountBdt,
              feeBdt = 0.0,
              cashbackBdt = calculatedCashback,
              referenceNote = selectedBundleName,
            )
          )
        },
        enabled = amountBdt >= 10.0 && mobileNumber.isNotBlank(),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = TakaMagenta),
        modifier = Modifier.fillMaxWidth().height(54.dp).testTag("recharge_proceed_button"),
      ) {
        Icon(Icons.Default.PhoneAndroid, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Recharge ৳${TakaPayRepository.formatBdtPlain(amountBdt)} (Cashback: ৳${TakaPayRepository.formatBdtPlain(calculatedCashback)})",
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UtilityBillSheet(
  wallet: WalletProfile,
  savedBillers: List<SavedBiller>,
  onProceedToAuth: (PendingTransactionDraft) -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val categories = listOf("All", "Electricity", "Gas", "Water", "Internet", "TV & Education")
  var selectedCategory by remember { mutableStateOf("All") }
  val filteredProviders =
    MfsPresets.utilityProviders.filter {
      selectedCategory == "All" || it.category == selectedCategory
    }
  var selectedProvider by remember { mutableStateOf(MfsPresets.utilityProviders.first()) }
  var meterOrAccountNo by remember { mutableStateOf(selectedProvider.sampleBillNo) }
  var amountText by remember { mutableStateOf(selectedProvider.defaultAmount.toInt().toString()) }
  var billingPeriod by remember { mutableStateOf("Nov 2026") }
  var saveBiller by remember { mutableStateOf(false) }

  val amountBdt = amountText.toDoubleOrNull() ?: 0.0
  val feeBdt = TakaPayViewModel.calculateFee(TransactionType.UTILITY_BILL, amountBdt)

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 24.dp)
          .padding(bottom = 28.dp),
    ) {
      SheetHeader(
        title = "Pay Utility Bill",
        subtitle = "Electricity, Gas, Water, Internet & DTH with Digital Receipt",
        onDismiss = onDismiss,
      )

      if (savedBillers.isNotEmpty()) {
        Text(
          text = "Saved Utility Accounts (1-Tap Fill)",
          style = MaterialTheme.typography.labelLarge,
          color = TakaMagenta,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          savedBillers.forEach { biller ->
            AssistChip(
              onClick = {
                meterOrAccountNo = biller.accountOrMeterNo
                MfsPresets.utilityProviders
                  .firstOrNull {
                    it.name.contains(biller.name.take(5), ignoreCase = true) ||
                      it.category.equals(biller.providerCategory, ignoreCase = true)
                  }
                  ?.let {
                    selectedProvider = it
                    amountText = it.defaultAmount.toInt().toString()
                  }
              },
              label = { Text("${biller.name} (#${biller.accountOrMeterNo})") },
              leadingIcon = {
                Icon(
                  Icons.Default.ElectricBolt,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
              },
            )
          }
        }
        Spacer(modifier = Modifier.height(10.dp))
      }

      Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        categories.forEach { cat ->
          FilterChip(
            selected = cat == selectedCategory,
            onClick = { selectedCategory = cat },
            label = { Text(cat) },
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      filteredProviders.forEach { provider ->
        val isSelected = provider.name == selectedProvider.name
        Surface(
          shape = RoundedCornerShape(12.dp),
          color =
            if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
          modifier =
            Modifier.fillMaxWidth()
              .padding(vertical = 3.dp)
              .clickable {
                selectedProvider = provider
                meterOrAccountNo = provider.sampleBillNo
                amountText = provider.defaultAmount.toInt().toString()
              },
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column {
              Text(
                text = provider.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "${provider.category} • ${provider.meterHint}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Text(
              text = "৳${provider.defaultAmount.toInt()}",
              style = MaterialTheme.typography.labelLarge,
              color = TakaMagenta,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      OutlinedTextField(
        value = meterOrAccountNo,
        onValueChange = { meterOrAccountNo = it },
        label = { Text(selectedProvider.meterHint) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().testTag("utility_meter_input"),
      )

      Spacer(modifier = Modifier.height(10.dp))

      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = amountText,
          onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
          label = { Text("Bill Amount (৳)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1f).testTag("utility_amount_input"),
        )
        OutlinedTextField(
          value = billingPeriod,
          onValueChange = { billingPeriod = it },
          label = { Text("Billing Month") },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1f),
        )
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 6.dp),
      ) {
        Checkbox(checked = saveBiller, onCheckedChange = { saveBiller = it })
        Text(
          text = "Save Biller Account for future 1-Tap Bill Pay",
          style = MaterialTheme.typography.bodySmall,
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      Button(
        onClick = {
          onProceedToAuth(
            PendingTransactionDraft(
              type = TransactionType.UTILITY_BILL,
              recipientNumber = meterOrAccountNo,
              recipientName = "${selectedProvider.name} (#$meterOrAccountNo)",
              operatorOrProvider = selectedProvider.category,
              amountBdt = amountBdt,
              feeBdt = feeBdt,
              cashbackBdt = 0.0,
              referenceNote = "Bill Month: $billingPeriod",
              saveAsFavorite = saveBiller,
              favoriteBillerType = "UTILITY",
            )
          )
        },
        enabled = amountBdt > 0.0 && meterOrAccountNo.isNotBlank(),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = TakaMagenta),
        modifier = Modifier.fillMaxWidth().height(54.dp).testTag("utility_proceed_button"),
      ) {
        Icon(Icons.Default.ReceiptLong, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Pay Bill ৳${TakaPayRepository.formatBdtPlain(amountBdt)} (Fee: ৳${TakaPayRepository.formatBdtPlain(feeBdt)})",
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}

@Composable
fun MfsReceiptDialog(
  transaction: MfsTransaction,
  onDismiss: () -> Unit,
) {
  val txType = TransactionType.fromCode(transaction.type)
  val formattedTime = remember(transaction.createdAt) {
    val date = transaction.createdAt?.toDate() ?: Date()
    SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(date)
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    confirmButton = {
      Button(
        onClick = onDismiss,
        colors = ButtonDefaults.buttonColors(containerColor = TakaMagenta),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().testTag("receipt_done_button"),
      ) {
        Text("Done", fontWeight = FontWeight.Bold)
      }
    },
    title = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Box(
          modifier =
            Modifier.size(56.dp).clip(CircleShape).background(TakaEmerald.copy(alpha = 0.15f)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Transaction Successful",
            tint = TakaEmerald,
            modifier = Modifier.size(36.dp),
          )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "${txType.displayName} Successful",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = "TrxID: ${transaction.trxId}",
          style = MaterialTheme.typography.labelLarge,
          color = TakaMagenta,
          fontWeight = FontWeight.Bold,
        )
      }
    },
    text = {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          ReceiptRow("Recipient / Account", transaction.recipientName)
          ReceiptRow("Number / ID", transaction.recipientNumber)
          ReceiptRow("Provider", transaction.operatorOrProvider)
          HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
          ReceiptRow("Amount", "৳${TakaPayRepository.formatBdtPlain(transaction.amountBdt)}", true)
          ReceiptRow("Charge / Fee", "৳${TakaPayRepository.formatBdtPlain(transaction.feeBdt)}")
          if (transaction.cashbackBdt > 0) {
            ReceiptRow(
              "Cashback Credited",
              "+৳${TakaPayRepository.formatBdtPlain(transaction.cashbackBdt)}",
              true,
            )
          }
          ReceiptRow(
            "New Wallet Balance",
            "৳${TakaPayRepository.formatBdtPlain(transaction.balanceAfterBdt)}",
            true,
          )
          HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
          ReceiptRow("Secured Via", "${transaction.authMethod} VERIFIED")
          ReceiptRow("Time", formattedTime)
          if (transaction.referenceNote.isNotBlank()) {
            ReceiptRow("Reference", transaction.referenceNote)
          }
        }
      }
    },
  )
}

@Composable
private fun ReceiptRow(label: String, value: String, boldValue: Boolean = false) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.width(12.dp))
    Text(
      text = value,
      style = MaterialTheme.typography.bodySmall,
      fontWeight = if (boldValue) FontWeight.Bold else FontWeight.Medium,
    )
  }
}

@Composable
private fun SheetHeader(
  title: String,
  subtitle: String,
  onDismiss: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = TakaMagenta,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    IconButton(onClick = onDismiss) {
      Icon(Icons.Default.Close, contentDescription = "Close")
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickAmountRow(
  amounts: List<Int>,
  onSelect: (Int) -> Unit,
) {
  FlowRow(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    amounts.forEach { amt ->
      AssistChip(
        onClick = { onSelect(amt) },
        label = { Text("৳$amt", fontWeight = FontWeight.SemiBold) },
      )
    }
  }
}
