package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.model.MfsTransaction
import com.example.data.model.SavedBiller
import com.example.data.model.TransactionType
import com.example.data.model.WalletProfile
import com.example.data.repository.TakaPayRepository
import com.example.ui.theme.TakaEmerald
import com.example.ui.theme.TakaGold
import com.example.ui.theme.TakaMagenta
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StatementScreen(
  transactions: List<MfsTransaction>,
  onTransactionClick: (MfsTransaction) -> Unit,
) {
  var searchQuery by remember { mutableStateOf("") }
  val filterOptions =
    listOf("All", "Inflow", "Send Money", "Cash Out", "Recharge & Bill", "Merchant QR", "Savings")
  var selectedFilter by remember { mutableStateOf("All") }

  val filteredTransactions =
    remember(transactions, searchQuery, selectedFilter) {
      transactions.filter { tx ->
        val matchesQuery =
          searchQuery.isBlank() ||
            tx.title.contains(searchQuery, ignoreCase = true) ||
            tx.trxId.contains(searchQuery, ignoreCase = true) ||
            tx.recipientNumber.contains(searchQuery, ignoreCase = true) ||
            tx.recipientName.contains(searchQuery, ignoreCase = true)

        val matchesFilter =
          when (selectedFilter) {
            "Inflow" -> tx.type in listOf("ADD_MONEY", "SAVINGS_WITHDRAW")
            "Send Money" -> tx.type == "SEND_MONEY"
            "Cash Out" -> tx.type == "CASH_OUT"
            "Recharge & Bill" -> tx.type in listOf("MOBILE_RECHARGE", "UTILITY_BILL")
            "Merchant QR" -> tx.type == "MERCHANT_PAY"
            "Savings" -> tx.type in listOf("SAVINGS_DEPOSIT", "SAVINGS_WITHDRAW")
            else -> true
          }
        matchesQuery && matchesFilter
      }
    }

  val totalInflow =
    remember(transactions) {
      transactions
        .filter { TransactionType.fromCode(it.type).isCredit }
        .sumOf { it.amountBdt + it.cashbackBdt }
    }
  val totalOutflow =
    remember(transactions) {
      transactions
        .filter { !TransactionType.fromCode(it.type).isCredit }
        .sumOf { it.amountBdt + it.feeBdt }
    }
  val totalCashback = remember(transactions) { transactions.sumOf { it.cashbackBdt } }

  LazyColumn(
    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "Transaction Statement",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = "Tap any transaction to inspect its verified TrxID receipt",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(12.dp))

      // Analytics Summary Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        StatSummaryCard(
          label = "Total Inflow",
          value = "+৳${TakaPayRepository.formatBdtPlain(totalInflow)}",
          accentColor = TakaEmerald,
          modifier = Modifier.weight(1f),
        )
        StatSummaryCard(
          label = "Total Outflow",
          value = "-৳${TakaPayRepository.formatBdtPlain(totalOutflow)}",
          accentColor = TakaMagenta,
          modifier = Modifier.weight(1f),
        )
        StatSummaryCard(
          label = "Cashback Earned",
          value = "৳${TakaPayRepository.formatBdtPlain(totalCashback)}",
          accentColor = Color(0xFFD97706),
          modifier = Modifier.weight(1f),
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        placeholder = { Text("Search by TrxID, Name, or Number...") },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().testTag("statement_search_input"),
      )

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        filterOptions.forEach { filter ->
          FilterChip(
            selected = filter == selectedFilter,
            onClick = { selectedFilter = filter },
            label = { Text(filter) },
          )
        }
      }
    }

    if (filteredTransactions.isEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors =
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
          modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Icon(
              imageVector = Icons.Default.History,
              contentDescription = null,
              tint = TakaMagenta,
              modifier = Modifier.size(42.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "No Matching Transactions",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = "Try adjusting your filter or search query.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    } else {
      items(filteredTransactions, key = { it.id }) { tx ->
        TransactionListItemCard(tx = tx, onClick = { onTransactionClick(tx) })
      }
    }

    item { Spacer(modifier = Modifier.height(80.dp)) }
  }
}

