package com.example.security

import android.content.Context
import android.content.DialogInterface
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Password
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.repository.TakaPayRepository
import com.example.ui.theme.TakaEmerald
import com.example.ui.theme.TakaGold
import com.example.ui.theme.TakaMagenta
import com.example.ui.theme.TakaMagentaDark
import kotlinx.coroutines.delay

object BiometricAuthHelper {

  fun triggerHapticTick(context: Context) {
    try {
      val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(45L, VibrationEffect.DEFAULT_AMPLITUDE))
      } else {
        @Suppress("DEPRECATION") vibrator.vibrate(45L)
      }
    } catch (_: Exception) {}
  }

  fun promptHardwareBiometric(
    context: Context,
    title: String,
    subtitle: String,
    onAuthenticated: () -> Unit,
    onFallbackToSheet: (String) -> Unit,
  ) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
      onFallbackToSheet("Use Biometric Touch Sensor or 4-Digit MFS PIN below")
      return
    }
    try {
      val executor = ContextCompat.getMainExecutor(context)
      val cancellationSignal = CancellationSignal()
      val prompt =
        BiometricPrompt.Builder(context)
          .setTitle(title)
          .setSubtitle(subtitle)
          .setNegativeButton(
            "Use MFS PIN",
            executor,
            DialogInterface.OnClickListener { _, _ ->
              onFallbackToSheet("Switched to MFS PIN verification")
            },
          )
          .build()

      prompt.authenticate(
        cancellationSignal,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
          override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
            super.onAuthenticationSucceeded(result)
            triggerHapticTick(context)
            onAuthenticated()
          }

          override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
            super.onAuthenticationError(errorCode, errString)
            onFallbackToSheet(
              errString?.toString() ?: "Hardware sensor unavailable; verify below"
            )
          }

          override fun onAuthenticationFailed() {
            super.onAuthenticationFailed()
          }
        },
      )
    } catch (e: Exception) {
      onFallbackToSheet("Verify using Biometric Touch-and-Hold or MFS PIN")
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiometricOrPinConfirmSheet(
  actionTitle: String,
  recipientSummary: String,
  amountBdt: Double,
  feeBdt: Double,
  cashbackBdt: Double,
  expectedPin: String,
  biometricPreferred: Boolean,
  isProcessing: Boolean,
  onConfirmed: (authMethod: String) -> Unit,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var useBiometricTab by remember { mutableStateOf(biometricPreferred) }
  var enteredPin by remember { mutableStateOf("") }
  var pinError by remember { mutableStateOf<String?>(null) }
  var statusMessage by remember {
    mutableStateOf("Touch & Hold fingerprint sensor or tap System Biometric")
  }
  var isHoldingFinger by remember { mutableStateOf(false) }
  var holdProgress by remember { mutableFloatStateOf(0f) }

  LaunchedEffect(isHoldingFinger) {
    if (isHoldingFinger) {
      val steps = 16
      for (i in 1..steps) {
        if (!isHoldingFinger) break
        holdProgress = i / steps.toFloat()
        delay(45L)
      }
      if (isHoldingFinger && holdProgress >= 0.98f) {
        BiometricAuthHelper.triggerHapticTick(context)
        isHoldingFinger = false
        onConfirmed("BIOMETRIC")
      }
    } else {
      holdProgress = 0f
    }
  }

  val infiniteTransition = rememberInfiniteTransition(label = "biometricPulse")
  val pulseScale by
    infiniteTransition.animateFloat(
      initialValue = 1f,
      targetValue = 1.12f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(900, easing = FastOutSlowInEasing),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "pulseScale",
    )

  ModalBottomSheet(
    onDismissRequest = { if (!isProcessing) onDismiss() },
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 28.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      // Header bar inspired by bKash/Nagad confirmation sheet
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = "Confirm $actionTitle",
            style = MaterialTheme.typography.titleLarge,
            color = TakaMagenta,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = recipientSummary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        IconButton(onClick = { if (!isProcessing) onDismiss() }) {
          Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Financial breakdown summary card
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text("Amount", style = MaterialTheme.typography.bodyMedium)
            Text(
              "৳${TakaPayRepository.formatBdtPlain(amountBdt)}",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
          }
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text("Charge / Fee", style = MaterialTheme.typography.bodySmall)
            Text(
              if (feeBdt > 0) "+ ৳${TakaPayRepository.formatBdtPlain(feeBdt)}" else "FREE (৳0.00)",
              style = MaterialTheme.typography.bodySmall,
              color =
                if (feeBdt > 0) MaterialTheme.colorScheme.onSurfaceVariant else TakaEmerald,
              fontWeight = FontWeight.SemiBold,
            )
          }
          if (cashbackBdt > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text("Instant Cashback", style = MaterialTheme.typography.bodySmall)
              Text(
                "- ৳${TakaPayRepository.formatBdtPlain(cashbackBdt)}",
                style = MaterialTheme.typography.bodySmall,
                color = TakaEmerald,
                fontWeight = FontWeight.Bold,
              )
            }
          }
          HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              "Total Impact",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
            )
            Text(
              "৳${TakaPayRepository.formatBdtPlain(amountBdt + feeBdt - cashbackBdt)}",
              style = MaterialTheme.typography.titleMedium,
              color = TakaMagenta,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Mode selector: Biometric Touch vs MFS PIN
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        FilterChip(
          selected = useBiometricTab,
          onClick = { useBiometricTab = true },
          label = { Text("Biometric Touch") },
          leadingIcon = {
            Icon(
              imageVector = Icons.Default.Fingerprint,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
          },
          modifier = Modifier.weight(1f).testTag("auth_mode_biometric"),
        )
        FilterChip(
          selected = !useBiometricTab,
          onClick = { useBiometricTab = false },
          label = { Text("4-Digit MFS PIN") },
          leadingIcon = {
            Icon(
              imageVector = Icons.Default.Password,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
          },
          modifier = Modifier.weight(1f).testTag("auth_mode_pin"),
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      if (isProcessing) {
        CircularProgressIndicator(color = TakaMagenta)
        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = "Securing transaction on TakaPay Cloud Ledger...",
          style = MaterialTheme.typography.bodyMedium,
        )
      } else if (useBiometricTab) {
        // Biometric Touch & Hold / Hardware Biometric Prompt
        val animatedProgress by animateFloatAsState(targetValue = holdProgress, label = "holdProg")

        Box(
          contentAlignment = Alignment.Center,
          modifier =
            Modifier.size(96.dp)
              .scale(if (isHoldingFinger) 1.08f else pulseScale)
              .clip(CircleShape)
              .background(
                Brush.radialGradient(
                  colors = listOf(TakaMagenta, TakaMagentaDark)
                )
              )
              .border(3.dp, TakaGold, CircleShape)
              .pointerInput(Unit) {
                detectTapGestures(
                  onPress = {
                    isHoldingFinger = true
                    tryAwaitRelease()
                    isHoldingFinger = false
                  },
                  onTap = {
                    BiometricAuthHelper.triggerHapticTick(context)
                    onConfirmed("BIOMETRIC")
                  },
                )
              }
              .testTag("biometric_confirm_button"),
        ) {
          Icon(
            imageVector = Icons.Default.Fingerprint,
            contentDescription = "Authenticate with Biometrics",
            tint = Color.White,
            modifier = Modifier.size(52.dp),
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        LinearProgressIndicator(
          progress = { animatedProgress },
          color = TakaMagenta,
          trackColor = MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier.fillMaxWidth(0.75f).height(8.dp).clip(RoundedCornerShape(4.dp)),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = statusMessage,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
          onClick = {
            BiometricAuthHelper.promptHardwareBiometric(
              context = context,
              title = "Authorize $actionTitle",
              subtitle = "৳${TakaPayRepository.formatBdtPlain(amountBdt)} • $recipientSummary",
              onAuthenticated = { onConfirmed("BIOMETRIC") },
              onFallbackToSheet = { msg -> statusMessage = msg },
            )
          },
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = TakaMagenta,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text("Launch System Biometric Prompt")
        }
      } else {
        // 4-Digit MFS PIN verification
        OutlinedTextField(
          value = enteredPin,
          onValueChange = {
            if (it.length <= 6 && it.all { ch -> ch.isDigit() }) {
              enteredPin = it
              pinError = null
            }
          },
          label = { Text("Enter TakaPay PIN (Default: 1234)") },
          leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
          visualTransformation = PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
          singleLine = true,
          isError = pinError != null,
          supportingText = {
            if (pinError != null) {
              Text(pinError!!, color = MaterialTheme.colorScheme.error)
            } else {
              Text("Default starter PIN is 1234")
            }
          },
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().testTag("pin_input_field"),
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
          onClick = {
            if (enteredPin == expectedPin || enteredPin == "1234") {
              BiometricAuthHelper.triggerHapticTick(context)
              onConfirmed("PIN")
            } else {
              pinError = "Incorrect MFS PIN. Try 1234."
            }
          },
          enabled = enteredPin.length >= 4,
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(containerColor = TakaMagenta),
          modifier = Modifier.fillMaxWidth().height(52.dp).testTag("confirm_pin_button"),
        ) {
          Text(
            text = "Tap to Confirm Payment",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }
  }
}
