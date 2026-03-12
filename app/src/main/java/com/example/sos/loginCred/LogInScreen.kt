package com.example.sos.loginCred

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.ActivityNavigatorExtras
import androidx.navigation.NavController
import com.example.sos.R
import com.example.sos.Routes
import com.example.sos.Screen
import com.google.android.gms.auth.api.signin.GoogleSignIn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogInScreen(navController: NavController,
                authViewModel: AuthViewModel = viewModel()) {

    val context = LocalContext.current
    val activity = context as Activity

    val googleClient = remember { getGoogleClient(context) }
    val isLoggedIn = authViewModel.isLoggedIn()

    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->

        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)

        if (task.isSuccessful) {

            val account = task.result

            val idToken = account.idToken ?: return@rememberLauncherForActivityResult
            val name = account.displayName ?: ""
            val email = account.email ?: ""

            authViewModel.googleLogin(
                idToken = idToken,
                name = name,
                email = email
            ) { success ->

                if (success) {
                    navController.navigate(Routes.PERMISSION) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                } else {
                    Toast.makeText(context, "Google Login Failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    var showOtpDialog by remember { mutableStateOf(false) }

    var showCountryPicker by remember { mutableStateOf(false) }
    var selectedCountry by remember { mutableStateOf(Country("India", "IN", "+91")) }


    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            navController.navigate(Routes.HOME) {
                popUpTo(Routes.LOGIN) { inclusive = true }
            }
        }
    }


    Box(
        modifier = Modifier

            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF102543),
                        Color(0xFF020205)
                    )
                )
            )
    ) {

        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = {  }){
                    Icon(
                        painter = painterResource(R.drawable.icons8_account_50),
                        null,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "Help",
                    color = Color.Blue,
                    modifier = Modifier.clickable{}
                )
            }

            Icon(
                painterResource(id = R.drawable.chatgpt_image_feb_27__2026__11_54_19_am),
                null,
                modifier = Modifier.size(150.dp),
                tint = Color.Unspecified
            )
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Stay Safe, Everywhere.",
                color = Color.White,
                style = MaterialTheme.typography.headlineLarge
            )

            Text(
                text = "AI-powered protection at your fingertips.",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(24.dp))


            Spacer(modifier = Modifier.height(24.dp))

            PhoneLoginSection(
                selectedCountry = selectedCountry,
                onCountryClick = { showCountryPicker = true },
                onSendCode = { phone, country ->
                    val fullPhone = "${country.dialCode}$phone"

                    authViewModel.sendOtp(
                        phone = fullPhone,
                        activity = activity,
                        onCodeSent = {
                            showOtpDialog = true
                        },
                        onError = { errorMsg ->
                            Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                        }
                    )
                }

            )

            Spacer(modifier = Modifier.height(48.dp))

            GoogleLogIn(
                onGoogleClick = {googleLauncher.launch(googleClient.signInIntent)},
            )

            Spacer(modifier = Modifier.height(24.dp))

            TermsAndPrivacyText(
                onTermsClick = { /* open terms */ },
                onPrivacyClick = { /* open privacy */ }
            )

        }

        if (showCountryPicker) {
            CountryPickerBottomSheet(
                countries = countries,
                onCountrySelected = {
                    selectedCountry = it
                    showCountryPicker = false
                },
                onDismiss = { showCountryPicker = false }
            )
        }
    }
    if (showOtpDialog) {
        OtpDialog(
            onDismiss = { showOtpDialog = false },
            onVerify = { otp ->
                authViewModel.verifyOtp(
                    otp = otp,
                    onNewUser = { phone ->
                        showOtpDialog = false
                        navController.navigate("${Routes.REGISTER}/$phone")
                    },
                    onExistingUser = {
                        showOtpDialog = false
                        navController.navigate(Routes.PERMISSION) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                    onError = {
                        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        )
    }
}

@Composable
fun GuestSOSCard(
    onEmergencyClicked: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF1A2433),
                        Color(0xFF121826)
                    )
                ),
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Column {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFFFF4D4D),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Guest SOS",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Need help now? Skip login for instant emergency assistance.",
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF2A1E26),
                                Color(0xFF20141C)
                            )
                        ),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onEmergencyClicked() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFF4D4D),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Emergency SOS",
                        color = Color(0xFFFF4D4D),
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
        }
    }
}

