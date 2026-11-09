package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class WalletProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val displayName: String = "",
  val accountNumber: String = "",
  val pinHash: String = "1234",
  val biometricEnabled: Boolean = true,
  val balanceBdt: Double = 0.0,
  val totalSavingsBdt: Double = 0.0,
  val rewardPoints: Long = 0L,
  val kycTier: String = "Verified",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class MfsTransaction(
  @DocumentId val id: String = "",
  val userId: String = "",
  val type: String = "SEND_MONEY",
  val title: String = "",
  val recipientNumber: String = "",
  val recipientName: String = "",
  val operatorOrProvider: String = "",
  val amountBdt: Double = 0.0,
  val feeBdt: Double = 0.0,
  val cashbackBdt: Double = 0.0,
  val balanceAfterBdt: Double = 0.0,
  val referenceNote: String = "",
  val trxId: String = "",
  val authMethod: String = "BIOMETRIC",
  val status: String = "COMPLETED",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class SavingsGoal(
  @DocumentId val id: String = "",
  val userId: String = "",
  val title: String = "",
  val category: String = "DPS",
  val targetAmountBdt: Double = 0.0,
  val savedAmountBdt: Double = 0.0,
  val monthlyDepositBdt: Double = 0.0,
  val interestRatePercent: Double = 8.5,
  val institutionName: String = "",
  val autoSaveRoundUp: Boolean = true,
  val status: String = "ACTIVE",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class SavedBiller(
  @DocumentId val id: String = "",
  val userId: String = "",
  val billerType: String = "UTILITY",
  val name: String = "",
  val accountOrMeterNo: String = "",
  val providerCategory: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

enum class TransactionType(
  val code: String,
  val displayName: String,
  val isCredit: Boolean,
) {
  SEND_MONEY("SEND_MONEY", "Send Money", false),
  ADD_MONEY("ADD_MONEY", "Add Money", true),
  CASH_OUT("CASH_OUT", "Cash Out", false),
  MOBILE_RECHARGE("MOBILE_RECHARGE", "Mobile Recharge", false),
  UTILITY_BILL("UTILITY_BILL", "Pay Bill", false),
  MERCHANT_PAY("MERCHANT_PAY", "Merchant Payment", false),
  SAVINGS_DEPOSIT("SAVINGS_DEPOSIT", "Savings Deposit", false),
  SAVINGS_WITHDRAW("SAVINGS_WITHDRAW", "Savings Encashment", true);

  companion object {
    fun fromCode(code: String): TransactionType =
      entries.firstOrNull { it.code == code } ?: SEND_MONEY
  }
}

data class TelcoOperatorPreset(
  val name: String,
  val prefixHint: String,
  val cashbackPercent: Double,
  val bundles: List<RechargeBundle>,
)

data class RechargeBundle(
  val label: String,
  val amountBdt: Double,
  val validity: String,
  val cashbackBdt: Double = 0.0,
)

data class UtilityProviderPreset(
  val name: String,
  val category: String,
  val meterHint: String,
  val sampleBillNo: String,
  val defaultAmount: Double,
)

data class MerchantQrPreset(
  val merchantName: String,
  val merchantCode: String,
  val category: String,
  val cashbackPercent: Double,
  val suggestedAmount: Double,
)

object MfsPresets {
  val telcoOperators = listOf(
    TelcoOperatorPreset(
      name = "Grameenphone",
      prefixHint = "017 / 013",
      cashbackPercent = 2.0,
      bundles = listOf(
        RechargeBundle("30 GB + 500 Min Combo", 499.0, "30 Days", 15.0),
        RechargeBundle("10 GB High-Speed 4G", 199.0, "7 Days", 5.0),
        RechargeBundle("150 Min Talktime Pack", 98.0, "15 Days", 0.0),
        RechargeBundle("Unlimited Night Data", 54.0, "3 Days", 0.0),
      ),
    ),
    TelcoOperatorPreset(
      name = "Robi",
      prefixHint = "018",
      cashbackPercent = 2.5,
      bundles = listOf(
        RechargeBundle("40 GB + 400 Min Super", 548.0, "30 Days", 20.0),
        RechargeBundle("15 GB Streaming Pack", 219.0, "14 Days", 8.0),
        RechargeBundle("200 Min + 2 GB Bonus", 129.0, "10 Days", 0.0),
      ),
    ),
    TelcoOperatorPreset(
      name = "Banglalink",
      prefixHint = "019 / 014",
      cashbackPercent = 3.0,
      bundles = listOf(
        RechargeBundle("35 GB Orange Blast", 399.0, "30 Days", 18.0),
        RechargeBundle("12 GB + 200 Min", 249.0, "15 Days", 6.0),
        RechargeBundle("100 Min Super Saver", 67.0, "7 Days", 0.0),
      ),
    ),
    TelcoOperatorPreset(
      name = "Airtel",
      prefixHint = "016",
      cashbackPercent = 2.5,
      bundles = listOf(
        RechargeBundle("25 GB Youth Social Pack", 298.0, "30 Days", 12.0),
        RechargeBundle("8 GB Gaming Low-Ping", 148.0, "7 Days", 4.0),
      ),
    ),
    TelcoOperatorPreset(
      name = "Teletalk",
      prefixHint = "015",
      cashbackPercent = 1.5,
      bundles = listOf(
        RechargeBundle("20 GB Bornomala Data", 185.0, "30 Days", 5.0),
        RechargeBundle("150 Min Agami Voice", 79.0, "15 Days", 0.0),
      ),
    ),
  )

  val utilityProviders = listOf(
    UtilityProviderPreset("DESCO Prepaid", "Electricity", "Meter No (11 digits)", "44019283746", 1450.0),
    UtilityProviderPreset("DPDC Smart Meter", "Electricity", "Customer No (8 digits)", "88291044", 1820.0),
    UtilityProviderPreset("Palli Bidyut (REB)", "Electricity", "SMS Account No", "109283746512", 960.0),
    UtilityProviderPreset("Titas Gas", "Gas", "Customer Code", "210499812", 1080.0),
    UtilityProviderPreset("Karnaphuli Gas", "Gas", "Biller Account ID", "330198271", 1080.0),
    UtilityProviderPreset("Dhaka WASA", "Water", "Bill / Consumer No", "771029384", 640.0),
    UtilityProviderPreset("Carnival Internet", "Internet", "Subscriber ID", "CRN-55821", 1200.0),
    UtilityProviderPreset("Link3 Fiber", "Internet", "Client ID", "L3-992014", 1500.0),
    UtilityProviderPreset("Akash DTH TV", "TV & Education", "Subscriber ID", "AK-440192", 400.0),
  )

  val merchantQrPresets = listOf(
    MerchantQrPreset("Swapno Super Shop", "QR-SWAPNO-8812", "Grocery & Superstore", 5.0, 1250.0),
    MerchantQrPreset("Aarong Flagship", "QR-AARONG-1024", "Lifestyle & Apparel", 10.0, 2800.0),
    MerchantQrPreset("Meena Bazar", "QR-MEENA-3390", "Grocery & Superstore", 4.0, 950.0),
    MerchantQrPreset("Unimart Gulshan", "QR-UNIMART-7721", "Hypermarket", 5.0, 3200.0),
    MerchantQrPreset("Star Cineplex", "QR-CINEPLEX-5012", "Entertainment", 8.0, 850.0),
    MerchantQrPreset("North End Coffee", "QR-NORTHEND-2209", "Cafe & Dining", 6.0, 480.0),
  )

  val depositSources = listOf(
    "City Bank (Citytouch Visa)",
    "BRAC Bank Astha Linked A/C",
    "Eastern Bank (EBL Skybanking)",
    "Dutch-Bangla Nexus Card",
    "Prime Bank Mastercard",
    "bKash / Nagad Instant Bridge",
  )

  val cashOutAgents = listOf(
    "Mayer Doa Telecom Agent (01715-882910)",
    "Bismillah MFS Point (01819-334455)",
    "Dhaka Express Agent Booth (01911-776655)",
    "TakaPay Premier ATM Cash Out (01612-998877)",
  )

  val savingsInstitutions = listOf(
    "IDLC Finance DPS (9.2% p.a.)",
    "BRAC Bank Tara Savings (8.8% p.a.)",
    "City Bank Islamic Mudaraba (8.5% p.a.)",
    "Dhaka Bank Maha Sanchay (9.0% p.a.)",
  )
}
