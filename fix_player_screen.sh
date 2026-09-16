#!/bin/bash

# Remove the appended lines from the bottom of PlayerScreen.kt
sed -i '/if (showMetadataDialog && media != null) {/,$d' app/src/main/java/com/example/ui/screens/PlayerScreen.kt

# Inject them inside the PlayerScreen function, just before the closing brace
sed -i '/^}$/i \    if (showMetadataDialog && media != null) {\n        EditMetadataDialog(media, viewModel) { showMetadataDialog = false }\n    }\n    \n    if (showLyricsDialog) {\n        AlertDialog(\n            onDismissRequest = { showLyricsDialog = false },\n            title = { Text("الكلمات") },\n            text = { Text("لم يتم العثور على كلمات مدمجة لهذه الأغنية.") },\n            confirmButton = { TextButton(onClick = { showLyricsDialog = false }) { Text("حسناً") } }\n        )\n    }' app/src/main/java/com/example/ui/screens/PlayerScreen.kt
