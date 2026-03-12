package com.example.sos.loginCred

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class Country(
    val name: String,
    val iso: String,
    val dialCode: String
)

val countries = listOf(
    Country("Afghanistan", "AF", "+93"),
    Country("Albania", "AL", "+355"),
    Country("Algeria", "DZ", "+213"),
    Country("Andorra", "AD", "+376"),
    Country("Angola", "AO", "+244"),
    Country("Antigua and Barbuda", "AG", "+1-268"),
    Country("Argentina", "AR", "+54"),
    Country("Armenia", "AM", "+374"),
    Country("Australia", "AU", "+61"),
    Country("Austria", "AT", "+43"),
    Country("Azerbaijan", "AZ", "+994"),
    Country("Bahamas", "BS", "+1-242"),
    Country("United States", "US", "+1"),
    Country("India", "IN", "+91"),
    Country("United Kingdom", "UK", "+44"),
    Country("Canada", "CA", "+1"),
    Country("China", "CN", "+86"),
    Country("Germany", "DE", "+49"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryPickerBottomSheet(
    countries: List<Country>,
    onCountrySelected: (Country) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var search by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0F1724)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {

            // Drag handle
            Box(
                modifier = Modifier
                    .size(width = 40.dp, height = 4.dp)
                    .background(Color.Gray.copy(alpha = 0.4f), RoundedCornerShape(50))
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Select Country",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Search
            TextField(
                value = search,
                onValueChange = { search = it },
                placeholder = {
                    Text("Search country", color = Color.White.copy(alpha = 0.4f))
                },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color.White,
                    focusedContainerColor = Color(0xFF1C2432),
                    unfocusedContainerColor = Color(0xFF1C2432),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedPlaceholderColor = Color.White.copy(alpha = 0.4f),
                    unfocusedPlaceholderColor = Color.White.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Country list
            LazyColumn {
                items(
                    countries.filter {
                        it.name.contains(search, true) ||
                                it.iso.contains(search, true) ||
                                it.dialCode.contains(search)
                    }
                ) { country ->

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onCountrySelected(country)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = country.iso,
                            color = Color.White,
                            modifier = Modifier.width(40.dp)
                        )

                        Text(
                            text = country.name,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = country.dialCode,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}