@Composable
fun PhoneLoginSection(
    selectedCountry: Country,
    onCountryClick: () -> Unit,
    onSendCode: (String, Country) -> Unit
) {
    var phone by remember { mutableStateOf("") }
    val enabled = phone.length >= 10

    Column(modifier = Modifier.padding(24.dp)) {

        Text("Mobile Number", color = Color.White)

        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {

            Box(
                modifier = Modifier
                    .clickable { onCountryClick() }
                    .background(Color(0xFF1C2432), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Text("${selectedCountry.iso} ${selectedCountry.dialCode}", color = Color.White)
            }

            Spacer(Modifier.width(8.dp))

            TextField(
                value = phone,
                onValueChange = { phone = it.filter(Char::isDigit) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color.White,
                    focusedContainerColor = Color(0xFF1C2432),
                    unfocusedContainerColor = Color(0xFF1C2432),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        }

        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (enabled) Color(0xFF1E5AE8)
                    else Color(0xFF1E5AE8).copy(alpha = 0.4f),
                    RoundedCornerShape(50.dp)
                )
                .clickable(enabled) {
                    onSendCode(phone, selectedCountry)
                }
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Send Code", color = Color.White)
        }
    }
}


@Composable
fun OtpDialog(
    onDismiss: () -> Unit,
    onVerify: (String) -> Unit
) {
    var otp by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter OTP") },
        text = {
            OutlinedTextField(
                value = otp,
                onValueChange = {
                    otp = it.filter(Char::isDigit)
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                singleLine = true,
                placeholder = { Text("6 digit code") }
            )
        },
        confirmButton = {
            Button(
                enabled = otp.length == 6,
                onClick = { onVerify(otp) }
            ) {
                Text("Verify")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


@Composable
fun GoogleLogIn(
    onGoogleClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Row(verticalAlignment = Alignment.CenterVertically) {
            Divider(Modifier.weight(1f), color = Color.White.copy(alpha = 0.2f))
            Text(
                " OR CONTINUE WITH ",
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.labelSmall
            )
            Divider(Modifier.weight(1f), color = Color.White.copy(alpha = 0.2f))
        }

        Spacer(modifier = Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1C2432), RoundedCornerShape(50.dp))
                .clickable { onGoogleClick() }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Row {
                Icon(
                    painterResource(id = R.drawable.icons8_google_48),
                    null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Google", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

    }
}

@Composable
fun TermsAndPrivacyText(
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit
) {
    val annotatedText = buildAnnotatedString {
        withStyle(
            style = SpanStyle(
                color = Color.White.copy(alpha = 0.5f)
            )
        ) {
            append("By continuing, you agree to our ")
        }

        pushStringAnnotation(tag = "TERMS", annotation = "terms")
        withStyle(
            style = SpanStyle(
                color = Color(0xFF1E5AE8),
                fontWeight = FontWeight.Medium
            )
        ) {
            append("Terms of Service")
        }
        pop()

        withStyle(
            style = SpanStyle(
                color = Color.White.copy(alpha = 0.5f)
            )
        ) {
            append(" and ")
        }

        pushStringAnnotation(tag = "PRIVACY", annotation = "privacy")
        withStyle(
            style = SpanStyle(
                color = Color(0xFF1E5AE8),
                fontWeight = FontWeight.Medium
            )
        ) {
            append("Privacy Policy")
        }
        pop()
    }

    ClickableText(
        text = annotatedText,
        style = MaterialTheme.typography.labelSmall.copy(
            textAlign = TextAlign.Center
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        onClick = { offset ->
            annotatedText.getStringAnnotations(offset, offset)
                .firstOrNull()?.let {
                    when (it.tag) {
                        "TERMS" -> onTermsClick()
                        "PRIVACY" -> onPrivacyClick()
                    }
                }
        }
    )
}