@Composable
fun TransactionListItemCard(
  tx: MfsTransaction,
  onClick: () -> Unit,
) {
  val txType = TransactionType.fromCode(tx.type)
  val isCredit = txType.isCredit
  val formattedDate =
    remember(tx.createdAt) {
      val date = tx.createdAt?.toDate() ?: Date()
      SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(date)
    }

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    modifier =
      Modifier.fillMaxWidth()
        .clickable(onClick = onClick)
        .testTag("tx_card_${tx.id}"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier =
          Modifier.size(44.dp)
            .clip(CircleShape)
            .background(
              if (isCredit) TakaEmerald.copy(alpha = 0.15f)
              else TakaMagenta.copy(alpha = 0.15f)
            ),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector =
            if (isCredit) Icons.AutoMirrored.Filled.CallReceived
            else Icons.AutoMirrored.Filled.CallMade,
          contentDescription = txType.displayName,
          tint = if (isCredit) TakaEmerald else TakaMagenta,
          modifier = Modifier.size(22.dp),
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = tx.title,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
        )
        Text(
          text = "${tx.operatorOrProvider} • TrxID: ${tx.trxId}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
        )
        Text(
          text = "$formattedDate • ${tx.authMethod}",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Column(horizontalAlignment = Alignment.End) {
        Text(
          text =
            "${if (isCredit) "+" else "-"}৳${TakaPayRepository.formatBdtPlain(tx.amountBdt)}",
          style = MaterialTheme.typography.titleMedium,
          color = if (isCredit) TakaEmerald else TakaMagenta,
          fontWeight = FontWeight.Bold,
        )
        if (tx.cashbackBdt > 0) {
          Text(
            text = "+৳${TakaPayRepository.formatBdtPlain(tx.cashbackBdt)} CB",
            style = MaterialTheme.typography.labelSmall,
            color = TakaEmerald,
            fontWeight = FontWeight.Bold,
          )
        } else if (tx.feeBdt > 0) {
          Text(
            text = "Fee ৳${TakaPayRepository.formatBdtPlain(tx.feeBdt)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@Composable
private fun StatSummaryCard(
  label: String,
  value: String,
  accentColor: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surfaceVariant,
    modifier = modifier,
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.labelLarge,
        color = accentColor,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}

@Composable
fun ProfileAndSecurityScreen(
  wallet: WalletProfile,
  savedBillers: List<SavedBiller>,
  onUpdateSecurity: (biometricEnabled: Boolean, newPinHash: String?) -> Unit,
  onDeleteBiller: (String) -> Unit,
  onSignOutClick: () -> Unit,
) {
  var biometricEnabled by remember(wallet.biometricEnabled) {
    mutableStateOf(wallet.biometricEnabled)
  }
  var newPin by remember { mutableStateOf("") }

  Column(
    modifier =
      Modifier.fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 14.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // Profile Header Card
    Card(
      shape = RoundedCornerShape(22.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(modifier = Modifier.padding(20.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text(
              text = wallet.displayName,
              style = MaterialTheme.typography.headlineMedium,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = "TakaPay Number: ${wallet.accountNumber}",
              style = MaterialTheme.typography.titleSmall,
              color = TakaMagenta,
              fontWeight = FontWeight.Bold,
            )
          }
          Surface(shape = RoundedCornerShape(12.dp), color = TakaMagenta) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                Icons.Default.Verified,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = wallet.kycTier,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            Icons.Default.Stars,
            contentDescription = null,
            tint = Color(0xFFD97706),
            modifier = Modifier.size(20.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "${wallet.rewardPoints} TakaPay Reward Points • Silver Tier",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
          )
        }
      }
    }

    // Biometric & PIN Security Card
    Card(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(modifier = Modifier.padding(18.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
              Icons.Default.Fingerprint,
              contentDescription = null,
              tint = TakaMagenta,
              modifier = Modifier.size(28.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "Biometric Transaction Auth",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "Require Fingerprint / Face touch for all transfers & bill payments",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          Switch(
            checked = biometricEnabled,
            onCheckedChange = {
              biometricEnabled = it
              onUpdateSecurity(it, null)
            },
            modifier = Modifier.testTag("biometric_toggle_switch"),
          )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))

        Text(
          text = "Update 4-Digit MFS PIN",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          OutlinedTextField(
            value = newPin,
            onValueChange = {
              if (it.length <= 6 && it.all { ch -> ch.isDigit() }) newPin = it
            },
            label = { Text("New 4-Digit PIN") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f).testTag("new_pin_input"),
          )
          Button(
            onClick = {
              onUpdateSecurity(biometricEnabled, newPin)
              newPin = ""
            },
            enabled = newPin.length >= 4,
            colors = ButtonDefaults.buttonColors(containerColor = TakaMagenta),
            shape = RoundedCornerShape(12.dp),
          ) {
            Text("Save PIN")
          }
        }
      }
    }

    // Saved Billers & Priyo Contacts
    Card(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(modifier = Modifier.padding(18.dp)) {
        Text(
          text = "Saved Billers & Priyo Numbers (${savedBillers.size})",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (savedBillers.isEmpty()) {
          Text(
            text = "No saved billers or contacts yet.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        } else {
          savedBillers.forEach { biller ->
            Row(
              modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = biller.name,
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.SemiBold,
                )
                Text(
                  text = "${biller.billerType} • ${biller.providerCategory} • ${biller.accountOrMeterNo}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              IconButton(onClick = { onDeleteBiller(biller.id) }) {
                Icon(
                  Icons.Default.DeleteOutline,
                  contentDescription = "Delete ${biller.name}",
                  tint = MaterialTheme.colorScheme.error,
                )
              }
            }
          }
        }
      }
    }

    OutlinedButton(
      onClick = onSignOutClick,
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier.fillMaxWidth().height(52.dp).testTag("sign_out_button"),
    ) {
      Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
      Spacer(modifier = Modifier.width(8.dp))
      Text("Sign Out of TakaPay", fontWeight = FontWeight.Bold)
    }

    Spacer(modifier = Modifier.height(40.dp))
  }
}
